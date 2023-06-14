package com.example.hichatclient.chatActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.hichatclient.ApplicationUtil;
import com.example.hichatclient.R;
import com.example.hichatclient.baseActivity.BaseActivity;
import com.example.hichatclient.data.entity.ChattingFriend;
import com.example.hichatclient.data.entity.Friend;
import com.example.hichatclient.newFriendsActivity.AddNewFriendActivity;
import com.example.hichatclient.viewModel.FriendInfoViewModel;

import java.io.IOException;
import java.net.Socket;

public class FriendInfoActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private ApplicationUtil applicationUtil;
    private FriendInfoViewModel friendInfoViewModel;
    private String userShortToken;
    private Socket socket;
    private String friendID;
    private String userID;
    private String flag;
    private String friendName;



    // UI控件
    ImageView imageViewFriendProfile;
    TextView textViewFriendID;
    TextView textViewFriendName;
    Button buttonSendMessage;
    Button buttonDeleteFriend;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_info);

        textViewFriendID = findViewById(R.id.textViewFriendID9);
        textViewFriendName = findViewById(R.id.textViewFriendName9);
        buttonSendMessage = findViewById(R.id.buttonSendMessageTo);
        buttonDeleteFriend = findViewById(R.id.buttonDeleteFriend);
        imageViewFriendProfile = findViewById(R.id.imageView4);

        friendInfoViewModel = new ViewModelProvider(this).get(FriendInfoViewModel.class);

        // 获取applicationUtil中的数据
        applicationUtil = (ApplicationUtil) FriendInfoActivity.this.getApplication();
        if (!applicationUtil.staticIsConnected()) {
            try {
                applicationUtil.initSocketStatic();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket = applicationUtil.getSocketStatic();
        userShortToken = applicationUtil.getUserShortToken();
        System.out.println("FriendInfoActivity userShortToken: " + userShortToken);

        // 获取Share Preferences中的数据
        sharedPreferences = getSharedPreferences("MY_DATA", Context.MODE_PRIVATE);
        userID = sharedPreferences.getString("userID", "fail");
        flag = sharedPreferences.getString("deleteFlag", "fail");

        // 获取ContactsFragment传来的参数
        friendID = getIntent().getStringExtra("friendID");
        System.out.println("FriendInfoActivity friendID: " + friendID);

        textViewFriendID.setText(friendID);

        friendInfoViewModel.getFriendInfo(userID, friendID).observe(this, new Observer<Friend>() {
            @Override
            public void onChanged(Friend friend) {
                System.out.println("FriendInfoActivity: " + friend.getFriendName());
                textViewFriendName.setText(friend.getFriendName());
                friendName = friend.getFriendName();
                if (friend.getFriendProfile() != null){
                    imageViewFriendProfile.setImageBitmap(toRoundCorner(BitmapFactory.decodeByteArray(friend.getFriendProfile(), 0, friend.getFriendProfile().length), 2));
                }else {
                    imageViewFriendProfile.setImageResource(R.drawable.head);
                }
            }
        });


        // 点击发信息，跳转和好友的聊天界面
        buttonSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ChatActivity.class);
                intent.putExtra("friendID", friendID);
                intent.putExtra("friendName", friendName);
                startActivity(intent);
            }
        });

        buttonDeleteFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                AlertDialog.Builder builder= new AlertDialog.Builder(v.getContext(), R.style.Theme_AppCompat_Light_Dialog_Alert);
                builder.setTitle("删除该联系人？");
                builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // 向服务器发送删好友请求
                                    flag = "true";
                                    friendInfoViewModel.deleteFriendToServer(friendID, userShortToken, socket);

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        t.start();
                        try {
                            t.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("deleteFlag", flag);
                        editor.apply();

                        Intent intent = new Intent();
                        intent.setClass(v.getContext(), BaseActivity.class);
                        intent.putExtra("isLogIn", "-1");
                        intent.putExtra("FragmentId", "1");
                        finish();
                        startActivity(intent);
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.create();
                builder.show();

            }
        });


    }

    @Override
    protected void onStop() {
        super.onStop();
        if (flag.equals("true")){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // 从本地数据库中删掉该好友
                    friendInfoViewModel.deleteFriendInSQL(userID, friendID);

                    // 从本地数据库中删掉聊天的好友
                    try {
                        ChattingFriend chattingFriend = friendInfoViewModel.findOneChattingFriend(userID, friendID);
                        if (chattingFriend != null){
                            friendInfoViewModel.deleteChattingFriend(chattingFriend);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static Bitmap toRoundCorner(Bitmap bitmap, float ratio) {
        System.out.println("图片是否变成圆形模式了+++++++++++++");
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rectF, bitmap.getWidth() / ratio,
                bitmap.getHeight() / ratio, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        System.out.println("pixels+++++++" + String.valueOf(ratio));

        return output;

    }

}