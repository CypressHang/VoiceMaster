package com.example.voicemaster.translate;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voicemaster.R;
import com.example.voicemaster.translate.bean.AddrBean;
import com.example.voicemaster.translate.util.HttpUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Translate extends AppCompatActivity {

    // OTS webapi 接口地址
    private static final String WebITS_URL = "https://itrans.xfyun.cn/v2/its";
    // 应用ID（到控制台获取）
    private static final String APPID = "5e6b5006";
    // 接口APIKey（到控制台机器翻译服务页面获取）
    private static final String API_KEY = "714619ce557650c953246041bebf978a";
    // 接口APISercet（到控制台机器翻译服务页面获取）
    private static final String API_SECRET = "626f33675996d26f624c1437ab23e2ae";
    // 语种列表参数值请参照接口文档：https://doc.xfyun.cn/rest_api/机器翻译.html
    // 源语种
    private static String FROM = "cn";
    // 目标语种
    private static String TO = "en";
    // 翻译文本
    private static String TEXT = "我好帅啊哈哈哈";

    private static final String TAG = "cypress";

    private String result = null;

    private EditText et_ori;
    private TextView tv_result;
    private Button btn_start;
    private Button btn_nothing;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

        et_ori = findViewById(R.id.et_oriTeans);
        et_ori.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        tv_result = findViewById(R.id.tv_result);
        btn_nothing = findViewById(R.id.btn_nothing);
        btn_start = findViewById(R.id.btn_start);


        //强制联网
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TEXT = et_ori.getText().toString();
                try {
                    result = runTranslate();
                } catch (Exception e) {
                    Log.d(TAG, "onCreate: 出错了，问题是: " + e.toString());
                    e.printStackTrace();
                }
                tv_result.setText(resolve(result));
                Toast.makeText(Translate.this, "翻译完成^.^", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public String resolve(String result){
        Gson json = new Gson();
        AddrBean resultJson = json.fromJson(result, AddrBean.class);
        return resultJson.getData().getResult().getTrans_result().getDst();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String runTranslate() throws Exception{
        String body = buildHttpBody();
        String resultStr = null;
        Map<String, String> header = buildHttpHeader(body);
        Map<String, Object> resultMap = HttpUtil.doPost2(WebITS_URL, header, body);
        if (resultMap != null) {
            resultStr = resultMap.get("body").toString();
            Log.d(TAG, "runTranslate: 【结果】" + resultStr);
            //以下仅用于调试
            Gson json = new Gson();
            ResponseData resultData = json.fromJson(resultStr, ResponseData.class);
            int code = resultData.getCode();
            if (resultData.getCode() != 0) {
                Log.d(TAG, "runTranslate: 错误代码 " + code);
            }
        } else {
            Log.d(TAG, "runTranslate: 调用失败！请根据错误信息检查代码");
        }
        return resultStr;
    }

    /**
     * 组装http请求头
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Map<String, String> buildHttpHeader(String body) throws Exception {
        Map<String, String> header = new HashMap<String, String>();
        URL url = new URL(WebITS_URL);

        //时间戳
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date dateD = new Date();
        String date = format.format(dateD);
        //System.out.println("【ITS WebAPI date】\n" + date);

        //对body进行sha256签名,生成digest头部，POST请求必须对body验证
        String digestBase64 = "SHA-256=" + signBody(body);
        //System.out.println("【ITS WebAPI digestBase64】\n" + digestBase64);

        //hmacsha256加密原始字符串
        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n").//
                append("date: ").append(date).append("\n").//
                append("POST ").append(url.getPath()).append(" HTTP/1.1").append("\n").//
                append("digest: ").append(digestBase64);
        //System.out.println("【ITS WebAPI builder】\n" + builder);
        String sha = hmacsign(builder.toString(), API_SECRET);
        //System.out.println("【ITS WebAPI sha】\n" + sha);

        //组装authorization
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", API_KEY, "hmac-sha256", "host date request-line digest", sha);
        System.out.println("【ITS WebAPI authorization】\n" + authorization);

        header.put("Authorization", authorization);
        header.put("Content-Type", "application/json");
        header.put("Accept", "application/json,version=1.0");
        header.put("Host", url.getHost());
        header.put("Date", date);
        header.put("Digest", digestBase64);
        System.out.println("【ITS WebAPI header】\n" + header);
        return header;
    }

    /**
     * 组装http请求体
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String buildHttpBody() throws Exception {
        JsonObject body = new JsonObject();
        JsonObject business = new JsonObject();
        JsonObject common = new JsonObject();
        JsonObject data = new JsonObject();
        //填充common
        common.addProperty("app_id", APPID);
        //填充business
        business.addProperty("from", FROM);
        business.addProperty("to", TO);
        //填充data
        //System.out.println("【OTS WebAPI TEXT字个数：】\n" + TEXT.length());
        byte[] textByte = TEXT.getBytes("UTF-8");
        String textBase64 = new String(Base64.getEncoder().encodeToString(textByte));
        //System.out.println("【OTS WebAPI textBase64编码后长度：】\n" + textBase64.length());
        data.addProperty("text", textBase64);
        //填充body
        body.add("common", common);
        body.add("business", business);
        body.add("data", data);
        return body.toString();
    }

    /**
     * 对body进行SHA-256加密
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static String signBody(String body) throws Exception {
        MessageDigest messageDigest;
        String encodestr = "";
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(body.getBytes("UTF-8"));
            encodestr = Base64.getEncoder().encodeToString(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encodestr;
    }

    /**
     * hmacsha256加密
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static String hmacsign(String signature, String apiSecret) throws Exception {
        Charset charset = Charset.forName("UTF-8");
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(signature.getBytes(charset));
        return Base64.getEncoder().encodeToString(hexDigits);
    }

    public static class ResponseData {
        private int code;
        private String message;
        private String sid;
        private Object data;
        public int getCode() {
            return code;
        }
        public String getMessage() {
            return this.message;
        }
        public String getSid() {
            return sid;
        }
        public Object getData() {
            return data;
        }
    }

}


