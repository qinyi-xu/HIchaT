package com.example.hichatclient.viewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.hichatclient.data.entity.ChattingFriend;
import com.example.hichatclient.data.entity.Friend;
import com.example.hichatclient.data.entity.OthersToMe;
import com.example.hichatclient.dataResource.FriendsRepository;
import com.example.hichatclient.dataResource.MessageRepository;
import com.example.hichatclient.dataResource.NewFriendsRepository;

import java.io.IOException;
import java.net.Socket;

public class OthersRequestViewModel extends AndroidViewModel {
    NewFriendsRepository newFriendsRepository;
    FriendsRepository friendsRepository;
    MessageRepository messageRepository;

    public OthersRequestViewModel(@NonNull Application application) {
        super(application);
        newFriendsRepository = new NewFriendsRepository(application);
        friendsRepository = new FriendsRepository(application);
        messageRepository = new MessageRepository(application);
    }

    public OthersToMe getOthersToMeByObjectID(String userID, String objectID) throws InterruptedException {
        return newFriendsRepository.getOthersToMeByObjectID(userID, objectID);
    }

    public void othersToMeResponseToServer(String userShortToken, String objectID, boolean refuse, Socket socket) throws IOException {
        newFriendsRepository.othersToMeResponseToServer(userShortToken, objectID, refuse, socket);
    }

    public void updateOthersToMeResponse(OthersToMe othersToMe){
        newFriendsRepository.updateOthersToMeResponse(othersToMe);
    }

    public void insertNewFriendIntoSQL(Friend friend){
        friendsRepository.insertNewFriendIntoSQL(friend);
    }

    public void updateChattingFriendIntoSQL(ChattingFriend chattingFriend){
        messageRepository.updateChattingFriendIntoSQL(chattingFriend);
    }
}
