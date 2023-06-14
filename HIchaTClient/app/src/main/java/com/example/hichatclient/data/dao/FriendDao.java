package com.example.hichatclient.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.hichatclient.data.entity.Friend;

import java.util.List;

@Dao
public interface FriendDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFriend(Friend... friends);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllFriend(List<Friend> friends);

    @Delete
    void deleteFriend(Friend... friends);

    @Update
    void updateFriend(Friend... friends);

    @Query("SELECT * FROM Friend Where userID LIKE :userID")
    LiveData<List<Friend>> getAllUserFriend(String userID);


    @Query("SELECT * FROM Friend Where friend_name LIKE :patten")
    LiveData<List<Friend>> findFriendsWithPatten(String patten);

    @Query("SELECT * FROM Friend Where userID LIKE :userID AND friendID LIKE :friendID")
    LiveData<Friend> getFriendInfo(String userID, String friendID);


    @Query("DELETE FROM Friend Where userID LIKE :userID AND friendID LIKE :friendID")
    void deleteOneFriend(String userID, String friendID);

    @Query("SELECT * FROM Friend Where userID LIKE :userID AND friendID LIKE :friendID")
    Friend getFriendInfo2(String userID, String friendID);

    @Query("SELECT * FROM Friend Where userID LIKE :userID")
    List<Friend> getUserFriendsInfoFromSQL(String userID);
}
