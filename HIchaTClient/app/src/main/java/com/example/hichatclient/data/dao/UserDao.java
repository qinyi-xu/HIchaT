package com.example.hichatclient.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.hichatclient.data.entity.Friend;
import com.example.hichatclient.data.entity.User;

import java.util.List;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(User... users);

    @Delete
    void deleteUser(User... users);

    @Update
    void updateUser(User... users);

    @Query("SELECT * FROM User Where userID LIKE :userID")
    LiveData<User> getUserInfo(String userID);


    @Query("SELECT * FROM User Where userID LIKE :userID")
    User getUserByUserID(String userID);

    @Query("SELECT * FROM User Where userID LIKE :userID")
    LiveData<User> getLiveUserByUserID(String userID);

}
