package com.example.hichatclient.baseActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.example.hichatclient.ApplicationUtil;
import com.example.hichatclient.R;
import com.example.hichatclient.newFriendsActivity.AddNewFriendActivity;
import com.example.hichatclient.viewModel.BaseActivityViewModel;
import com.example.hichatclient.viewModel.LogInViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.net.Socket;

public class BaseActivity extends AppCompatActivity {
    private BaseActivityViewModel baseActivityViewModel;
    private SharedPreferences sharedPreferences;
    private ApplicationUtil applicationUtil;
    private Socket socket;
    private ContactsFragment contactsFragment;
    private NavController navController;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        // 设置底部导航栏
        final BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationViewBase);
        navController = Navigation.findNavController(this, R.id.fragment2);
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                if(destination.getId()==R.id.changeNameFragment||destination.getId()==R.id.changePasswordFragment){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bottomNavigationView.setVisibility(View.GONE);
                        }
                    });
                }else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bottomNavigationView.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });
        AppBarConfiguration configuration = new AppBarConfiguration.Builder(bottomNavigationView.getMenu()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, configuration);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);



        baseActivityViewModel = new ViewModelProvider(this).get(BaseActivityViewModel.class);

        // 获取Share Preferences中的数据
        sharedPreferences = getSharedPreferences("MY_DATA", Context.MODE_PRIVATE);
        final String userID = sharedPreferences.getString("userID", "fail");

        // 获取applicationUtil中的数据
        applicationUtil = (ApplicationUtil) this.getApplication();
        if (!applicationUtil.staticIsConnected()) {
            try {
                applicationUtil.initSocketStatic();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket = applicationUtil.getSocketStatic();
        final String userShortToken = applicationUtil.getUserShortToken();

        String isLogIn = getIntent().getStringExtra("isLogIn");
//        final String deleteId = getIntent().getStringExtra("deleteId");
//        if (!deleteId.equals("-1")){
//            Thread thread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    // 从本地数据库中删掉该好友
//                    System.out.println("BaseActivity deleteFriendInSQL");
//                    baseActivityViewModel.deleteFriendInSQL(userID, deleteId);
//                }
//            });
//            thread.start();
//            try {
//                thread.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }

        if (isLogIn.equals("2")){
             // 从服务器获取好友列表并存入数据库中
            new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        baseActivityViewModel.getUserFriendsFromServer(userID, userShortToken, socket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navController.navigateUp();
    }

    @Override
    public boolean onSupportNavigateUp() {
        navController.navigateUp();
        return super.onSupportNavigateUp();
    }

}