package com.example.hichatclient.viewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hichatclient.data.entity.User;
import com.example.hichatclient.dataResource.MeRepository;
import com.example.hichatclient.dataResource.UserRepository;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class MeViewModel extends AndroidViewModel {
    private MeRepository meRepository;
    private UserRepository userRepository;
    private LiveData<User> user;

    public MeViewModel(@NonNull Application application) {
        super(application);
        meRepository = new MeRepository(application);
    }

    public LiveData<User> getUser() {
        return user;
    }

    public void setUser(LiveData<User> user) {
        this.user = user;
    }

    public LiveData<User> getUserInfo(String userID){
        return meRepository.getUserInfo(userID);
    }

    public void insertUser(User user) throws InterruptedException {
        userRepository.insertUser(user);
    }

    public int updateUserProfileToServer(String userShortToken, byte[] userNewProfile, Socket socket) throws IOException {
        return userRepository.updateUserProfileToServer(userShortToken, userNewProfile, socket);
    }


}
