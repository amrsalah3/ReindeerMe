package com.amr.mineapps.reindeerme.friends_classes;
import com.google.firebase.database.DataSnapshot;
public class Friend {

    private String ppUrl;
    private String name;
    private String uid;
    private DataSnapshot lastMessage;
    private int lastMsgStatus;
    public Friend(String ppUrl, String name, String uid, DataSnapshot lastMsg, int lastMsgStatus){
        this.ppUrl = ppUrl;
        this.name = name;
        this.uid = uid;
        this.lastMessage = lastMsg;
        this.lastMsgStatus = lastMsgStatus;
    }



    public String getPpUrl() {
        return ppUrl;
    }

    public void setPpUrl(String ppUrl) {
        this.ppUrl = ppUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public DataSnapshot getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(DataSnapshot lastMessage) {
        this.lastMessage = lastMessage;
    }

    public int getLastMsgStatus() {
        return lastMsgStatus;
    }

    public void setLastMsgStatus(int lastMsgStatus) {
        this.lastMsgStatus = lastMsgStatus;
    }

    @Override
    public String toString() {
        return "Friend{" +
                "ppUrl='" + ppUrl + '\'' +
                ", name='" + name + '\'' +
                ", uid='" + uid + '\'' +
                ", lastMessage=" + lastMessage +
                '}';
    }
}

