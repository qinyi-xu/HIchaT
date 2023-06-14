package com.example.hichatclient.data.entity;

import android.graphics.Bitmap;

public class SearchResult {
    private String resultID;
    private String resultName;
    private byte[] resultProfile;

    public byte[] getResultProfile() {
        return resultProfile;
    }

    public void setResultProfile(byte[] resultProfile) {
        this.resultProfile = resultProfile;
    }

    public String getResultID() {
        return resultID;
    }

    public void setResultID(String resultID) {
        this.resultID = resultID;
    }

    public String getResultName() {
        return resultName;
    }

    public void setResultName(String resultName) {
        this.resultName = resultName;
    }

    public SearchResult(String resultID, String resultName, byte[] resultProfile) {
        this.resultID = resultID;
        this.resultName = resultName;
        this.resultProfile = resultProfile;
    }


}
