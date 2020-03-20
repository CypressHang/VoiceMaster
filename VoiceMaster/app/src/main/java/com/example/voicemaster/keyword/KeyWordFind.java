package com.example.voicemaster.keyword;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voicemaster.R;
import com.iflytek.cloud.msc.util.Base64;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;


public class KeyWordFind extends AppCompatActivity {

    // webapi接口地址
//    private static final String WEBTTS_URL = "http://ltpapi.xfyun.cn/v1/ke";
    private static final String WEBTTS_URL = "http://ltpapi.xfyun.cn/v1/ke";
    // 应用ID
    private static final String APPID = "5e6b5006";
    // 接口密钥
    private static final String API_KEY = "dc49e97387e0581ab06b143ab10c447f";
    // 文本
    private String TEXT = "打开页面物联网";


    private static final String TYPE = "dependent";
    private static final String TAG = "Cypress";

    private TextView TV_Key;
    private TextView et_oriText;
    private Button btn_Key;
    private Button btn_none;

    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_word);

        //强制联网
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }


        TV_Key = (TextView)findViewById(R.id.TV_key);
        et_oriText = (TextView)findViewById(R.id.et_oriText);
        btn_Key = (Button)findViewById(R.id.btn_key);

        initBtn();
    }
    private void initBtn(){
        btn_Key.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TEXT = et_oriText.getText().toString();
                Toast.makeText(getBaseContext(),"开始喽",Toast.LENGTH_LONG).show();
                getResult();
            }
        });
    }

    private void getResult(){
        Log.d(TAG, "onCreate: System.out.println(TEXT.length()) = " + TEXT.length());
        Map<String, String> header = null;
        try {
            header = buildHttpHeader();
            Log.d(TAG, "onCreate: header 读取到了");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String result = null;
        try {
            result = HttpUtil.doPost1(WEBTTS_URL, header, "text=" + URLEncoder.encode(TEXT, "utf-8"));
            Log.d(TAG, "onCreate: result读取到了，是"+result);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        TV_Key.setText("itp 接口调用结果：" + result);
    }


    /**
     * 组装http请求头
     */
    private static Map<String, String> buildHttpHeader() throws UnsupportedEncodingException {
        String curTime = System.currentTimeMillis() / 1000L + "";
        String param = "{\"type\":\"" + TYPE +"\"}";
        String paramBase64 = new String(Base64.encode(param.getBytes("UTF-8")));
//        String checkSum = DigestUtils.md5Hex(API_KEY + curTime + paramBase64);
        String checkSum = md5Encode32(API_KEY + curTime + paramBase64);
        Map<String, String> header = new HashMap<String, String>();
        header.put("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        header.put("X-Param", paramBase64);
        header.put("X-CurTime", curTime);
        header.put("X-CheckSum", checkSum);
        header.put("X-Appid", APPID);
        return header;
    }
    /**
     * 32位MD5加密
     * @param content -- 待加密内容
     * @return
     */
    public static String md5Encode32(String content) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(content.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("NoSuchAlgorithmException",e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UnsupportedEncodingException", e);
        }
        //对生成的16字节数组进行补零操作
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10){
                hex.append("0");
            }
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }

    /**
     * 发送post请求
     *
     * @param url
     * @param header
     * @param body
     * @return
     */
    public static String doPost1(String url, Map<String, String> header, String body) {
        String result = "";
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            // 设置 url
            URL realUrl = new URL(url);
            URLConnection connection = realUrl.openConnection();
            HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
            // 设置 header
            for (String key : header.keySet()) {
                httpURLConnection.setRequestProperty(key, header.get(key));
            }
            // 设置请求 body
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);
            out = new PrintWriter(httpURLConnection.getOutputStream());
            // 保存body
            out.print(body);
            // 发送body
            out.flush();
            if (HttpURLConnection.HTTP_OK != httpURLConnection.getResponseCode()) {
                Log.d(TAG, "doPost1: " + "Http 请求失败，状态码：" + httpURLConnection.getResponseCode());
                return null;
            }

            // 获取响应body
            in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            Log.d(TAG, "doPost1: 读取失败");
            return null;
        }
        return result;
    }



}
