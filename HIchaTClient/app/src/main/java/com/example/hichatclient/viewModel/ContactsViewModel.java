package com.example.hichatclient.viewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.hichatclient.data.entity.ChattingFriend;
import com.example.hichatclient.data.entity.Friend;
import com.example.hichatclient.dataResource.FriendsRepository;
import com.example.hichatclient.dataResource.MessageRepository;

import java.util.List;

public class ContactsViewModel extends AndroidViewModel {
    // TODO: Implement the ViewModel
    private FriendsRepository friendsRepository;
    private MessageRepository messageRepository;



    public ContactsViewModel(@NonNull Application application) {
        super(application);
        friendsRepository = new FriendsRepository(application);
        messageRepository = new MessageRepository(application);
    }

    public LiveData<List<Friend>> getUserFriendsFromSQL(String userID) {
        return friendsRepository.getUserFriendsFromSQL(userID);
    }

    public LiveData<List<Friend>> findFriendsWithPatten(String patten){
        return friendsRepository.findFriendsWithPatten(patten);
    }


}