package com.example.hichatclient.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(primaryKeys = {"userID", "friendID"})
public class Friend {

    @NonNull
    private String userID;
    @NonNull
    private String friendID;

    @ColumnInfo(name = "friend_name")
    private String friendName;
    @ColumnInfo(name = "friend_profile")
    private byte[] friendProfile;
    @ColumnInfo(name = "friend_ip")
    private String friendIP;
    @ColumnInfo(name = "friend_port")
    private String friendPort;

    public Friend(@NonNull String userID, @NonNull String friendID, String friendName, byte[] friendProfile, String friendIP, String friendPort) {
        this.userID = userID;
        this.friendID = friendID;
        this.friendName = friendName;
        this.friendProfile = friendProfile;
        this.friendIP = friendIP;
        this.friendPort = friendPort;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getFriendID() {
        return friendID;
    }

    public void setFriendID(String friendID) {
        this.friendID = friendID;
    }

    public String getFriendName() {
        return friendName;
    }

    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }

    public byte[] getFriendProfile() {
        return friendProfile;
    }

    public void setFriendProfile(byte[] friendProfile) {
        this.friendProfile = friendProfile;
    }

    public String getFriendIP() {
        return friendIP;
    }

    public void setFriendIP(String friendIP) {
        this.friendIP = friendIP;
    }

    public String getFriendPort() {
        return friendPort;
    }

    public void setFriendPort(String friendPort) {
        this.friendPort = friendPort;
    }

}
