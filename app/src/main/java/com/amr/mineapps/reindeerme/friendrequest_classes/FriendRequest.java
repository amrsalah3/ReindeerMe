package com.amr.mineapps.reindeerme.friendrequest_classes;

/**
 * Created by Amr on 24-Jun-19.
 */

public class FriendRequest {

    private String mReqPP;
    private String mReqName;
    private String mReqUid;

    public FriendRequest(String reqPP, String reqName, String reqUid) {
        mReqPP = reqPP;
        mReqName = reqName;
        mReqUid = reqUid;
    }

    public String getmReqPP() {
        return mReqPP;
    }

    public void setmReqPP(String mReqPP) {
        this.mReqPP = mReqPP;
    }

    public String getmReqName() {
        return mReqName;
    }

    public void setmReqName(String mReqName) {
        this.mReqName = mReqName;
    }

    public String getmReqUid() {
        return mReqUid;
    }

    public void setmReqUid(String mReqUid) {
        this.mReqUid = mReqUid;
    }
}
