package com.example.hichatclient.viewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.hichatclient.data.entity.ChattingContent;
import com.example.hichatclient.data.entity.ChattingFriend;
import com.example.hichatclient.data.entity.Friend;
import com.example.hichatclient.data.entity.User;
import com.example.hichatclient.dataResource.FriendsRepository;
import com.example.hichatclient.dataResource.MeRepository;
import com.example.hichatclient.dataResource.MessageRepository;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends AndroidViewModel {
    private MessageRepository messageRepository;
    private MeRepository meRepository;
    private FriendsRepository friendsRepository;
    private LiveData<Friend> friend;
    private LiveData<User> user;


    public ChatViewModel(@NonNull Application application) {
        super(application);
        messageRepository = new MessageRepository(application);
        meRepository = new MeRepository(application);
        friendsRepository = new FriendsRepository(application);
    }

    public LiveData<Friend> getFriend() {
        return friend;
    }

    public void setFriend(LiveData<Friend> friend) {
        this.friend = friend;
    }

    public LiveData<User> getUser() {
        return user;
    }

    public void setUser(LiveData<User> user) {
        this.user = user;
    }

    public boolean sendMessageToServer(ChattingContent chattingContent, String userShortToken, Socket socket) throws IOException {
        return messageRepository.sendMessageToServer(chattingContent, userShortToken, socket);
    }

    public LiveData<List<ChattingContent>> getAllMessageLive(String userID, String friendID) {
        return messageRepository.getChattingContentFromSQL(userID, friendID);
    }

    public LiveData<List<ChattingContent>> getAllReceiveMsgLive(String userID, String friendID, String type) {
        return messageRepository.getAllReceiveMsgLive(userID, friendID, type);
    }


    public void insertOneMessageIntoSQL(ChattingContent chattingContent) {
        messageRepository.insertOneMessageIntoSQL(chattingContent);
    }

    public void updateOneMessageIntoSQL(ChattingContent chattingContent) {
        messageRepository.updateOneMessageIntoSQL(chattingContent);
    }

    public void updateAllMessageIntoSQL(List<ChattingContent> chattingContents){
        messageRepository.updateAllMessageIntoSQL(chattingContents);
    }

    public void updateChattingFriendIntoSQL(ChattingFriend chattingFriend) {
        messageRepository.updateChattingFriendIntoSQL(chattingFriend);
    }

    public void sendReadMsgToServer(String userShortToken, String friendID, long time, Socket socket) throws IOException {
        messageRepository.sendReadMsgToServer(userShortToken, friendID, time, socket);
    }

    public LiveData<Friend> getFriendInfo(String userID, String friendID) {
        return friendsRepository.getFriendInfo(userID, friendID);
    }

    public LiveData<User> getUserInfoByUserID(String userID) {
        return meRepository.getLiveUserInfoByUserID(userID);
    }


    public int getChatRecord(String userID, String friendID, String userShortToken, Socket socket, Long chatRecordTime) throws IOException {
        return messageRepository.getChatRecord(userID, friendID, userShortToken, socket, chatRecordTime);
    }

    public List<ChattingContent> findAllContentNotRead(String userID, String friendID, boolean isRead, long time, String type) throws InterruptedException {
        return messageRepository.findAllContentNotRead(userID, friendID, isRead, time, type);
    }
}

