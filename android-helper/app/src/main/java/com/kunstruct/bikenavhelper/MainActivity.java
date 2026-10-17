package com.kunstruct.bikenavhelper;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

public final class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView state, diagnostics;
    private EditText ssid, password, address;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            state.setText(Diagnostics.state());
            diagnostics.setText(Diagnostics.report(MainActivity.this));
            handler.postDelayed(this, 1500);
        }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(28, 36, 28, 36); body.setBackgroundColor(Color.rgb(14, 27, 32)); scroll.addView(body);
        TextView title = text(body, "BikeNav Helper", 30); title.setTextColor(Color.rgb(101, 228, 176));
        text(body, "Experimental • " + BuildConfig.VERSION_NAME, 14);
        text(body, "1. Turn off the phone hotspot.\n2. Forget the saved MiniNav Wi-Fi network in Settings.\n3. On BikeNav, tap the Wi-Fi label until Helper WiFi appears.\n4. Start Android Auto’s headunit server.\n5. Return here, tap Connect and approve the Wi-Fi dialog.", 17);
        text(body, "Keep mobile data on. This test aims to keep internet on cellular; compatibility and battery savings are not yet verified.", 15);
        ssid = field(body, "Board Wi-Fi name", "MiniNav", false);
        password = field(body, "Board Wi-Fi password", "mininav123", true);
        address = field(body, "Board address", "192.168.4.1", false);
        button(body, "Connect", v -> connect());
        button(body, "Disconnect", v -> { stopService(new Intent(this, RelayService.class)); Diagnostics.state("Disconnected"); });
        button(body, "Wi-Fi settings", v -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        button(body, "Android Auto settings", v -> {
            Intent i = new Intent("com.google.android.gms.car.settings");
            try { startActivity(i); }
            catch (android.content.ActivityNotFoundException e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
                Toast.makeText(this, "Search Settings for Android Auto", Toast.LENGTH_LONG).show();
            }
        });
        state = text(body, "Ready", 21); state.setTextColor(Color.rgb(101, 228, 176));
        button(body, "Share debug report", v -> share());
        text(body, "The report includes device model, network addresses/routes, relay counts and battery readings. It excludes Wi-Fi passwords and Android Auto payloads. Review it below before sharing.", 14);
        diagnostics = text(body, "", 13); diagnostics.setTextIsSelectable(true);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(scroll);
    }
    private TextView text(LinearLayout parent, String value, int size) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(Color.rgb(226, 238, 239));
        t.setPadding(0, 10, 0, 14); parent.addView(t); return t;
    }
    private EditText field(LinearLayout parent, String label, String value, boolean secret) {
        text(parent, label, 14);
        EditText e = new EditText(this); e.setText(value); e.setSingleLine(true); e.setTextColor(Color.WHITE);
        e.setInputType(secret ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        parent.addView(e); return e;
    }
    private void button(LinearLayout parent, String label, View.OnClickListener listener) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setOnClickListener(listener); parent.addView(b);
    }
    private void connect() {
        ArrayList<String> permissions = new ArrayList<>();
        String wifiPermission = Build.VERSION.SDK_INT >= 33 ? Manifest.permission.NEARBY_WIFI_DEVICES : Manifest.permission.ACCESS_FINE_LOCATION;
        if (checkSelfPermission(wifiPermission) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(wifiPermission);
            if (Build.VERSION.SDK_INT <= 32) permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!permissions.isEmpty()) { requestPermissions(permissions.toArray(new String[0]), 1); return; }
        startConnection();
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        String required = Build.VERSION.SDK_INT >= 33 ? Manifest.permission.NEARBY_WIFI_DEVICES : Manifest.permission.ACCESS_FINE_LOCATION;
        if (checkSelfPermission(required) == PackageManager.PERMISSION_GRANTED) startConnection();
        else Toast.makeText(this, "Wi-Fi permission is required to connect", Toast.LENGTH_LONG).show();
    }
    private void startConnection() {
        String name = ssid.getText().toString(), pass = password.getText().toString(), ip = address.getText().toString().trim();
        if (name.isEmpty() || name.getBytes(StandardCharsets.UTF_8).length > 32 || pass.length() < 8 || pass.length() > 63 || !validIPv4(ip)) {
            Toast.makeText(this, "Enter a Wi-Fi name, 8–63 character password and valid IPv4 address", Toast.LENGTH_LONG).show(); return;
        }
        WifiManager wifi = getSystemService(WifiManager.class);
        if (!wifi.isWifiEnabled()) { Toast.makeText(this, "Enable Wi-Fi first", Toast.LENGTH_LONG).show(); return; }
        Intent intent = new Intent(this, RelayService.class).putExtra("ssid", name).putExtra("password", pass).putExtra("address", ip);
        startForegroundService(intent);
    }
    private boolean validIPv4(String text) {
        String[] parts = text.split("\\.", -1); if (parts.length != 4) return false;
        for (String p : parts) { try { if (!p.matches("[0-9]{1,3}") || Integer.parseInt(p) > 255) return false; } catch (NumberFormatException e) { return false; } }
        return true;
    }
    private void share() {
        try {
            Files.write(new File(getCacheDir(), "debug-report.txt").toPath(), Diagnostics.report(this).getBytes(StandardCharsets.UTF_8));
            Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "BikeNav Helper debug report")
                    .putExtra(Intent.EXTRA_STREAM, ReportProvider.URI)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newRawUri("BikeNav report", ReportProvider.URI));
            startActivity(Intent.createChooser(intent, "Share BikeNav report"));
        } catch (Exception e) { Toast.makeText(this, "Could not share report: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show(); }
    }
    @Override public void onResume() { super.onResume(); handler.post(refresh); }
    @Override public void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
}
