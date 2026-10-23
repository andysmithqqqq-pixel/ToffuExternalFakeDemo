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
        if (expanded) { if(panel!=null){wm.removeView(panel);panel=null;} expanded=false; return; }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(12,12,12,12);
        root.setBackground(bg(0xF0161619, 0xFF343438));
        TextView title = label("Toffu External", 17);
        root.addView(title);
        TextView desc = label("Combat  ·  World  ·  Misc  ·  Configs", 12);
        desc.setTextColor(0xFFC8B6FF); root.addView(desc);
        String[] items = {"Combat", "AimBot (визуальный переключатель)", "World", "Misc", "Configs", "Оверлей активен"};
        for (String s : items) {
            TextView row = label("◉  " + s, 13);
            row.setBackground(bg(0xFF222226, 0xFF343438));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = 5; root.addView(row, lp);
        }
        TextView close = label("Свернуть меню", 13);
        close.setGravity(Gravity.CENTER);
        close.setBackground(bg(0xFF2B3A31, 0xFF3B4A42));
        close.setOnClickListener(v -> togglePanel());
        root.addView(close);
        panel = root;
        panelParams = new WindowManager.LayoutParams(
            (int)(getResources().getDisplayMetrics().widthPixels * .86),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.CENTER;
        panelParams.x=0; panelParams.y=0;
        root.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy; int ox, oy;
            public boolean onTouch(View v, MotionEvent e) {
                if(e.getAction()==MotionEvent.ACTION_DOWN){sx=e.getRawX();sy=e.getRawY();ox=panelParams.x;oy=panelParams.y;return true;}
                if(e.getAction()==MotionEvent.ACTION_MOVE){panelParams.x=ox+(int)(e.getRawX()-sx);panelParams.y=oy+(int)(e.getRawY()-sy);wm.updateViewLayout(panel,panelParams);return true;}
                return false;
            }
        });
        wm.addView(panel, panelParams); expanded=true;
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public void onDestroy() {
        if (panel != null) { try { wm.removeView(panel); } catch(Exception ignored) {} }
        if (pill != null) { try { wm.removeView(pill); } catch(Exception ignored) {} }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
