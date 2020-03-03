package com.amr.mineapps.reindeerme;

/**
 * Created by Amr on 19-Jun-19.
 */

public class UserFB {

    private String UID;
    private String Email;
    private String Name;
    private String PPURL;

    public UserFB() {

    }

    public UserFB(String UID, String Email, String Name) {
        this.UID = UID;
        this.Email = Email;
        this.Name = Name;
        this.PPURL = "default";

    }


    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String Email) {
        this.Email = Email;
    }

    public String getName() {
        return Name;
    }

    public void setName(String Name) {
        this.Name = Name;
    }

    public String getPPURL() {
        return PPURL;
    }

    public void setPPURL(String PPURL) {
        this.PPURL = PPURL;
    }



}
