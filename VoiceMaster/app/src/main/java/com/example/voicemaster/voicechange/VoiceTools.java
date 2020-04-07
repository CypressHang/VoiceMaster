package com.example.voicemaster.voicechange;

/**
 * Created by AxeChen on 2018/1/2.
 */

public class VoiceTools {

    public static native void  changeVoice(String path, int mode);

    static {
        System.loadLibrary("changeVoice");
        System.loadLibrary("fmod");
        System.loadLibrary("fmodL");
    }

}
