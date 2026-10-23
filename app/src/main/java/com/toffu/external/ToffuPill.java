package com.toffu.external;

import android.content.Context;
import android.content.Intent;
import android.webkit.JavascriptInterface;

public class ToffuPill {
    private final Context ctx;

    public ToffuPill(Context c) { ctx = c.getApplicationContext(); }

    @JavascriptInterface
    public void setPillSize(int pct) {
        int v = Math.max(50, Math.min(200, pct));
        ctx.getSharedPreferences(OverlayService.PREFS, 0).edit().putInt("pill_size", v).apply();
        refresh();
    }

    @JavascriptInterface
    public void setConfig(String name) {
        ctx.getSharedPreferences(OverlayService.PREFS, 0).edit().putString("cfg", name == null ? "" : name).apply();
        refresh();
    }

    private void refresh() {
        if (!OverlayService.running) return;
        Intent i = new Intent(ctx, OverlayService.class);
        i.setAction("refresh");
        ctx.startService(i);
    }
}
