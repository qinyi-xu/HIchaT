package com.example.hichatclient.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.hichatclient.data.entity.ChattingContent;
import com.example.hichatclient.data.entity.ChattingFriend;

import java.util.List;

@Dao
public interface ChattingFriendDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChattingFriend(ChattingFriend... chattingFriends);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllChattingFriend(List<ChattingFriend> chattingFriends);

    @Delete
    void deleteChattingFriend(ChattingFriend chattingFriend);

    @Query("SELECT * FROM chattingfriend WHERE userID LIKE :userID")
    LiveData<List<ChattingFriend>> findAllChattingFriend(String userID);

    @Query("SELECT * FROM chattingfriend WHERE userID LIKE :userID AND friendID LIKE :friendID")
    ChattingFriend findOneChattingFriend(String userID, String friendID);
}
