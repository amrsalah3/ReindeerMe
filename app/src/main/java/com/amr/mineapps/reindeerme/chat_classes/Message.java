package com.amr.mineapps.reindeerme.chat_classes;

public class Message {
    private String msg_content;
    private int msg_type;
    private int msg_status;
    private String reply_key;
    private int deleted;
    private String sender_id;
    private String sender_name;
    private String mKey;
    private int mSentOrRecieved;
    private String data;
    private long date_in_millis;

    public Message() {

    }

    public Message(String msg_content, int msg_type, int msg_status, String reply_key, int deleted, String sender_id, String sender_name, String mKey, int mSentOrRecieved, String data, long date_in_millis) {
        this.msg_content = msg_content;
        this.msg_type = msg_type;
        this.msg_status = msg_status;
        this.reply_key = reply_key;
        this.deleted = deleted;
        this.sender_id = sender_id;
        this.sender_name = sender_name;
        this.mKey = mKey;
        this.mSentOrRecieved = mSentOrRecieved;
        this.data = data;
        this.date_in_millis = date_in_millis;
    }

    public String getMsg_content() {
        return msg_content;
    }

    public void setMsg_content(String msg_content) {
        this.msg_content = msg_content;
    }

    public int getMsg_type() {
        return msg_type;
    }

    public void setMsg_type(int msg_type) {
        this.msg_type = msg_type;
    }

    public int getMsg_status() {
        return msg_status;
    }

    public void setMsg_status(int msg_status) {
        this.msg_status = msg_status;
    }

    public String getReply_key() {
        return reply_key;
    }

    public void setReply_key(String reply_key) {
        this.reply_key = reply_key;
    }

    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    public String getSender_id() {
        return sender_id;
    }

    public void setSender_id(String sender_id) {
        this.sender_id = sender_id;
    }

    public String getSender_name() {
        return sender_name;
    }

    public void setSender_name(String sender_name) {
        this.sender_name = sender_name;
    }

    public String getmKey() {
        return mKey;
    }

    public void setmKey(String mKey) {
        this.mKey = mKey;
    }

    public int getmSentOrRecieved() {
        return mSentOrRecieved;
    }

    public void setmSentOrRecieved(int mSentOrRecieved) {
        this.mSentOrRecieved = mSentOrRecieved;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public long getDate_in_millis() {
        return date_in_millis;
    }

    public void setDate_in_millis(long date_in_millis) {
        this.date_in_millis = date_in_millis;
    }

    @Override
    public String toString() {
        return "Message{" +
                "msg_content='" + msg_content + '\'' +
                ", msg_type=" + msg_type +
                ", msg_status=" + msg_status +
                ", reply_key='" + reply_key + '\'' +
                ", deleted=" + deleted +
                ", sender_id='" + sender_id + '\'' +
                ", sender_name='" + sender_name + '\'' +
                ", mKey='" + mKey + '\'' +
                ", mSentOrRecieved=" + mSentOrRecieved +
                ", data='" + data + '\'' +
                ", date_in_millis='" + date_in_millis + '\'' +
                '}';
    }
}