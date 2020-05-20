package com.example.voicemaster.translate.bean;

public class TranslationBean {

    /**
     * code : 0
     * message : success
     * sid : its....
     * data : {"result":{"from":"cn","to":"en","trans_result":{"dst":"Hello World ","src":"你好世界"}}}
     */

    private int code;
    private String message;
    private String sid;
    private DataBean data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean {
        /**
         * result : {"from":"cn","to":"en","trans_result":{"dst":"Hello World ","src":"你好世界"}}
         */

        private ResultBean result;

        public ResultBean getResult() {
            return result;
        }

        public void setResult(ResultBean result) {
            this.result = result;
        }

        public static class ResultBean {
            /**
             * from : cn
             * to : en
             * trans_result : {"dst":"Hello World ","src":"你好世界"}
             */

            private String from;
            private String to;
            private TransResultBean trans_result;

            public String getFrom() {
                return from;
            }

            public void setFrom(String from) {
                this.from = from;
            }

            public String getTo() {
                return to;
            }

            public void setTo(String to) {
                this.to = to;
            }

            public TransResultBean getTrans_result() {
                return trans_result;
            }

            public void setTrans_result(TransResultBean trans_result) {
                this.trans_result = trans_result;
            }

            public static class TransResultBean {
                /**
                 * dst : Hello World
                 * src : 你好世界
                 */

                private String dst;
                private String src;

                public String getDst() {
                    return dst;
                }

                public void setDst(String dst) {
                    this.dst = dst;
                }

                public String getSrc() {
                    return src;
                }

                public void setSrc(String src) {
                    this.src = src;
                }
            }
        }
    }
}
