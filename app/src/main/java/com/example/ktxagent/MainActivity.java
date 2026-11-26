package com.example.ktxagent;

import android.app.*;
import android.os.*;
import android.content.*;
import android.provider.Settings;
import android.widget.*;

public class MainActivity extends Activity {

    public void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout l = new LinearLayout(this);
        l.setOrientation(1);
        l.setPadding(40, 80, 40, 40);
        setContentView(l);

        TextView t = new TextView(this);
        t.setText("KTX Agent\n전체 열차 · 일반실/특실 무관\n코레일톡 조회 화면에서 감시합니다.");
        t.setTextSize(22);
        l.addView(t);

        var p = getSharedPreferences("agent", 0);

        for (String k : new String[]{"sound", "vibration", "stop"}) {
            Switch s = new Switch(this);

            s.setText(k.equals("sound") ? "소리 알림" :
                k.equals("vibration") ? "진동 알림" :
                "예약 성공 시 종료");

            s.setChecked(p.getBoolean(k, true));

            s.setOnCheckedChangeListener((v, c) ->
                p.edit().putBoolean(k, c).apply());

            l.addView(s);
        }

        Button a = new Button(this);
        a.setText("접근성 권한 설정");

        a.setOnClickListener(v ->
            startActivity(new Intent(
                Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        l.addView(a);

        Button start = new Button(this);
        start.setText("감시 시작 / 코레일톡 열기");

        start.setOnClickListener(v -> {

            if (AgentService.instance == null) {
                Toast.makeText(this,
                    "접근성 권한을 먼저 허용하세요",
                    1).show();
                return;
            }

            AgentService.instance.arm();

            Intent i = getPackageManager()
                .getLaunchIntentForPackage("com.korail.talk");

            if (i != null)
                startActivity(i);
            else
                Toast.makeText(this,
                    "코레일톡을 직접 열어주세요",
                    1).show();
        });

        l.addView(start);

        Button stop = new Button(this);
        stop.setText("감시 종료");

        stop.setOnClickListener(v -> {
            if (AgentService.instance != null)
                AgentService.instance.disarm();
        });

        l.addView(stop);
    }
}
