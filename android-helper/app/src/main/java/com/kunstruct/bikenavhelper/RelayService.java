package com.kunstruct.bikenavhelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class RelayService extends Service {
    public static final String STOP = "com.kunstruct.bikenavhelper.STOP";
    private final Handler main = new Handler(Looper.getMainLooper());
    private ConnectivityManager cm;
    private ConnectivityManager.NetworkCallback wifiCallback, defaultCallback;
    private Session session;
    private boolean requested;

    @Override public void onCreate() {
        super.onCreate();
        cm = getSystemService(ConnectivityManager.class);
        getSystemService(NotificationManager.class).createNotificationChannel(
                new NotificationChannel("relay", "BikeNav connection", NotificationManager.IMPORTANCE_LOW));
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        startForeground(1, notification());
        // Prevent a second tap/Activity recreation from opening another AA session.
        if (requested) return START_NOT_STICKY;
        Diagnostics.begin(this);
        String ssid = intent.getStringExtra("ssid"), password = intent.getStringExtra("password");
        String address = intent.getStringExtra("address");
        if (ssid == null || password == null || address == null) { stopSelf(); return START_NOT_STICKY; }
        requested = true;
        Diagnostics.state("Requesting local Wi-Fi. Approve the Android connection dialog.");
        defaultCallback = new ConnectivityManager.NetworkCallback() {
            private String last = "";
            @Override public void onCapabilitiesChanged(Network n, NetworkCapabilities c) {
                String now = Diagnostics.network(cm, n);
                if (!now.equals(last)) { last = now; Diagnostics.log("Default network: " + now); }
            }
            @Override public void onLost(Network n) { Diagnostics.log("Default network lost"); }
        };
        wifiCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network network) {
                if (!requested || session != null) return;
                Diagnostics.wifi = network;
                Diagnostics.state("Local Wi-Fi connected. Connecting to BikeNav…");
                Session next = new Session(network, address); session = next;
                new Thread(next, "BikeNav-relay").start();
            }
            @Override public void onLinkPropertiesChanged(Network n, android.net.LinkProperties p) {
                Diagnostics.log("Local Wi-Fi routes updated: " + p.getRoutes());
            }
            @Override public void onLost(Network network) {
                if (session != null && session.network.equals(network)) { session.cancel(); session = null; }
                Diagnostics.wifi = null;
                Diagnostics.state("Wi-Fi disconnected. Tap Disconnect, then Connect to retry.");
            }
            @Override public void onUnavailable() {
                Diagnostics.state("Wi-Fi request declined or unavailable. Check Helper WiFi mode and retry.");
                stopSelf();
            }
        };
        try {
            cm.registerDefaultNetworkCallback(defaultCallback, main);
            WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(ssid).setWpa2Passphrase(password).build();
            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier).build();
            cm.requestNetwork(request, wifiCallback, main, 60000);
        } catch (RuntimeException e) {
            Diagnostics.state("Cannot request Wi-Fi: " + e.getClass().getSimpleName() + ". Check permissions and retry.");
            stopSelf();
        }
        return START_NOT_STICKY;
    }
    private Notification notification() {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, RelayService.class).setAction(STOP), PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, "relay").setSmallIcon(R.drawable.ic_bike)
                .setContentTitle("BikeNav Helper active").setContentText("Local Wi-Fi relay • tap for diagnostics")
                .setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", stop).build()).build();
    }
    @Override public void onDestroy() {
        requested = false;
        if (session != null) { session.cancel(); session = null; }
        if (wifiCallback != null) { try { cm.unregisterNetworkCallback(wifiCallback); } catch (IllegalArgumentException ignored) { } }
        if (defaultCallback != null) { try { cm.unregisterNetworkCallback(defaultCallback); } catch (IllegalArgumentException ignored) { } }
        Diagnostics.wifi = null;
        if (!Diagnostics.state().startsWith("Wi-Fi request") && !Diagnostics.state().startsWith("Cannot request"))
            Diagnostics.state("Disconnected");
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }

    private final class Session implements Runnable {
        final Network network;
        final String address;
        private volatile boolean cancelled;
        private Socket board, phone;
        private RelayPipe pipe;
        Session(Network network, String address) { this.network = network; this.address = address; }
        synchronized boolean own(Socket socket, boolean isBoard) {
            if (cancelled) { close(socket); return false; }
            if (isBoard) board = socket; else phone = socket;
            return true;
        }
        synchronized void cancel() {
            cancelled = true;
            if (pipe != null) pipe.close();
            close(board); close(phone);
        }
        private void close(Socket socket) { if (socket != null) try { socket.close(); } catch (IOException ignored) { } }
        @Override public void run() {
            PowerManager.WakeLock awake = getSystemService(PowerManager.class).newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, "BikeNav:Relay");
            boolean openedHeadunit = false;
            try {
                // Retry only before opening the headunit server, whose session may
                // become stuck if repeatedly probed. Board listener opens after DHCP.
                IOException last = null;
                for (int attempt = 0; attempt < 10 && !cancelled; attempt++) {
                    Socket candidate = new Socket();
                    if (!own(candidate, true)) return;
                    try {
                        network.bindSocket(candidate);
                        candidate.connect(new InetSocketAddress(address, 5278), 1500);
                        candidate.setTcpNoDelay(true); candidate.setKeepAlive(true);
                        last = null; break;
                    } catch (IOException e) { last = e; close(candidate); Thread.sleep(1000); }
                }
                if (cancelled) return;
                if (last != null) throw new IOException("BikeNav relay unreachable. Select Helper WiFi on firmware 1.2.0-helper-alpha1.", last);
                Diagnostics.state("BikeNav found. Opening Android Auto server on this phone…");
                Socket local = new Socket();
                if (!own(local, false)) return;
                // Do not bind the whole process: loopback AA and other apps retain
                // their normal network selection. Only the board socket uses WiFi.
                local.connect(new InetSocketAddress("127.0.0.1", 5277), 2500);
                openedHeadunit = true;
                local.setTcpNoDelay(true); local.setKeepAlive(true);
                synchronized (this) {
                    if (cancelled) return;
                    pipe = new RelayPipe(board, local);
                }
                Diagnostics.pipe = pipe;
                awake.acquire(6 * 60 * 60 * 1000L); // safety cap; normally released on EOF/Disconnect
                try {
                    Diagnostics.state("Relaying Android Auto. Check that Chrome still uses mobile data.");
                    pipe.run();
                } finally {
                    if (awake.isHeld()) awake.release();
                }
                if (!cancelled) Diagnostics.state("Android Auto session ended. Stop/start headunit server, then Disconnect → Connect.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException | RuntimeException e) {
                if (!cancelled) {
                    Diagnostics.log("Relay failure: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    Diagnostics.state(openedHeadunit
                            ? "Relay ended. Stop/start the headunit server then Disconnect → Connect."
                            : (board != null && board.isConnected()
                                ? "Android Auto server unavailable. Start headunit server, then Disconnect → Connect."
                                : "BikeNav unavailable. Select Helper WiFi, check board address, then Disconnect → Connect."));
                }
            } finally {
                if (pipe != null) {
                    Diagnostics.lastToBoard = pipe.toBoard.get(); Diagnostics.lastToPhone = pipe.toPhone.get();
                    if (Diagnostics.pipe == pipe) Diagnostics.pipe = null;
                }
                cancel();
                // Keep WiFi request alive for diagnostics until explicit disconnect.
                // No automatic AA probes/reconnects against a possibly stuck server.
            }
        }
    }
}
