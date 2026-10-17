package com.kunstruct.bikenavhelper;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.SystemClock;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

final class Diagnostics {
    private static final ArrayDeque<String> events = new ArrayDeque<>();
    private static volatile String state = "Ready";
    private static long started;
    private static String initialBattery = "not started";
    static volatile RelayPipe pipe;
    static volatile Network wifi;
    static volatile long lastToBoard, lastToPhone;

    static synchronized void begin(Context context) {
        events.clear(); started = SystemClock.elapsedRealtime();
        initialBattery = battery(context); lastToBoard = lastToPhone = 0;
        log("Helper " + BuildConfig.VERSION_NAME + "; Android " + Build.VERSION.RELEASE);
    }
    static synchronized void log(String message) {
        events.addLast(new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()) + " " + message);
        while (events.size() > 160) events.removeFirst();
    }
    static void state(String message) { state = message; log(message); }
    static String state() { return state; }
    static String network(ConnectivityManager cm, Network n) {
        if (n == null) return "none";
        NetworkCapabilities c = cm.getNetworkCapabilities(n);
        if (c == null) return "disconnected";
        return "cellular=" + c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                + " WiFi=" + c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                + " VPN=" + c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                + " internet=" + c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                + " validated=" + c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
    static String battery(Context context) {
        Intent i = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) return "unavailable";
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        return (scale > 0 ? level * 100 / scale : -1) + "%; temperature="
                + i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0 + " C; plugged="
                + i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
    }
    static synchronized String report(Context context) {
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        RelayPipe current = pipe;
        long down = current != null ? current.toBoard.get() : lastToBoard;
        long up = current != null ? current.toPhone.get() : lastToPhone;
        StringBuilder s = new StringBuilder("BikeNav Helper " + BuildConfig.VERSION_NAME + "\n");
        s.append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append(" Android ").append(Build.VERSION.RELEASE).append(" SDK ").append(Build.VERSION.SDK_INT).append('\n');
        s.append("State: ").append(state).append("\nElapsed: ").append(started == 0 ? 0 : (SystemClock.elapsedRealtime()-started)/1000).append(" s\n");
        s.append("Battery at connect: ").append(initialBattery).append("\nBattery now: ").append(battery(context)).append('\n');
        s.append("Default network: ").append(network(cm, cm.getActiveNetwork())).append('\n');
        s.append("Local WiFi: ").append(network(cm, wifi)).append('\n');
        if (wifi != null) {
            LinkProperties lp = cm.getLinkProperties(wifi);
            if (lp != null) s.append("WiFi interface: ").append(lp.getInterfaceName()).append("\nAddresses: ").append(lp.getLinkAddresses()).append("\nRoutes: ").append(lp.getRoutes()).append("\nDNS: ").append(lp.getDnsServers()).append('\n');
        }
        s.append("AA → board: ").append(down).append(" bytes\nBoard → AA: ").append(up).append(" bytes\n\n");
        for (String e : events) s.append(e).append('\n');
        return s.toString();
    }
}
