package com.example.voicemaster.audio;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voicemaster.MainActivity;
import com.example.voicemaster.R;
import com.example.voicemaster.tool.VocalVerify;
import com.example.voicemaster.tool.VoiceToWord;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

    private ScrollView mScrollView;
    private TextView tv_audio_succeess;

    //pcm文件
    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_record);

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
        jumpWord = (Button) findViewById(R.id.jump_word);
        jumpWord.setOnClickListener(this);
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
                        Log.d(TAG,"start");
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
                ButtonEnabled(true, false, false);
                printLog("播放录音");
                break;
            case R.id.jump_word:
                if(file == null ){
                    Log.d(TAG, "onClick: 还没创建文件呢，请先录音！");
                    Toast.makeText(this, "还没创建文件呢，请先录音！", Toast.LENGTH_SHORT).show();
                    break;
                }
                VoiceToWord.RecordSoundFile = file;
                Toast.makeText(this, "文件路径是" + file.toString(), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SoundRecord.this, VoiceToWord.class));
                break;
            case R.id.deleteAudio:
                deleFile();
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
        Log.d(TAG,"开始录音");
        //16K采集率
        int frequency = 16000;
        //格式
        int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        //16Bit
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        //生成PCM文件
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"voice_master");
        if(!dir.exists()){
            dir.mkdirs();
        }
        file = new File(dir , System.currentTimeMillis()+"Bai.pcm");
        Log.d(TAG,"生成文件");
        //如果存在，就先删除再创建
        if (file.exists())
            file.delete();
        Log.d(TAG,"删除已经存在的文件");
        try {
            file.createNewFile();
            Log.d(TAG,"创建文件，路径是" + file.toString());
        } catch (IOException e) {
            Log.d(TAG,"未能创建");
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
        if(file == null){
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
        if(file == null || !file.exists()){
            return;
        }
        file.delete();
        printLog("文件删除成功");
    }

}
