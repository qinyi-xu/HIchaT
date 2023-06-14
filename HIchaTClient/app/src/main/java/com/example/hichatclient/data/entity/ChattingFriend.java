package com.example.hichatclient.data.entity;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(primaryKeys = {"userID", "friendID"})
public class ChattingFriend {

    @NonNull
    private String userID;
    @NonNull
    private String friendID;

    @ColumnInfo(name = "friend_name")
    private String friendName;
    @ColumnInfo(name = "friend_profile")
    private byte[] friendProfile;
    @ColumnInfo(name = "the_last_msg")
    private String theLastMsg;
    @ColumnInfo(name = "time")
    private long time;


    public ChattingFriend(@NonNull String userID, @NonNull String friendID, String friendName, byte[] friendProfile, String theLastMsg, long time) {
        this.userID = userID;
        this.friendID = friendID;
        this.theLastMsg = theLastMsg;
        this.time = time;
        this.friendName = friendName;
        this.friendProfile = friendProfile;
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

    @NonNull
    public String getUserID() {
        return userID;
    }

    public void setUserID(@NonNull String userID) {
        this.userID = userID;
    }

    @NonNull
    public String getFriendID() {
        return friendID;
    }

    public void setFriendID(@NonNull String friendID) {
        this.friendID = friendID;
    }

    public String getTheLastMsg() {
        return theLastMsg;
    }

    public void setTheLastMsg(String theLastMsg) {
        this.theLastMsg = theLastMsg;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
