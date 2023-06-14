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

import java.util.List;

@Dao
public interface OthersToMeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOthersToMe(OthersToMe... othersToMes);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllOthersToMe(List<OthersToMe> othersToMes);

    @Delete
    void deleteOthersToMe(OthersToMe... othersToMes);

    @Update
    void updateOthersToMe(OthersToMe... othersToMes);

    @Query("SELECT * FROM OthersToMe WHERE userID LIKE :userID ")
    LiveData<List<OthersToMe>> getAllOthersToMe(String userID);

    @Query("SELECT * FROM OthersToMe WHERE userID LIKE :userID AND objectID LIKE :objectID")
    OthersToMe getOthersToMeByObjectID(String userID, String objectID);
}
