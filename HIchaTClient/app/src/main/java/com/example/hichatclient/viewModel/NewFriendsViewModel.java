package com.example.hichatclient.viewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.hichatclient.data.entity.Friend;
import com.example.hichatclient.data.entity.MeToOthers;
import com.example.hichatclient.data.entity.OthersToMe;
import com.example.hichatclient.dataResource.FriendsRepository;
import com.example.hichatclient.dataResource.NewFriendsRepository;

import java.util.List;

public class NewFriendsViewModel extends AndroidViewModel {
    private NewFriendsRepository newFriendsRepository;
    private FriendsRepository friendsRepository;


    public NewFriendsViewModel(@NonNull Application application) {
        super(application);
        newFriendsRepository = new NewFriendsRepository(application);
        friendsRepository = new FriendsRepository(application);
    }


    public LiveData<List<MeToOthers>> getAllMeToOthersFromSQL(String userID){
        return newFriendsRepository.getAllMeToOthersFromSQL(userID);
    }

    public LiveData<List<OthersToMe>> getAllOthersToMeFromSQL(String userID){
        return newFriendsRepository.getAllOthersToMeFromSQL(userID);
    }

    public void updateMeToOthers(List<MeToOthers> meToOthers){
        newFriendsRepository.updateMeToOthers(meToOthers);
    }

    public void updateOthersToMe(List<OthersToMe> othersToMes){
        newFriendsRepository.updateOthersToMe(othersToMes);
    }

    public void insertNewFriendIntoSQL(Friend friend){
        friendsRepository.insertNewFriendIntoSQL(friend);
    }

}
