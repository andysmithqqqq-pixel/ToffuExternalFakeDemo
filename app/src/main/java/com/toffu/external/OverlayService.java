package com.toffu.external;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OverlayService extends Service {
    public static volatile boolean running = false;
    static final String PREFS = "toffu";
    private static final int ACCENT = 0xFFA8D5BA;

    private SharedPreferences sp;
    private WindowManager wm;
    private float density;
    private final Handler h = new Handler(Looper.getMainLooper());
    private LinearLayout pill;
    private TextView pillTop, pillBottom;
    private WindowManager.LayoutParams pillLp, panelLp;
    private LinearLayout panel, content;
    private final TextView[] tabViews = new TextView[4];
    private final String[] tabNames = {"Combat", "World", "Misc", "Configs"};
    private String tab = "Combat";
    private final Runnable tick = new Runnable() {
        @Override public void run() { updateClock(); h.postDelayed(this, 20000); }
    };

    @Override public void onCreate() {
        super.onCreate();
        sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        density = getResources().getDisplayMetrics().density;
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) { stopSelf(); return; }
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        try {
            createPill();
            running = true;
            h.postDelayed(tick, 20000);
        } catch (Exception e) { stopSelf(); }
    }

    private int dp(float v) { return (int) (v * density + 0.5f); }
    private float scale() { return sp.getInt("pill_size", 100) / 100f; }
    private int panelBg() {
        int a = Math.round(sp.getInt("menu_alpha", 94) * 255 / 100f);
        return (a << 24) | 0x161619;
    }
    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
    }
    private GradientDrawable bg(int fill, int stroke, float r) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(r));
        d.setStroke(Math.max(1, dp(1)), stroke);
        return d;
    }
    private TextView label(String t, float size, int color) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    /* ---------- плашка ---------- */
    private void createPill() {
        pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.VERTICAL);
        pill.setBackground(bg(0xF019191C, 0xFF37343E, 12));
        pillTop = label("", 11, Color.WHITE);
        pillBottom = label("|  External", 10, 0xFFC8B6FF);
        pill.addView(pillTop);
        pill.addView(pillBottom);

        pillLp = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        pillLp.gravity = Gravity.TOP | Gravity.START;
        pillLp.x = dp(8);
        pillLp.y = dp(70);

        applySizeViews();
        updateClock();

        pill.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy; int ox, oy; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        sx = e.getRawX(); sy = e.getRawY();
                        ox = pillLp.x; oy = pillLp.y; moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - sx, dy = e.getRawY() - sy;
                        if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) moved = true;
                        if (moved) {
                            pillLp.x = ox + (int) dx;
                            pillLp.y = oy + (int) dy;
                            try { wm.updateViewLayout(pill, pillLp); } catch (Exception ignored) {}
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) togglePanel();
                        return true;
                }
                return false;
            }
        });
        wm.addView(pill, pillLp);
    }

    private void applySizeViews() {
        float s = scale();
        pillTop.setTextSize(11f * s);
        pillBottom.setTextSize(10f * s);
        int ph = dp(10 * s), pv = dp(6 * s);
        pillTop.setPadding(ph, pv, ph, 0);
        pillBottom.setPadding(ph, 0, ph, pv);
    }

    private void applySize() {
        if (pill == null) return;
        applySizeViews();
        try { wm.updateViewLayout(pill, pillLp); } catch (Exception ignored) {}
    }

    private void applyAlpha() {
        if (panel != null) panel.setBackground(bg(panelBg(), 0xFF343438, 16));
    }

    private void updateClock() {
        if (pillTop == null) return;
        pillTop.setText("ϟ Storm  |  Grand Criminal Online  |  ◷ "
            + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
    }

    /* ---------- меню ---------- */
    private void togglePanel() {
        if (panel != null) closePanel(); else openPanel();
    }

    private void closePanel() {
        if (panel != null && wm != null) {
            try { wm.removeView(panel); } catch (Exception ignored) {}
        }
        panel = null;
        content = null;
    }

    private void makeDraggable(View handle, final WindowManager.LayoutParams lp, final View target) {
        handle.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy; int ox, oy;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        sx = e.getRawX(); sy = e.getRawY(); ox = lp.x; oy = lp.y;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        lp.x = ox + (int) (e.getRawX() - sx);
                        lp.y = oy + (int) (e.getRawY() - sy);
                        try { wm.updateViewLayout(target, lp); } catch (Exception ignored) {}
                        return true;
                }
                return false;
            }
        });
    }

    private void openPanel() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(10), dp(12), dp(12));
        root.setBackground(bg(panelBg(), 0xFF343438, 16));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("Storm External", 16, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        head.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView x = label("✕", 16, 0xFF9A9A9F);
        x.setPadding(dp(14), dp(6), dp(4), dp(6));
        x.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { closePanel(); }
        });
        head.addView(x);
        root.addView(head);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 4; i++) {
            final String n = tabNames[i];
            TextView t = label(n, 12, 0xFF8A8A8F);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, dp(8), 0, dp(8));
            t.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { tab = n; renderTab(); }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.leftMargin = dp(2); lp.rightMargin = dp(2);
            tabs.addView(t, lp);
            tabViews[i] = t;
        }
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = dp(8);
        root.addView(tabs, tp);

        ScrollView sv = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        sv.addView(content);
        int maxH = (int) (getResources().getDisplayMetrics().heightPixels * 0.45f);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(-1, maxH);
        svLp.topMargin = dp(6);
        root.addView(sv, svLp);

        int w = Math.min(dp(380), (int) (getResources().getDisplayMetrics().widthPixels * 0.9f));
        panelLp = new WindowManager.LayoutParams(
            w, WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        panelLp.gravity = Gravity.CENTER;

        makeDraggable(head, panelLp, root);
        panel = root;
        renderTab();
        try { wm.addView(panel, panelLp); } catch (Exception e) { panel = null; content = null; }
    }

    private void add(View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(6);
        content.addView(v, lp);
    }

    private void renderTab() {
        if (content == null) return;
        for (int i = 0; i < 4; i++) {
            boolean on = tabNames[i].equals(tab);
            tabViews[i].setTextColor(on ? ACCENT : 0xFF8A8A8F);
            tabViews[i].setBackground(on ? bg(0xFF2B3A31, 0xFF3B4A42, 10) : bg(0x00000000, 0x00000000, 10));
        }
        content.removeAllViews();
        if (tab.equals("Combat")) {
            for (String s : new String[]{"Fly", "NoClip", "Speed x2", "Speed x5", "High Jump", "No Gravity", "Teleport to Spawn", "Walk on Water", "Instant Respawn", "Ghost Mode"}) add(toggleRow(s));
        } else if (tab.equals("World")) {
            for (String s : new String[]{"Mini MoveHelper", "Mini DetectHelper"}) add(toggleRow(s));
        } else if (tab.equals("Misc")) {
            add(toggleRow("God Mode"));
            add(toggleRow("Inf. Health"));
            add(toggleRow("Inf. Armor"));
            add(toggleRow("Inf. Money"));
            add(toggleRow("Inf. GC"));
            add(toggleRow("Inf. XP"));
            add(toggleRow("Give Money"));
            add(toggleRow("Give Level"));
            add(toggleRow("Anti-AFK"));
            add(toggleRow("Anti-Kick"));
            add(sliderRow("Размер плашки", 60, 160, "pill_size", 100, new Runnable() {
                @Override public void run() { applySize(); }
            }));
            add(sliderRow("Прозрачность меню", 40, 100, "menu_alpha", 94, new Runnable() {
                @Override public void run() { applyAlpha(); }
            }));
            add(buttonRow("Остановить оверлей", new Runnable() {
                @Override public void run() { stopSelf(); }
            }));
        } else {
            String cfg = sp.getString("cfg", "");
            TextView t = label("Активный конфиг:  " + (cfg.isEmpty() ? "не выбран" : cfg), 13, Color.WHITE);
            t.setPadding(dp(12), dp(12), dp(12), dp(12));
            t.setBackground(bg(0xFF222226, 0xFF343438, 10));
            add(t);
            add(buttonRow("Сбросить переключатели", new Runnable() {
                @Override public void run() {
                    SharedPreferences.Editor ed = sp.edit();
                    for (String k : sp.getAll().keySet()) if (k.startsWith("t_")) ed.remove(k);
                    ed.apply();
                    renderTab();
                }
            }));
        }
    }

    private View toggleRow(String name) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(12), dp(10), dp(12), dp(10));
        r.setBackground(bg(0xFF222226, 0xFF343438, 10));
        r.addView(label(name, 13, Color.WHITE), new LinearLayout.LayoutParams(0, -2, 1f));

        final FrameLayout track = new FrameLayout(this);
        final View knob = new View(this);
        track.addView(knob);
        r.addView(track, new LinearLayout.LayoutParams(dp(40), dp(22)));

        final String key = "t_" + name;
        paintToggle(track, knob, sp.getBoolean(key, false));
        r.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean n = !sp.getBoolean(key, false);
                sp.edit().putBoolean(key, n).apply();
                paintToggle(track, knob, n);
            }
        });
        return r;
    }

    private void paintToggle(FrameLayout track, View knob, boolean on) {
        track.setBackground(bg(on ? 0xFF3B4A42 : 0xFF2A2A2C, on ? 0xFF3B4A42 : 0xFF3A3A3E, 11));
        GradientDrawable k = new GradientDrawable();
        k.setShape(GradientDrawable.OVAL);
        k.setColor(on ? ACCENT : 0xFF8A8A8A);
        knob.setBackground(k);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(16), dp(16));
        lp.gravity = (on ? Gravity.END : Gravity.START) | Gravity.CENTER_VERTICAL;
        lp.leftMargin = dp(3); lp.rightMargin = dp(3);
        knob.setLayoutParams(lp);
    }

    private View sliderRow(String name, final int min, int max, final String key, int def, final Runnable onChange) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(dp(12), dp(10), dp(12), dp(8));
        r.setBackground(bg(0xFF222226, 0xFF343438, 10));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.addView(label(name, 13, Color.WHITE), new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView val = label("", 12, ACCENT);
        top.addView(val);
        r.addView(top);

        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        int cur = Math.max(min, Math.min(max, sp.getInt(key, def)));
        sb.setProgress(cur - min);
        val.setText(cur + "%");
        if (Build.VERSION.SDK_INT >= 21) {
            sb.setProgressTintList(ColorStateList.valueOf(ACCENT));
            sb.setThumbTintList(ColorStateList.valueOf(ACCENT));
        }
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int v = min + p;
                val.setText(v + "%");
                if (fromUser) { sp.edit().putInt(key, v).apply(); onChange.run(); }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        r.addView(sb, new LinearLayout.LayoutParams(-1, -2));
        return r;
    }

    private View buttonRow(String text, final Runnable action) {
        TextView b = label(text, 13, ACCENT);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), dp(11), dp(12), dp(11));
        b.setBackground(bg(0xFF2B3A31, 0xFF3B4A42, 10));
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
        return b;
    }

    /* ---------- жизненный цикл ---------- */
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (wm == null || pill == null) return START_NOT_STICKY;
        if (intent != null && "refresh".equals(intent.getAction())) {
            applySize();
            applyAlpha();
            if (panel != null && "Configs".equals(tab)) renderTab();
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        running = false;
        h.removeCallbacks(tick);
        closePanel();
        if (pill != null && wm != null) {
            try { wm.removeView(pill); } catch (Exception ignored) {}
        }
        pill = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
