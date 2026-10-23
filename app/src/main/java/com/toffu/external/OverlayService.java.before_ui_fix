package com.toffu.external;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OverlayService extends Service {
    private WindowManager wm;
    private View pill, panel;
    private WindowManager.LayoutParams pillParams, panelParams;
    private boolean expanded = false;

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        createPill();
    }

    private GradientDrawable bg(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(18);
        d.setStroke(1, stroke); return d;
    }
    private TextView label(String text, int size) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextColor(Color.WHITE); t.setTextSize(size);
        t.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        t.setPadding(12, 7, 12, 7);
        return t;
    }
    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void applyOverlaySettings(Intent intent) {
        if (intent == null) return;
        int size = intent.getIntExtra("pillsize", 100);
        size = Math.max(70, Math.min(150, size));
        float scale = size / 100f;
        if (pill != null) {
            pill.setScaleX(scale);
            pill.setScaleY(scale);
        }
    }

    private void createPill() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(bg(0xF019191C, 0xFF37343E));
        TextView top = label("ϟ Toffu  |  Матрёшка РП  |  ◷ " + new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()), 11);
        TextView bottom = label("|  External", 10);
        bottom.setTextColor(0xFFC8B6FF);
        box.addView(top); box.addView(bottom);
        pill = box;
        pillParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        pillParams.gravity = Gravity.TOP | Gravity.LEFT;
        pillParams.x = 8; pillParams.y = 70;
        box.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy; int ox, oy;
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    sx=e.getRawX(); sy=e.getRawY(); ox=pillParams.x; oy=pillParams.y; return true;
                }
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    if (Math.abs(e.getRawX()-sx)<8 && Math.abs(e.getRawY()-sy)<8) togglePanel();
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    pillParams.x=ox+(int)(e.getRawX()-sx); pillParams.y=oy+(int)(e.getRawY()-sy);
                    wm.updateViewLayout(pill,pillParams); return true;
                }
                return true;
            }
        });
        wm.addView(pill, pillParams);
    }
    private void togglePanel() {
        if (expanded) {
            if (panel != null) {
                try { wm.removeView(panel); } catch (Exception ignored) {}
                panel = null;
            }
            expanded = false;
            return;
        }

        final int accent = 0xFFC8B6FF;
        final int surface = 0xFF222228;
        final int selectedColor = 0xFF42365C;
        final int border = 0xFF494452;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 14, 16, 14);
        root.setBackground(bg(0xF0141419, border));

        TextView title = label("ϟ  TOFFU EXTERNAL", 18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(accent);
        root.addView(title);

        TextView subtitle = label("МАТРЁШКА RP  •  OVERLAY", 10);
        subtitle.setTextColor(0xFFAAA5B5);
        root.addView(subtitle);

        View divider = new View(this);
        divider.setBackgroundColor(0xFF38343F);
        LinearLayout.LayoutParams dividerLp =
            new LinearLayout.LayoutParams(-1, 1);
        dividerLp.topMargin = 10;
        dividerLp.bottomMargin = 10;
        root.addView(divider, dividerLp);

        TextView section = label("ВЫБЕРИТЕ РАЗДЕЛ", 10);
        section.setTextColor(0xFFAAA5B5);
        root.addView(section);

        final TextView status = label("Выберите категорию меню", 12);
        status.setTextColor(accent);
        status.setPadding(4, 12, 4, 12);
        root.addView(status);

        final String[] categories = {
            "Combat", "AimBot", "World", "Misc", "Configs"
        };
        final TextView[] buttons = new TextView[categories.length];
        final int[] active = {-1};

        for (int i = 0; i < categories.length; i++) {
            final int index = i;
            TextView row = label("◇    " + categories[i], 14);
            row.setBackground(bg(surface, border));
            row.setPadding(14, 12, 14, 12);
            row.setClickable(true);
            row.setFocusable(true);

            LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = 5;
            root.addView(row, lp);
            buttons[i] = row;

            row.setOnClickListener(v -> {
                active[0] = index;
                for (int j = 0; j < buttons.length; j++) {
                    buttons[j].setBackground(bg(
                        j == index ? selectedColor : surface,
                        j == index ? accent : border
                    ));
                    buttons[j].setText(
                        (j == index ? "◆    " : "◇    ") + categories[j]
                    );
                    buttons[j].setTextColor(j == index ? accent : Color.WHITE);
                }
                status.setText(categories[index] + "  •  раздел выбран");
            });
        }

        TextView note = label(
            "Элементы меню демонстрационные — игровые функции не подключены.",
            10
        );
        note.setTextColor(0xFFAAA5B5);
        note.setPadding(4, 12, 4, 8);
        root.addView(note);

        TextView close = label("⌄   СВЕРНУТЬ МЕНЮ", 12);
        close.setGravity(Gravity.CENTER);
        close.setTextColor(Color.WHITE);
        close.setBackground(bg(0xFF30283D, accent));
        close.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams closeLp =
            new LinearLayout.LayoutParams(-1, -2);
        closeLp.topMargin = 8;
        root.addView(close, closeLp);
        close.setOnClickListener(v -> togglePanel());

        panel = root;
        panelParams = new WindowManager.LayoutParams(
            (int)(getResources().getDisplayMetrics().widthPixels * .86),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        panelParams.gravity = Gravity.CENTER;
        panelParams.x = 0;
        panelParams.y = 0;

        root.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy;
            int ox, oy;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    sx = e.getRawX();
                    sy = e.getRawY();
                    ox = panelParams.x;
                    oy = panelParams.y;
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    panelParams.x = ox + (int)(e.getRawX() - sx);
                    panelParams.y = oy + (int)(e.getRawY() - sy);
                    try {
                        wm.updateViewLayout(panel, panelParams);
                    } catch (Exception ignored) {}
                    return true;
                }
                return false;
            }
        });

        wm.addView(panel, panelParams);
        expanded = true;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        applyOverlaySettings(intent);
        return START_STICKY;
    }
    @Override public void onDestroy() {
        if (panel != null) { try { wm.removeView(panel); } catch(Exception ignored) {} }
        if (pill != null) { try { wm.removeView(pill); } catch(Exception ignored) {} }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
