package com.example.ktxagent;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.*;
import android.os.*;
import android.media.*;
import android.content.*;
import android.widget.Toast;
import java.util.*;

public class AgentService extends AccessibilityService {

    static AgentService instance;
    private boolean armed = false, pending = false;
    private long lastRefresh = 0;

    private final Handler h =
        new Handler(Looper.getMainLooper());

    private final Runnable tick = new Runnable() {
        public void run() {
            if (!armed) return;
            inspect();
            if (armed) h.postDelayed(this, 3000);
        }
    };

    protected void onServiceConnected() {
        instance = this;
    }

    public void arm() {
        disarm();
        armed = true;
        pending = false;
        lastRefresh = 0;
        h.post(tick);
    }

    public void disarm() {
        armed = false;
        pending = false;
        h.removeCallbacks(tick);
    }

    public void onInterrupt() {
        disarm();
    }

    public void onDestroy() {
        disarm();
        instance = null;
        super.onDestroy();
    }

    public void onAccessibilityEvent(AccessibilityEvent e) {}

    private String label(AccessibilityNodeInfo n) {
        return ((n.getText() == null ? "" :
            n.getText().toString()) + " " +
            (n.getContentDescription() == null ? "" :
            n.getContentDescription().toString())).trim();
    }

    private void collect(AccessibilityNodeInfo n,
                         List<AccessibilityNodeInfo> out) {
        if (n == null) return;
        out.add(n);

        for (int i = 0; i < n.getChildCount(); i++)
            collect(n.getChild(i), out);
    }

    private boolean click(AccessibilityNodeInfo n) {
        if (!n.isVisibleToUser() || !n.isEnabled())
            return false;

        for (int i = 0; n != null && i < 3;
             i++, n = n.getParent()) {

            if (n.isClickable() && n.isEnabled())
                return n.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK);
        }

        return false;
    }

    private void inspect() {
        AccessibilityNodeInfo root = getRootInActiveWindow();

        if (root == null ||
            !"com.korail.talk".contentEquals(
                root.getPackageName())) return;

        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        collect(root, nodes);

        StringBuilder all = new StringBuilder();

        for (var n : nodes)
            all.append(label(n)).append('\n');

        String screen = all.toString();

        if (pending) {
            if (screen.contains("예약이 완료") ||
                screen.contains("예약 완료") ||
                screen.contains("예약완료")) {

                signal();
                disarm();
            }
            return;
        }

        if (!screen.contains("KTX") ||
            !screen.contains("일반실") ||
            !screen.contains("특실") ||
            screen.contains("결제하기")) return;

        for (var n : nodes) {
            String s = label(n);

            if (s.equals("예약") ||
                s.equals("예약가능") ||
                s.equals("예약 가능") ||
                s.equals("일반실 예약") ||
                s.equals("특실 예약")) {

                if (click(n)) {
                    pending = true;

                    Toast.makeText(this,
                        "예약 버튼 클릭: 결과 확인 중",
                        Toast.LENGTH_LONG).show();

                    return;
                }
            }
        }

        long now = SystemClock.elapsedRealtime();

        if (now - lastRefresh >= 30000) {
            for (var n : nodes) {
                String s = label(n);

                if (s.equals("다시 조회") ||
                    s.equals("다시조회") ||
                    s.equals("새로고침")) {

                    lastRefresh = now;
                    click(n);
                    return;
                }
            }
        }
    }

    private void signal() {
        var p = getSharedPreferences("agent", 0);

        if (p.getBoolean("sound", true)) {
            Ringtone r = RingtoneManager.getRingtone(
                this, RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_NOTIFICATION));

            if (r != null) r.play();
        }

        if (p.getBoolean("vibration", true)) {
            Vibrator v = (Vibrator)
                getSystemService(VIBRATOR_SERVICE);

            if (v != null)
                v.vibrate(VibrationEffect.createOneShot(
                    700, VibrationEffect.DEFAULT_AMPLITUDE));
        }

        Toast.makeText(this,
            "예약 완료 문구 감지 - 코레일톡에서 확인 및 결제하세요",
            Toast.LENGTH_LONG).show();
    }
}
