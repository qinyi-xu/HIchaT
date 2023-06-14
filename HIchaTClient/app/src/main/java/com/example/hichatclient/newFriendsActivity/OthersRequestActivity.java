package com.example.hichatclient.newFriendsActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
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
import com.example.hichatclient.data.entity.OthersToMe;
import com.example.hichatclient.viewModel.OthersRequestViewModel;

import java.io.IOException;
import java.net.Socket;

public class OthersRequestActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private OthersRequestViewModel othersRequestViewModel;
    private ApplicationUtil applicationUtil;
    private Socket socket;

    private OthersToMe othersToMe;
    private String objectID;

    // UI控件
    private ImageView imageViewObjectImage;
    private TextView textViewObjectID;
    private TextView textViewObjectName;
    private Button buttonRefuse;
    private Button buttonAgree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_others_request);

        applicationUtil = (ApplicationUtil) OthersRequestActivity.this.getApplication();
        othersRequestViewModel = new ViewModelProvider(this).get(OthersRequestViewModel.class);

        textViewObjectID = findViewById(R.id.textViewObjectID3);
        textViewObjectName = findViewById(R.id.textViewObjectName3);
        buttonAgree = findViewById(R.id.buttonAgree);
        buttonRefuse = findViewById(R.id.buttonRefuse);
        imageViewObjectImage = findViewById(R.id.imageView2);

        // 获取Share Preferences中的数据
        sharedPreferences = getSharedPreferences("MY_DATA", Context.MODE_PRIVATE);
        final String userID = sharedPreferences.getString("userID", "fail");

        // 获取applicationUtil中的数据
        final String userShortToken = applicationUtil.getUserShortToken();
        if (!applicationUtil.staticIsConnected()) {
            try {
                applicationUtil.initSocketStatic();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket = applicationUtil.getSocketStatic();

        // 获取NewFriendActivity传来的参数
        objectID = getIntent().getStringExtra("objectID");

        try {
            othersToMe = othersRequestViewModel.getOthersToMeByObjectID(userID, objectID);  // 通过objectID获取OtherToMe的具体信息
            if (othersToMe.getUserResponse().equals("agree") || othersToMe.getUserResponse().equals("refuse")){
                buttonAgree.setEnabled(false);
                buttonRefuse.setEnabled(false);
            }
            if (othersToMe.getObjectProfile() != null){
                imageViewObjectImage.setImageBitmap(toRoundCorner(BitmapFactory.decodeByteArray(othersToMe.getObjectProfile(), 0, othersToMe.getObjectProfile().length), 2));
            }else {
                imageViewObjectImage.setImageResource(R.drawable.head);
            }
            textViewObjectID.setText(othersToMe.getObjectID());
            textViewObjectName.setText(othersToMe.getObjectName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 当用户拒绝别人的好友请求时
        buttonRefuse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            othersRequestViewModel.othersToMeResponseToServer(userShortToken, objectID, true, socket);  // 告诉服务器用户的回应
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

                othersToMe.setUserResponse("refuse");
                othersRequestViewModel.updateOthersToMeResponse(othersToMe);  // 更新数据库中OthersToMe的信息

                Intent intent = new Intent();
                intent.setClass(v.getContext(), NewFriendsActivity.class);
                startActivity(intent);
            }
        });

        // 当用户同意别人的好友请求时
        buttonAgree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            othersRequestViewModel.othersToMeResponseToServer(userShortToken, objectID, false, socket);  // 告诉服务器用户的回应
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
                othersToMe.setUserResponse("agree");
                othersRequestViewModel.updateOthersToMeResponse(othersToMe);  // 更新数据库中的OthersToMe信息
                Friend friend = new Friend(userID, othersToMe.getObjectID(), othersToMe.getObjectName(), othersToMe.getObjectProfile(), "null", "null");
                othersRequestViewModel.insertNewFriendIntoSQL(friend);  // 更新数据库中的Friend信息
                ChattingFriend chattingFriend = new ChattingFriend(userID, friend.getFriendID(), friend.getFriendName(), friend.getFriendProfile(), "We are new friends", System.currentTimeMillis());
                othersRequestViewModel.updateChattingFriendIntoSQL(chattingFriend);  // 更新数据库中的ChattingFriend信息

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("deleteFlag", "false");
                editor.apply();

                Intent intent = new Intent();
                intent.setClass(v.getContext(), NewFriendsActivity.class);
                startActivity(intent);

            }
        });

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