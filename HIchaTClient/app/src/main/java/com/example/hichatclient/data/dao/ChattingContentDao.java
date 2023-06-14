package com.example.hichatclient.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.hichatclient.data.entity.ChattingContent;

import java.util.List;

@Dao
public interface ChattingContentDao {
    @Insert
    void insertContent(ChattingContent... chattingContents);

    @Insert
    void insertAllContent(List<ChattingContent> chattingContents);

    @Update
    void updateAllContent(List<ChattingContent> chattingContents);

    @Update
    void updateOneContent(ChattingContent chattingContent);

    @Query("SELECT * FROM chattingcontent WHERE user_id LIKE :userID AND friend_id LIKE :friendID ORDER BY msg_time")
    LiveData<List<ChattingContent>> findAllContent(String userID, String friendID);

    @Query("SELECT * FROM chattingcontent WHERE user_id LIKE :userID AND friend_id LIKE :friendID AND msg_type LIKE :type ORDER BY msg_time")
    LiveData<List<ChattingContent>> getAllReceiveMsgLive(String userID, String friendID, String type);

    @Query("SELECT * FROM chattingcontent WHERE user_id LIKE :userID AND friend_id LIKE :friendID AND is_read LIKE :isRead AND msg_time <= :time AND msg_type LIKE :type ORDER BY msg_time")
    List<ChattingContent> findAllContentNotRead(String userID, String friendID, boolean isRead, long time, String type);




}
