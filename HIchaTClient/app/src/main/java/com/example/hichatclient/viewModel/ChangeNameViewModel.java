package com.example.hichatclient.viewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;

import com.example.hichatclient.data.entity.ChattingFriend;
import com.example.hichatclient.data.entity.User;
import com.example.hichatclient.dataResource.MeRepository;
import com.example.hichatclient.dataResource.MessageRepository;

import java.io.IOException;
import java.net.Socket;

public class ChangeNameViewModel extends AndroidViewModel {
    private MeRepository meRepository;
    private MessageRepository messageRepository;


    public ChangeNameViewModel(@NonNull Application application) {
        super(application);
        meRepository = new MeRepository(application);
        messageRepository = new MessageRepository(application);
    }

    public int updateUserNameToServer(String shortToken, String userNewName, Socket socket) throws IOException {
        return meRepository.updateUserNameToServer(shortToken, userNewName, socket);
    }

    public void updateUserInfoInSQL(User user){
        meRepository.updateUserInfoInSQL(user);
    }

    public User getUserInfoByUserID(String userID) throws InterruptedException {
        return meRepository.getUserInfoByUserID(userID);
    }

}