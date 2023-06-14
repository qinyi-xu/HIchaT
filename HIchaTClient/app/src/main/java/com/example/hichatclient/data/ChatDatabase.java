package com.example.hichatclient.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.hichatclient.data.dao.ChattingContentDao;
import com.example.hichatclient.data.dao.ChattingFriendDao;
import com.example.hichatclient.data.dao.FriendDao;
import com.example.hichatclient.data.dao.MeToOthersDao;
import com.example.hichatclient.data.dao.OthersToMeDao;
import com.example.hichatclient.data.dao.UserDao;
import com.example.hichatclient.data.entity.ChattingContent;
import com.example.hichatclient.data.entity.ChattingFriend;
import com.example.hichatclient.data.entity.Friend;
import com.example.hichatclient.data.entity.MeToOthers;
import com.example.hichatclient.data.entity.OthersToMe;
import com.example.hichatclient.data.entity.User;

// singleton
@Database(entities = {User.class, Friend.class, ChattingContent.class, MeToOthers.class, OthersToMe.class, ChattingFriend.class}, version = 1, exportSchema = false)
public abstract class ChatDatabase extends RoomDatabase {
    private static ChatDatabase INSTANCE;
    public static synchronized ChatDatabase getDatabase(Context context){
        if (INSTANCE == null){
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), ChatDatabase.class, "chat_database")
                    .build();
        }
        return INSTANCE;
    }

    public abstract UserDao getUserDao();

    public abstract FriendDao getFriendDao();

    public abstract ChattingContentDao getChattingContentDao();

    public abstract MeToOthersDao getMeToOthersDao();

    public abstract OthersToMeDao getOthersToMeDao();

    public abstract ChattingFriendDao getChattingFriendDao();
}
