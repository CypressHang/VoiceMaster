package com.example.voicemaster.audio;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voicemaster.R;
import com.alibaba.fastjson.JSON;
import com.iflytek.msp.cpdb.lfasr.exception.LfasrException;
import com.iflytek.msp.cpdb.lfasr.model.LfasrType;
import com.iflytek.msp.cpdb.lfasr.model.Message;
import com.iflytek.msp.cpdb.lfasr.model.ProgressStatus;
import com.iflytek.msp.cpdb.lfasr.client.LfasrClientImp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;


public class SoundRecord extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "cypress";

    //是否在录制
    private boolean isRecording = false;
    //开始录音
    private Button startAudio;
    //结束录音
    private Button stopAudio;
    //播放录音
    private Button playAudio;
    //删除文件
    private Button deleteAudio;
    //跳转
    private Button jumpWord;
    //转wav
    private Button toWav;
    //隐私模式
    private Button privateMod;

    private ScrollView mScrollView;
    private TextView tv_audio_succeess;

    //pcm文件
    private File file;

    // 原始音频存放地址
    private static String local_file = "./resource/audio/cypress.wav";

    private LfasrClientImp lc = null;

    /**
     * pcm格式转wav格式工具类
     */
    private PcmToWavUtil pcmToWavUtil = new PcmToWavUtil();

    /*
     * 转写类型选择：标准版和电话版(旧版本, 不建议使用)分别为：
     * LfasrType.LFASR_STANDARD_RECORDED_AUDIO 和 LfasrType.LFASR_TELEPHONY_RECORDED_AUDIO
     * */
    private static final LfasrType type = LfasrType.LFASR_STANDARD_RECORDED_AUDIO;

    // 等待时长（秒）
    private static int sleepSecond = 10;

    //采样率
    private int frequency = 16000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_record);

//        PropertyConfigurator.configure("src/log4j.properties");
        // 初始化LFASRClient实例
        try {
            lc = LfasrClientImp.initLfasrClient();
        } catch (LfasrException e) {
            // 初始化异常，解析异常描述信息
            Message initMsg = JSON.parseObject(e.getMessage(), Message.class);
            System.out.println("ecode=" + initMsg.getErr_no());
            System.out.println("failed=" + initMsg.getFailed());
        }

        initView();

    }

    //初始化View
    private void initView() {

        mScrollView = (ScrollView) findViewById(R.id.mScrollView);
        tv_audio_succeess = (TextView) findViewById(R.id.tv_audio_succeess);
        printLog("初始化成功");
        startAudio = (Button) findViewById(R.id.startAudio);
        startAudio.setOnClickListener(this);
        stopAudio = (Button) findViewById(R.id.stopAudio);
        stopAudio.setOnClickListener(this);
        playAudio = (Button) findViewById(R.id.playAudio);
        playAudio.setOnClickListener(this);
        deleteAudio = (Button) findViewById(R.id.deleteAudio);
        deleteAudio.setOnClickListener(this);
        jumpWord = (Button) findViewById(R.id.toChange);
        jumpWord.setOnClickListener(this);
        toWav = (Button) findViewById(R.id.btn_toWav);
        toWav.setOnClickListener(this);
        privateMod = (Button) findViewById(R.id.btn_private);
        privateMod.setOnClickListener(this);
    }

    //点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startAudio:
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        StartRecord();
                        Log.d(TAG, "start");
                    }
                });
                thread.start();
                printLog("开始录音");
                ButtonEnabled(false, true, false);
                break;
            case R.id.stopAudio:
                isRecording = false;
                ButtonEnabled(true, false, true);
                printLog("停止录音");
                break;
            case R.id.playAudio:
                PlayRecord();
                ButtonEnabled(true, false, true);
                printLog("播放录音");
                break;
            case R.id.btn_toWav:
                if (file == null) {
                    Log.d(TAG, "onClick: 还没创建文件呢，请先录音！");
                    Toast.makeText(this, "还没创建文件呢，请先录音！", Toast.LENGTH_SHORT).show();
                    break;
                }
                pcmToWav(file.toString());
                printLog("转换成wav");
                break;
            case R.id.toChange:
                if (file == null) {
                    Log.d(TAG, "onClick: 还没创建文件呢，请先录音！");
                    Toast.makeText(this, "还没创建文件呢，请先录音！", Toast.LENGTH_SHORT).show();
                    break;
                }
                printLog("文件路径是" + file.toString());
                break;
            case R.id.deleteAudio:
                deleFile();
                printLog("删除录音");
                break;
            case R.id.btn_private:
                if (frequency == 16000) {
                    frequency = 8000;
                    printLog("开启隐私模式");
                } else if (frequency == 8000) {
                    frequency = 16000;
                    printLog("正常模式");
                } else {
                    frequency = 16000;
                }
                break;
        }
    }

    //打印log
    private void printLog(final String resultString) {
        tv_audio_succeess.post(new Runnable() {
            @Override
            public void run() {
                tv_audio_succeess.append(resultString + "\n");
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    //获取/失去焦点
    private void ButtonEnabled(boolean start, boolean stop, boolean play) {
        startAudio.setEnabled(start);
        stopAudio.setEnabled(stop);
        playAudio.setEnabled(play);
    }

    //开始录音
    public void StartRecord() {
        Log.d(TAG, "开始录音");
        //16K/8K采集率
//        frequency = 16000;
        //格式  单声道
        int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
        // 16Bit /8Bit
//        int audioEncoding = AudioFormat.ENCODING_PCM_8BIT;
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        //生成PCM文件
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "voice_master");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        file = new File(dir, System.currentTimeMillis() + "Bai.pcm");
        Log.d(TAG, "生成文件，名字是: " + file.toString());
        //如果存在，就先删除再创建
        if (file.exists())
            file.delete();
        Log.d(TAG, "删除已经存在的文件");
        try {
            file.createNewFile();
            Log.d(TAG, "创建文件，路径是" + file.toString());
        } catch (IOException e) {
            Log.d(TAG, "未能创建");
            throw new IllegalStateException("未能创建" + file.toString());
        }
        try {
            //输出流
            OutputStream os = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);
            int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, bufferSize);

            short[] buffer = new short[bufferSize];
            audioRecord.startRecording();
            Log.d(TAG, "开始录音");
            isRecording = true;
            while (isRecording) {
                int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
                for (int i = 0; i < bufferReadResult; i++) {
                    dos.writeShort(buffer[i]);
                }
            }
            audioRecord.stop();
            dos.close();
        } catch (Throwable t) {
            Log.d(TAG, "录音失败");
        }
    }

    //播放文件
    public void PlayRecord() {
        if (file == null) {
            return;
        }
        //读取文件
        int musicLength = (int) (file.length() / 2);
        short[] music = new short[musicLength];
        try {
            InputStream is = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);
            int i = 0;
            while (dis.available() > 0) {
                music[i] = dis.readShort();
                i++;
            }
            dis.close();
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    16000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    musicLength * 2,
                    AudioTrack.MODE_STREAM);
            audioTrack.play();
            audioTrack.write(music, 0, musicLength);
            audioTrack.stop();
        } catch (Throwable t) {
            Log.d(TAG, "播放失败");
        }
    }

    //删除文件
    private void deleFile() {
        if (file == null || !file.exists()) {
            return;
        }
        file.delete();
        printLog("文件删除成功");
    }

    //转写
    private void toWord() {
        // 获取上传任务ID
        String task_id = "";
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("has_participle", "true");
        //合并后标准版开启电话版功能
        //params.put("has_seperate", "true");
        try {
            // 上传音频文件
//            Message uploadMsg = lc.lfasrUpload(local_file, type, params);
            Message uploadMsg = lc.lfasrUpload(local_file, type, params);

            // 判断返回值
            int ok = uploadMsg.getOk();
            if (ok == 0) {
                // 创建任务成功
                task_id = uploadMsg.getData();
                System.out.println("task_id=" + task_id);
            } else {
                // 创建任务失败-服务端异常
                System.out.println("ecode=" + uploadMsg.getErr_no());
                System.out.println("failed=" + uploadMsg.getFailed());
            }
        } catch (LfasrException e) {
            // 上传异常，解析异常描述信息
            Message uploadMsg = JSON.parseObject(e.getMessage(), Message.class);
            System.out.println("ecode=" + uploadMsg.getErr_no());
            System.out.println("failed=" + uploadMsg.getFailed());
        }

        // 循环等待音频处理结果
        while (true) {
            try {
                // 等待20s在获取任务进度
                Thread.sleep(sleepSecond * 1000);
                System.out.println("waiting ...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                // 获取处理进度
                Message progressMsg = lc.lfasrGetProgress(task_id);

                // 如果返回状态不等于0，则任务失败
                if (progressMsg.getOk() != 0) {
                    System.out.println("task was fail. task_id:" + task_id);
                    System.out.println("ecode=" + progressMsg.getErr_no());
                    System.out.println("failed=" + progressMsg.getFailed());

                    return;
                } else {
                    ProgressStatus progressStatus = JSON.parseObject(progressMsg.getData(), ProgressStatus.class);
                    if (progressStatus.getStatus() == 9) {
                        // 处理完成
                        System.out.println("task was completed. task_id:" + task_id);
                        break;
                    } else {
                        // 未处理完成
                        System.out.println("task is incomplete. task_id:" + task_id + ", status:" + progressStatus.getDesc());
                        continue;
                    }
                }
            } catch (LfasrException e) {
                // 获取进度异常处理，根据返回信息排查问题后，再次进行获取
                Message progressMsg = JSON.parseObject(e.getMessage(), Message.class);
                System.out.println("ecode=" + progressMsg.getErr_no());
                System.out.println("failed=" + progressMsg.getFailed());
            }
        }

        // 获取任务结果
        try {
            Message resultMsg = lc.lfasrGetResult(task_id);
            // 如果返回状态等于0，则获取任务结果成功
            if (resultMsg.getOk() == 0) {
                // 打印转写结果
                System.out.println(resultMsg.getData());
            } else {
                // 获取任务结果失败
                System.out.println("ecode=" + resultMsg.getErr_no());
                System.out.println("failed=" + resultMsg.getFailed());
            }
        } catch (LfasrException e) {
            // 获取结果异常处理，解析异常描述信息
            Message resultMsg = JSON.parseObject(e.getMessage(), Message.class);
            System.out.println("ecode=" + resultMsg.getErr_no());
            System.out.println("failed=" + resultMsg.getFailed());
        }
    }

    //pcm转wav
    private void pcmToWav(String path) {
        //按原路径把音频文件后缀改一下;
        final String outpath = path.replace(".pcm", ".wav");
        pcmToWavUtil.pcmToWav(path, outpath);
    }
}
