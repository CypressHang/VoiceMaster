package com.example.voicemaster.voicechange;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by l on 2018/2/28.
 */

public class AudioRecordUtils {

    private static final String TAG = "cypress";
    private int bufferSize;
    private boolean isRecording;
    private DataOutputStream dos;
    private AudioRecord audioRecord;
    private int sampleRateInHz = 16000;

    private AudioRecordUtils() {
        //设置音频的录制声道，CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
    }

    private static AudioRecordUtils mInstance;

    public static AudioRecordUtils getInstance() {
        if (mInstance == null) {
            synchronized (AudioRecordUtils.class) {
                if (mInstance == null) {
                    mInstance = new AudioRecordUtils();
                }
            }
        }
        return mInstance;
    }

    class RecordRunnable implements Runnable {

        private String originFilePath = null;
        private String wavFilePath = null;

        public RecordRunnable() {

        }

        public RecordRunnable(String originFilePath, String wavFilePath) {

            this.originFilePath = originFilePath;
            this.wavFilePath = wavFilePath;
        }

        @Override
        public void run() {
            isRecording = true;
            try {
                byte[] buffer = new byte[bufferSize];
                audioRecord.startRecording();
                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
//                        for (int i = 0; i < read; i++) {
//                            dos.writeShort(buffer[i]);
//                        }
                        dos.write(buffer);
                    }
                }
                audioRecord.stop();
                dos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (wavFilePath != null) {
                copyWaveFile(originFilePath, wavFilePath);
            }
        }
    }


    private void setFilePath(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    任何一种文件在头部添加相应的头文件才能够确定的表示这种文件的格式，wave是RIFF文件结构，
    每一部分为一个chunk，其中有RIFF WAVE chunk，
    FMT Chunk，Fact chunk,Data chunk,其中Fact chunk是可以选择的，
     */
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate,
                                     int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (1 * 16 / 8);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }


    private void copyWaveFile(String inFileName, String outFileName) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long longSampleRate = sampleRateInHz;
        long totalDataLen = totalAudioLen + 36;
        int channels = 1;//你录制是单声道就是1 双声道就是2（如果错了声音可能会急促等）
        long byteRate = 16 * longSampleRate * channels / 8;

        byte[] data = new byte[bufferSize];
        try {
            in = new FileInputStream(inFileName);
            out = new FileOutputStream(outFileName);

            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void startRecord(String path) {
        if (isRecording) {
            return;
        }
        setFilePath(path);
        startThread();
    }

    public void startRecord(String path, String wavFile) {
        if (isRecording) {
            return;
        }
        setFilePath(path);
        startThread(path, wavFile);
    }

    private void startThread() {
        Thread recordThread = new Thread(new RecordRunnable());
        recordThread.start();
    }

    private void startThread(String originFilePath, String wavFilePath) {
        Thread recordThread = new Thread(new RecordRunnable(originFilePath, wavFilePath));
        recordThread.start();
    }


    public void stopRecord() {
        isRecording = false;
    }
}

