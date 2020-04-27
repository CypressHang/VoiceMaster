package com.example.voicemaster.audio;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voicemaster.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.example.voicemaster.keyword.KeyWordFind;
import com.example.voicemaster.translate.Translate;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class LongVoice extends AppCompatActivity implements com.baidu.speech.EventListener {
    private static final String TAG = "cypress" ;
    protected TextView txtResult;
    protected EditText et_result;
    protected Button btn;
    protected Button btn_key;
    protected Button btn_translation;
    protected Button stopBtn;
    private EventManager asr;
    private void start(){
        Map<String,Object> params = new LinkedHashMap<>();//传递Map<String,Object>的参数，会将Map自动序列化为json
        String event = null;
        event = SpeechConstant.ASR_START;
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME,false);//回调当前音量
        String json = null;
//        json = new JSONObject(params).toString();//demo用json数据来做数据交换的方式
        String para = "{vad.endpoint-timeout=0, accept-audio-volume=false}";
        asr.send(event, para, null, 0, 0);// 初始化EventManager对象,这个实例只能创建一次，就是我们上方创建的asr，此处开始传入
    }

    private void stop(){
        txtResult.append("停止识别");
        asr.send(SpeechConstant.ASR_STOP, null, null, 0, 0);//此处停止
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_long);

        initView();
        initPermission();

        asr = EventManagerFactory.create(LongVoice.this,"asr");//注册自己的输出事件类
        asr.registerListener(this);//// 调用 EventListener 中 onEvent方法

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });

        btn_translation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jumpTranslation();
            }
        });
        btn_key.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jumpKeyWord();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
        asr.unregisterListener(this);//退出事件管理器
        // 必须与registerListener成对出现，否则可能造成内存泄露
    }
    public void onEvent(String name, String params, byte[] data, int offset, int length) {
        String resultTxt = null;
        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)){//识别结果参数
            if (params.contains("\"final_result\"")){//语义结果值
                try {
                    JSONObject json = new JSONObject(params);
                    String result = json.getString("best_result");//取得key的识别结果
                    resultTxt = result;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resultTxt != null){
            resultTxt += "\n";
//            txtResult.append(resultTxt);
            et_result.append(resultTxt);
        }
    }
    private void initView() {
        txtResult = findViewById(R.id.txtResult);
        txtResult.setMovementMethod(ScrollingMovementMethod.getInstance());
        btn = findViewById(R.id.btn);
        btn_key = findViewById(R.id.btn_toKey);
        btn_translation = findViewById(R.id.btn_toTranslation);
        stopBtn = findViewById(R.id.btn_stop);
        et_result = findViewById(R.id.et_result);
        et_result.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        et_result.setSingleLine(false);
        et_result.setHorizontallyScrolling(false);
    }
    private void initPermission() {
        String permissions[] = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm :permissions){
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                //进入到这里代表没有权限.
                Log.d(TAG, "initPermission: 没权限");
            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()){
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
    }

    public void jumpKeyWord(){
        if (et_result.getText().toString() == null || et_result.getText().toString().equals("")){
            Toast.makeText(this, "啥都没有，不能跳转", Toast.LENGTH_SHORT).show();
        }
        else {
            //取消换行
            KeyWordFind.TEXT = et_result.getText().toString().replaceAll("\r|\n", "");
            Log.d(TAG, "onNavigationItemSelected: 打开关键词提取");
            startActivity(new Intent(this, KeyWordFind.class));
        }
    }
    public void jumpTranslation(){
        if (et_result.getText().toString() == null || et_result.getText().toString().equals("")){
            Toast.makeText(this, "啥都没有，不能跳转", Toast.LENGTH_SHORT).show();
        }
        else {
            Translate.TEXT = et_result.getText().toString();
            Log.d(TAG, "onNavigationItemSelected: 打开翻译");
            startActivity(new Intent(this, Translate.class));
        }
    }
}
