package com.example.hichatclient.mainActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;


import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.hichatclient.ApplicationUtil;
import com.example.hichatclient.R;
import com.example.hichatclient.baseActivity.BaseActivity;
import com.example.hichatclient.dataResource.AuthService;
import com.example.hichatclient.dataResource.TextToken;
import com.example.hichatclient.service.ChatService;
import com.example.hichatclient.viewModel.LogInViewModel;
import com.example.hichatclient.data.entity.User;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;


public class LogInFragment extends Fragment {
    private Button buttonLogIn;
    private Button buttonToSignUp;
    private EditText editTextUserID;
    private EditText editTextUserPassword;
    private LogInViewModel logInViewModel;
    private FragmentActivity activity;
    private SharedPreferences sharedPreferences;
    private ApplicationUtil applicationUtil;
    private Socket socket;
    private String isLogIn;
    private User user;


    public LogInFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log_in, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = requireActivity();
        logInViewModel = new ViewModelProvider(activity).get(LogInViewModel.class);
        applicationUtil = (ApplicationUtil) activity.getApplication();
        if (!applicationUtil.staticIsConnected()) {
            try {
                applicationUtil.initSocketStatic();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket = applicationUtil.getSocketStatic();

//        progressBar = activity.findViewById(R.id.spin_kit);
//        doubleBounce = new DoubleBounce();
//

        buttonLogIn = activity.findViewById(R.id.buttonLogIn);
        buttonToSignUp = activity.findViewById(R.id.buttonToSignUp);
        editTextUserID = activity.findViewById(R.id.userID);
        editTextUserPassword = activity.findViewById(R.id.userPassword1);

        buttonLogIn.setEnabled(false);

        final TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String userID = editTextUserID.getText().toString().trim();
                String userPassword = editTextUserPassword.getText().toString().trim();
                buttonLogIn.setEnabled(!userID.isEmpty() && !userPassword.isEmpty());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        editTextUserID.addTextChangedListener(textWatcher);
        editTextUserPassword.addTextChangedListener(textWatcher);

//        progressBar.setVisibility(View.GONE);

        buttonLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String userID = editTextUserID.getText().toString().trim();
                String userPassword = editTextUserPassword.getText().toString().trim();

                try {
//                    activity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            // 增加动画效果
//                            progressBar.setVisibility(View.VISIBLE);
//                            progressBar.setIndeterminateDrawable(doubleBounce);
//                        }
//                    });


                    System.out.println("LogInFragment userID: " + userID);
                    System.out.println("LogInFragment userPassword: " + userPassword);
                    Map<Integer, User> map = logInViewModel.sendIDAndPassword(userID, userPassword, socket);


                    for(Integer key:map.keySet()){
                        isLogIn = key.toString();
                        if (!isLogIn.equals("0")){
                            user = map.get(key);
                        }
                    }
                    //user = logInViewModel.sendIDAndPasswordTest(userID, userPassword); // 用于本地测试
                    if (isLogIn.equals("0")) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), "登录失败！", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else if (isLogIn.equals("3")){
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), "您的账号已被别人登录！", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        logInViewModel.insertUser(user);
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), "登录成功！", Toast.LENGTH_SHORT).show();
                            }
                        });

                        Intent service_intent = new Intent(activity, ChatService.class);
                        activity.startService(service_intent);

                        // 将用户的ID存到share preferences里面
                        sharedPreferences = activity.getSharedPreferences("MY_DATA", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("userID", userID);
                        editor.putString("deleteFlag", "false");
                        editor.apply();

                        // 将用户的tokens存到ApplicationUtil中作为全局变量
                        applicationUtil.setUserShortToken(user.getUserShortToken());
                        applicationUtil.setUserLongToken(user.getUserLongToken());
                        applicationUtil.setUserID(user.getUserID());
                        applicationUtil.setReceive(System.currentTimeMillis());

                        Thread thread1 = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                AuthService authService = new AuthService();
                                String access_token = authService.getAuth();
                                System.out.println("LogInFragment sentiment access token: " + access_token);
                                applicationUtil.setAccessToken(access_token);
                            }
                        });
                        thread1.start();
                        try {
                            thread1.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        Thread thread2 = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                TextToken textToken = new TextToken();
                                String text_access_token = textToken.getAuth();
                                System.out.println("LogInFragment text access token: " + text_access_token);
                                applicationUtil.setTextAccessToken(text_access_token);
                            }
                        });
                        thread2.start();
                        try {
                            thread2.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }



                        // 跳转至BaseActivity的MeFragment
                        Intent intent = new Intent();
                        intent.setClass(activity, BaseActivity.class);
                        intent.putExtra("isLogIn", isLogIn);
                        intent.putExtra("FragmentId", "-1");
                        startActivity(intent);


                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        buttonToSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController controller = Navigation.findNavController(v);
                controller.navigate(R.id.action_logInFragment_to_signUpFragment);
            }
        });


    }

}