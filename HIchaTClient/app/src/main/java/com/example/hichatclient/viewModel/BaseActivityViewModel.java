package com.example.hichatclient.viewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.hichatclient.dataResource.FriendsRepository;
import com.example.hichatclient.dataResource.MeRepository;

import java.io.IOException;
import java.net.Socket;

public class BaseActivityViewModel extends AndroidViewModel {
    private FriendsRepository friendsRepository;
    private MeRepository meRepository;

    public BaseActivityViewModel(@NonNull Application application) {
        super(application);
        friendsRepository = new FriendsRepository(application);
        meRepository = new MeRepository(application);
    }


    public void getUserFriendsFromServer(String userID, String userShortToken, Socket socket) throws IOException {
        friendsRepository.getUserFriendsFromServer(userID, userShortToken, socket);
    }

    public void deleteFriendInSQL(String userID, String friendID){
        friendsRepository.deleteFriendInSQL(userID, friendID);
    }




}
