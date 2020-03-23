package com.example.voicemaster.keyword.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class KeyWordBean {

    /**
     * code : 0
     * data : {"ke":[{"score":"0.598","word":"计算机"},{"score":"0.590","word":"自然"},{"score":"0.581","word":"科学"},{"score":"0.570","word":"语言学"},{"score":"0.570","word":"领域"},{"score":"0.561","word":"研究"},{"score":"0.561","word":"处理"},{"score":"0.553","word":"通信"},{"score":"0.550","word":"人工智能"},{"score":"0.550","word":"系统"}]}
     * desc : success
     * sid : ltp00000001@dx4a810f1a863f000100
     */

    private String code;
    private DataBean data;
    private String desc;
    private String sid;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public static class DataBean {
        @SerializedName("ke")
        private List<KeyBean> key;

        public List<KeyBean> getKey() {
            return key;
        }

        public void setKey(List<KeyBean> key) {
            this.key = key;
        }

        public static class KeyBean {
            /**
             * score : 0.598
             * word : 计算机
             */

            private double score;
            private String word;

            public double getScore() {
                return score;
            }

            public void setScore(double score) {
                this.score = score;
            }

            public String getWord() {
                return word;
            }

            public void setWord(String word) {
                this.word = word;
            }
        }
    }
}
