package com.example.voicemaster.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.voicemaster.MainActivity;
import com.example.voicemaster.R;
import com.example.voicemaster.login.Login_Activity;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * 描述：闪屏页
 *
 *
 */

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "cypress";
    SharedPreferences sp;
    private long currentTime;
    TextView tv_tip;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        TextView tv_logo = findViewById(R.id.tv_logo);
        tv_tip = findViewById(R.id.tv_tip);

        sp=this.getSharedPreferences("recordTime", this.MODE_PRIVATE);
        //获取当前时间
        currentTime = System.currentTimeMillis();

        init();

        tv_logo.postDelayed(new Runnable() {
            @Override
            public void run() {
                jump();
            }
        }
        , 500L);
    }

    /* 完成一些初始化操作 */
    private void init() {
        Toast.makeText(this, "欢迎使用", Toast.LENGTH_SHORT).show();

        //获得上次时间，要是没有，放入这次时间
        long lastTime = sp.getLong("usedTime",currentTime);
        if(lastTime == currentTime){
            SharedPreferences.Editor editor = sp.edit();
            editor.putLong("usedTime",currentTime);
            editor.apply();
        }
        Log.d(TAG, "init: currentTime 是" +currentTime + "\nlastTime 是 "+lastTime);

        long day = (currentTime - lastTime)/(86400*1000) + 1;
        tv_tip.setText("语音大师已经陪伴您 " + day + " 天");
        if(day >= 30){
            tv_tip.setText("您已经免费使用语音大师 " + day + " 天，超过30天，请及时充值！");
        }
    }

    /* 页面逻辑跳转 */
    private void jump() {
        startActivity(new Intent(this, Login_Activity.class));
        finish();
    }

}
