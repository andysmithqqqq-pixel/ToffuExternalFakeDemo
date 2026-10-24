package com.toffu.external;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private WebView web;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0F0F10);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(), "ToffuNative");
        web.addJavascriptInterface(new ToffuPill(MainActivity.this), "ToffuPill");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    private String rootCheck() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            int code = p.waitFor();
            if (code == 0 && line != null && line.contains("uid=0")) return "ROOT_OK";
            return "ROOT_DENIED";
        } catch (Exception e) { return "ROOT_DENIED"; }
    }

    private String gamePid() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "pidof ru.SOFFGames.gco"});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String s = r.readLine();
            p.waitFor();
            return (s == null || s.trim().isEmpty()) ? "не найден" : s.trim().split("\\s+")[0];
        } catch (Exception e) { return "не найден"; }
    }

    private boolean gameInstalled() {
        try {
            getPackageManager().getPackageInfo("ru.SOFFGames.gco", 0);
            return true;
        } catch (Exception e) { return false; }
    }

    public class Bridge {
        @JavascriptInterface public String checkRoot() { return rootCheck(); }
        @JavascriptInterface public String androidVersion() { return Build.VERSION.RELEASE; }
        @JavascriptInterface public String deviceModel() { return Build.MODEL == null ? "не определена" : Build.MODEL; }
        @JavascriptInterface public String pid() { return gamePid(); }
        @JavascriptInterface public boolean gameInstalled() { return MainActivity.this.gameInstalled(); }

        @JavascriptInterface public String launchGame() {
            runOnUiThread(() -> {
                try {
                    Intent launch = getPackageManager().getLaunchIntentForPackage("ru.SOFFGames.gco");
                    if (launch != null) startActivity(launch);
                } catch (Exception ignored) {}
            });
            return gameInstalled() ? "LAUNCH_ATTEMPTED" : "GAME_MISSING";
        }

        @JavascriptInterface public void requestOverlay() {
            runOnUiThread(() -> {
                if (!Settings.canDrawOverlays(MainActivity.this)) {
                    Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                    Toast.makeText(MainActivity.this, "Разрешите показ поверх других приложений", Toast.LENGTH_LONG).show();
                } else {
                    startService(new Intent(MainActivity.this, OverlayService.class));
                }
            });
        }

        @JavascriptInterface public void stopOverlay() {
            runOnUiThread(() -> stopService(new Intent(MainActivity.this, OverlayService.class)));
        }

        @JavascriptInterface public void openOverlaySettings() {
            runOnUiThread(() -> {
                if (!Settings.canDrawOverlays(MainActivity.this)) {
                    Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                } else startService(new Intent(MainActivity.this, OverlayService.class));
            });
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (web != null) web.evaluateJavascript(
            "if(window.nativeResume){window.nativeResume();}", null);
    }
}
