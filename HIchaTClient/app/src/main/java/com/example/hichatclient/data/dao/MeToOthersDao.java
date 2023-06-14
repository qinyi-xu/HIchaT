package com.example.hichatclient.data.dao;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.hichatclient.data.entity.MeToOthers;
import com.example.hichatclient.data.entity.OthersToMe;
import com.example.hichatclient.data.entity.User;

import java.util.List;

@Dao
public interface MeToOthersDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMeToOthers(MeToOthers... meToOthers);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllMeToOthers(List<MeToOthers> meToOthers);

    @Delete
    void deleteMeToOthers(MeToOthers... meToOthers);

    @Update
    void updateMeToOthers(MeToOthers... meToOthers);

    @Query("SELECT * FROM MeToOthers WHERE userID LIKE :userID")
    LiveData<List<MeToOthers>> getAllMeToOthers(String userID);


    @Query("SELECT * FROM MeToOthers WHERE userID LIKE :userID AND objectID LIKE :objectID")
    MeToOthers getMeToOthersByObjectID(String userID, String objectID);
}
