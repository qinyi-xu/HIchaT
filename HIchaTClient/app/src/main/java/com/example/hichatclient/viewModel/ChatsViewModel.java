package com.example.hichatclient.viewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.hichatclient.data.entity.ChattingFriend;
import com.example.hichatclient.data.entity.Friend;
import com.example.hichatclient.dataResource.MessageRepository;

import java.util.List;

public class ChatsViewModel extends AndroidViewModel {
    private MessageRepository messageRepository;

    public ChatsViewModel(@NonNull Application application) {
        super(application);
        messageRepository = new MessageRepository(application);
    }

    public LiveData<List<ChattingFriend>> getAllChattingFriendFromSQL(String userID){
        return messageRepository.getAllChattingFriendFromSQL(userID);
    }


}