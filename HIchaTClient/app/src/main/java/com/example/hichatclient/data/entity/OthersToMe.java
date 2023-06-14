package com.example.hichatclient.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(primaryKeys = {"userID", "objectID"})
public class OthersToMe {
    @NonNull
    private String userID;
    @NonNull
    private String objectID;

    @ColumnInfo(name = "object_name")
    private String objectName;
    @ColumnInfo(name = "object_profile")
    private byte[] objectProfile;
    @ColumnInfo(name = "user_response")
    private String userResponse;

    public OthersToMe(String userID, String objectID, String objectName, byte[] objectProfile, String userResponse) {
        this.userID = userID;
        this.objectID = objectID;
        this.objectName = objectName;
        this.objectProfile = objectProfile;
        this.userResponse = userResponse;
    }


    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getObjectID() {
        return objectID;
    }

    public void setObjectID(String objectID) {
        this.objectID = objectID;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public byte[] getObjectProfile() {
        return objectProfile;
    }

    public void setObjectProfile(byte[] objectProfile) {
        this.objectProfile = objectProfile;
    }

    public String getUserResponse() {
        return userResponse;
    }

    public void setUserResponse(String userResponse) {
        this.userResponse = userResponse;
    }
}
