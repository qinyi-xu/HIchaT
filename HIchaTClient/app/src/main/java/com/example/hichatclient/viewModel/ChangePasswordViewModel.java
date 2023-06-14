package com.example.hichatclient.viewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;

import com.example.hichatclient.data.entity.User;
import com.example.hichatclient.dataResource.MeRepository;

import java.io.IOException;
import java.net.Socket;

public class ChangePasswordViewModel extends AndroidViewModel {
    // TODO: Implement the ViewModel
    private MeRepository meRepository;

    public ChangePasswordViewModel(@NonNull Application application) {
        super(application);
        meRepository = new MeRepository(application);
    }


    public boolean checkOldPasswordIsRight(String userID, String oldPassword) throws InterruptedException {
        User user = meRepository.getUserInfoByUserID(userID);
        System.out.println("real password: " + user.getUserPassword());
        System.out.println("input password: " + oldPassword);
        return user.getUserPassword().equals(oldPassword);
    }

    public int updateUserPasswordToServer(String userShortToken, String userNewPassword, Socket socket) throws IOException {
        return meRepository.updateUserPasswordToServer(userShortToken, userNewPassword, socket);
    }

    public User getUserInfoByUserID(String userID) throws InterruptedException {
        return meRepository.getUserInfoByUserID(userID);
    }

    public void updateUserInfoInSQL(User user){
        meRepository.updateUserInfoInSQL(user);
    }



}