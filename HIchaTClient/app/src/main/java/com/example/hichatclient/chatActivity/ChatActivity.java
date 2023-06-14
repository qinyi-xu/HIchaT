package com.example.hichatclient.chatActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;

import android.net.sip.SipSession;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hichatclient.ApplicationUtil;
import com.example.hichatclient.R;
import com.example.hichatclient.data.entity.ChattingContent;
import com.example.hichatclient.data.entity.ChattingFriend;
import com.example.hichatclient.data.entity.Friend;
import com.example.hichatclient.data.entity.User;
import com.example.hichatclient.dataResource.TextToken;
import com.example.hichatclient.newFriendsActivity.AddNewFriendActivity;
import com.example.hichatclient.service.ChatService;
import com.example.hichatclient.viewModel.ChatViewModel;
import com.hjq.bar.OnTitleBarListener;
import com.hjq.bar.TitleBar;
import com.mordred.wordcloud.WordCloud;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.rockerhieu.emojicon.EmojiconEditText;
import io.github.rockerhieu.emojicon.EmojiconGridFragment;
import io.github.rockerhieu.emojicon.EmojiconsFragment;
import io.github.rockerhieu.emojicon.emoji.Emojicon;

public class ChatActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, EmojiconGridFragment.OnEmojiconClickedListener, EmojiconsFragment.OnEmojiconBackspaceClickedListener{
    private SharedPreferences sharedPreferences;
    private ApplicationUtil applicationUtil;
    private Socket socket;
    private ChatViewModel chatViewModel;

    // UI控件
    private Button buttonSend;
    private TextView textViewFriendTitle;
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private SwipeRefreshLayout swipeRefreshLayout = null;
    private TitleBar titleBar;

    private FrameLayout frameLayout;
    private EmojiconEditText editTextSendMsg;  // a EditText which can render emojis
    private ImageButton imageButtonFindEmoji;
    private boolean hasClick;



    // 用户和某好友的聊天信息
    private List<ChattingContent> allMessage = new ArrayList<>();
    private LiveData<List<ChattingContent>> allMessageLive;
    private LiveData<List<ChattingContent>> allReceiveMsgLive;
    private Friend friendChatting;
    private User userChatting;
    private boolean flag;
    private String userShortToken;
    private String friendID;
    private String friendName;
    private String msgSentiment;  // like喜爱, happy愉快, angry愤怒, disgusting厌恶, fearful恐惧, sad悲伤, neutral中性情绪, thinking无法判断
    private String msgContent;
    private String msgLegal = "1";
    private String userID;
    private long time;
    private boolean getRecordFlag;  // true: 用户刚刚拉取了历史聊天记录
    private int getRecordSize;  // 0:历史记录为空；1：拉取成功且有历史记录；2：拉取失败


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置软键盘弹出时把布局顶上去
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        setContentView(R.layout.activity_chat);

        // 隐藏标题栏
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        buttonSend = findViewById(R.id.buttonSend2);
        editTextSendMsg = findViewById(R.id.editTextSendContent);
        swipeRefreshLayout = findViewById(R.id.chatSwipeRefreshLayout);
        textViewFriendTitle = findViewById(R.id.textViewTitle);
        titleBar = findViewById(R.id.titleBar);
        frameLayout = findViewById(R.id.emojicons);
        imageButtonFindEmoji = findViewById(R.id.imageButtonFindEmoji);


        // 获取applicationUtil中的数据
        applicationUtil = (ApplicationUtil) ChatActivity.this.getApplication();
        if (!applicationUtil.staticIsConnected()) {
            System.out.println("ChatActivity init socket");
            try {
                applicationUtil.initSocketStatic();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket = applicationUtil.getSocketStatic();
        userShortToken = applicationUtil.getUserShortToken();

        setEmojiconFragment(false);


        // 获取Share Preferences中的数据
        sharedPreferences = getSharedPreferences("MY_DATA", Context.MODE_PRIVATE);
        userID = sharedPreferences.getString("userID", "fail");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("deleteFlag", "false");
        editor.apply();

        final SimpleDateFormat newSimpleDateFormat = new SimpleDateFormat(
                "yyyy年MM月dd日HH时mm分ss秒", Locale.getDefault());

        // 接收FriendInfoActivity传来的参数
        friendID = getIntent().getStringExtra("friendID");
        friendName = getIntent().getStringExtra("friendName");

        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        recyclerView = findViewById(R.id.recyclerViewChatContent);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        messageAdapter = new MessageAdapter();
        recyclerView.setAdapter(messageAdapter);

        // 以下为历史记录功能的UI实现
        // 设置下拉进度的背景颜色，默认白色
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.white));
        // 设置下拉进度的主题颜色
        swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(R.color.top1),
                getResources().getColor(R.color.top2),
                getResources().getColor(R.color.top3)
        );
        // 下拉时触发下拉动画，动画完毕之后回调该方法
        swipeRefreshLayout.setOnRefreshListener(this);


        // 标题栏设置
        if (friendName != null){
            titleBar.setTitle(friendName);
        }
        titleBar.setOnTitleBarListener(new OnTitleBarListener() {
            @Override
            public void onLeftClick(View v) {

            }

            @Override
            public void onTitleClick(View v) {

            }

            @Override
            public void onRightClick(View v) {
                Intent intent = new Intent(v.getContext(), WordCloudActivity.class);
                intent.putExtra("friendID", friendID);
                startActivity(intent);
            }
        });



        chatViewModel.getFriendInfo(userID, friendID).observe(this, new Observer<Friend>() {
            @Override
            public void onChanged(Friend friend) {
                friendChatting = friend;
                friendName = friend.getFriendName();
                messageAdapter.setBytesLeft(friendChatting.getFriendProfile());
            }
        });
        chatViewModel.getUserInfoByUserID(userID).observe(this, new Observer<User>() {
            @Override
            public void onChanged(User user) {
                userChatting = user;
                messageAdapter.setBytesRight(userChatting.getUserProfile());
            }
        });

        allMessage = chatViewModel.getAllMessageLive(userID, friendID).getValue();
        allMessageLive = chatViewModel.getAllMessageLive(userID, friendID);
        allReceiveMsgLive = chatViewModel.getAllReceiveMsgLive(userID, friendID, "receive");

        if (allMessage != null){
            System.out.println("ChatActivity: there are messages!!!");
            messageAdapter.setAllMsg(allMessage);
            messageAdapter.setBytesLeft(friendChatting.getFriendProfile());
            messageAdapter.setBytesRight(userChatting.getUserProfile());

        }

        // 当数据库中的聊天记录有变化时
        allMessageLive.observe(this, new Observer<List<ChattingContent>>() {
            @Override
            public void onChanged(List<ChattingContent> chattingContents) {
                if (chattingContents.size() > 0){
//                    final ChattingContent msg = chattingContents.get(chattingContents.size() - 1);
                    time = chattingContents.get(0).getMsgTime(); // 获取数据库中最早的聊天信息
                    // 更新数据库中的ChattingFriend信息
//                    assert userID != null;
//                    ChattingFriend chattingFriend = new ChattingFriend(userID, friend.getFriendID(), friend.getFriendName(), friend.getFriendProfile(), msg.getMsgContent(), msg.getMsgTime());
//                    chatViewModel.updateChattingFriendIntoSQL(chattingFriend);

                    messageAdapter.setAllMsg(chattingContents);
                    messageAdapter.notifyDataSetChanged();

                    if (getRecordFlag){ // 如果用户刚刚拉取历史记录
                        System.out.println("scrolltoPosition");
                        recyclerView.scrollToPosition(0);  // 将RecyclerView定位第一行

                        getRecordFlag = false;  // 初始化

                        if (getRecordSize == 0){
                            Toast.makeText(ChatActivity.this, "没有更多消息了！", Toast.LENGTH_SHORT).show();
                        }else if (getRecordSize == 2){
                            Toast.makeText(ChatActivity.this, "拉取失败！", Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        recyclerView.scrollToPosition(chattingContents.size()-1);  // 将RecyclerView定位在最后一行
                    }


//                    if (msg.getMsgType().equals("receive")){
//                        Thread t2 = new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                // 当用户打开与某个好友的对话框时
//                                // 若对方的最后一条消息没有被已读，则向服务器发送已读消息
//                                if (!msg.isRead()){
//                                    try {
//                                        System.out.println("call this function: sendReadMsgToServer2");
//                                        chatViewModel.sendReadMsgToServer(userShortToken, friendID, msg.getMsgTime(), socket);
//                                        System.out.println("用户发送已读消息的时间是：" + newSimpleDateFormat.format(msg.getMsgTime()));
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            }
//                        });
//                        t2.start();
//                        try {
//                            t2.join();
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }

                } else {
                    time = System.currentTimeMillis();
                }
            }
        });

        allReceiveMsgLive.observe(this, new Observer<List<ChattingContent>>() {
            @Override
            public void onChanged(List<ChattingContent> chattingContents) {
                if (chattingContents.size() > 0) {
                    final ChattingContent msg = chattingContents.get(chattingContents.size() - 1);

                    // 如果该消息没有情感分析，则发送http请求获取
                    if (msg.getSentiment() == null){
                        try {
                            msgContent = msg.getMsgContent();
                            Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        getSentimentFromBaidu();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            thread.start();
                            thread.join();
                            msg.setSentiment(msgSentiment);
                            chatViewModel.updateOneMessageIntoSQL(msg);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }


                    Thread t2 = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // 当用户打开与某个好友的对话框时
                            if (!msg.isRead()){ // 若对方的最后一条消息没有被已读
                                try {
                                    // 向服务器发送已读消息
                                    System.out.println("call this function: sendReadMsgToServer2");
                                    chatViewModel.sendReadMsgToServer(userShortToken, friendID, msg.getMsgTime(), socket);
                                    System.out.println("用户发送已读消息的时间是：" + newSimpleDateFormat.format(msg.getMsgTime()));
                                    // 将本地收到的对方的消息设置为已读
                                    List<ChattingContent> chattingContents1 = chatViewModel.findAllContentNotRead(userID, friendID, false, msg.getMsgTime(), "receive");
                                    List<ChattingContent> chattingContents2 = new ArrayList<>();
                                    if (chattingContents1 != null ){
                                        for (int j=0; j<chattingContents1.size(); j++){
                                            System.out.println("ChatActivity j: " + j);
                                            chattingContents1.get(j).setRead(true);
                                            chattingContents2.add(chattingContents1.get(j));
                                        }
                                    }
                                    chatViewModel.updateAllMessageIntoSQL(chattingContents2);
                                } catch (IOException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    t2.start();
                    try {
                        t2.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });



        // 当用户按下发送按钮时
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = editTextSendMsg.getText().toString();
                if(!"".equals(content)){
                    //如果字符串不为空，则创建ChattingContent对象
                    final ChattingContent msg = new ChattingContent(userID, friendID, "send", System.currentTimeMillis(), content, false, null);
                    ChattingFriend chattingFriend = new ChattingFriend(userID, friendID, friendChatting.getFriendName(), friendChatting.getFriendProfile(), msg.getMsgContent(), msg.getMsgTime());
                    String LogTime = newSimpleDateFormat.format(msg.getMsgTime());
                    System.out.println("用户发送消息的时间是: " + LogTime);
                    System.out.println("用户发送消息的内容是: " + content);

                    // 对用户发出的信息进行敏感词检测
//                    Thread thread = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            getLegalFromBaidu();
//                        }
//                    });
//                    thread.start();
//                    try {
//                        thread.join();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    if (msg.getMsgContent().contains("法轮功") || msg.getMsgContent().contains("傻逼") || msg.getMsgContent().contains("乙醚")){
                        msgLegal = "2";
                    }

                    if (msgLegal.equals("1")){
                        // 若用户信息合法
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    flag = chatViewModel.sendMessageToServer(msg, userShortToken, socket);
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
                        if (flag){
                            chatViewModel.insertOneMessageIntoSQL(msg); // 将该消息插入数据库中
                            chatViewModel.updateChattingFriendIntoSQL(chattingFriend);
                            editTextSendMsg.setText("");  // 清空输入框的内容
                            editTextSendMsg.requestFocus();  // 输入光标回到输入框中
                        } else {
                            Toast.makeText(v.getContext().getApplicationContext(), "发送失败！", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        // 若用户信息不合法
                        AlertDialog.Builder builder= new AlertDialog.Builder(v.getContext(), R.style.Theme_AppCompat_Light_Dialog_Alert);
                        builder.setTitle("您的消息包含敏感词，不允许发送！");
                        builder.setNeutralButton("知道了", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                msgLegal = "1";
                            }
                        });
                        builder.create();
                        builder.show();

                    }
                }
            }
        });


        // 当按下表情按钮时
        imageButtonFindEmoji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasClick){
                    frameLayout.setVisibility(View.GONE);
                }else {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0); //强制隐藏键盘
                    frameLayout.setVisibility(View.VISIBLE);
                }
                hasClick = !hasClick;
            }
        });

        // 点输入框时：隐藏emoji frame layout
        editTextSendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                frameLayout.setVisibility(View.GONE);
                hasClick = !hasClick;
            }
        });

    }

    private void setEmojiconFragment(boolean useSystemDefault) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.emojicons, EmojiconsFragment.newInstance(useSystemDefault))
                .commit();
    }

    @Override
    public void onEmojiconClicked(Emojicon emojicon) {
        EmojiconsFragment.input(editTextSendMsg, emojicon);
    }

    @Override
    public void onEmojiconBackspaceClicked(View v) {
        EmojiconsFragment.backspace(editTextSendMsg);
    }

    @Override
    public void onBackPressed() {
        if(hasClick){
            findViewById(R.id.emojicons).setVisibility(View.GONE);
            hasClick = !hasClick;
        }else {
            super.onBackPressed();
        }
    }


    public void getSentimentFromBaidu() throws InterruptedException {
        String access_token = applicationUtil.getAccessToken();
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            // 获取访问地址的url
            // 注意要有access_token参数
            // 接口默认支持的是GBK编码，若需要输入的文本为UTF-8编码，要在url上添加参数charset=UTF-8
            URL url = new URL("https://aip.baidubce.com/rpc/2.0/nlp/v1/emotion?access_token=" + access_token + "&charset=UTF-8");
            // 创建HttpURLConnection对象
            connection = (HttpURLConnection) url.openConnection();
            // 设置请求方法为POST
            connection.setRequestMethod("POST");
            // 允许往流中写数据
            connection.setDoOutput(true);
            // 允许从流中读数据
            connection.setDoInput(true);
            // 设置连接超时时间（毫秒）
            connection.setConnectTimeout(5000);
            // 设置读取超时时间（毫秒）
            connection.setReadTimeout(5000);
            // 设置header内的参数
            connection.setRequestProperty("Content-Type", "application/json");
            // 设置body内的参数
            JSONObject param = new JSONObject();
            param.put("scene", "talk");
            param.put("text", msgContent);

            // 建立实际的连接
            connection.connect();

            // 得到请求的输出流对象
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(),"UTF-8");
            writer.write(param.toString());  // 写入参数
            writer.flush();

            // 获取服务端响应，通过输入流来读取URL的响应
            InputStream is = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder result = new StringBuilder();
            String strRead = null;
            while ((strRead = reader.readLine()) != null) {
                result.append(strRead);
            }
            reader.close();

            // 将服务端返回的json数据进行转化
            JSONObject jsonObject = new JSONObject(result.toString());
            JSONObject jsonObject1 = new JSONObject(jsonObject.getJSONArray("items").getString(0));

            // 表示情感极性分类结果
            String sentiment = jsonObject1.getString("label");
            String sentiment_prob = jsonObject1.getString("prob");
            if (Float.parseFloat(sentiment_prob) < 0.3){
                System.out.println("ChatActivity sentiment: thinking");
                msgSentiment = "thinking";
            }else {
                if (sentiment.equals("neutral")){
                    System.out.println("ChatActivity sentiment: neutral");
                    msgSentiment = "neutral";
                }else {
                    JSONObject jsonObject2 = new JSONObject(jsonObject1.getJSONArray("subitems").getString(0));
                    String sub_sentiment = jsonObject2.getString("label");
                    System.out.println("ChatActivity sentiment: " + sub_sentiment);
                    msgSentiment = sub_sentiment;
                }
            }

            // 关闭连接
            connection.disconnect();

            // 打印读到的响应结果
            System.out.println("ChatActivity msg content: " + msgContent);
            System.out.println("ChatActivity msg sentiment: " + msgSentiment);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {//关闭连接
                connection.disconnect();
            }
        }
    }


    public void getLegalFromBaidu(){
        String isLegal = null;
        String access_token = applicationUtil.getTextAccessToken();
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            // 获取访问地址的url
            // 注意要有access_token参数
            // 接口默认支持的是GBK编码，若需要输入的文本为UTF-8编码，要在url上添加参数charset=UTF-8
            // 注意：urlencode格式化请求体
            URL url = new URL("https://aip.baidubce.com/rest/2.0/solution/v1/text_censor/v2/user_defined?access_token=" + access_token + "&charset=UTF-8" + "&text=" + msgContent);
            // 创建HttpURLConnection对象
            connection = (HttpURLConnection) url.openConnection();
            // 设置请求方法为POST
            connection.setRequestMethod("POST");
            // 允许从流中读数据
            connection.setDoInput(true);
            // 设置连接超时时间（毫秒）
            connection.setConnectTimeout(5000);
            // 设置读取超时时间（毫秒）
            connection.setReadTimeout(5000);
            // 设置header内的参数
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // 建立实际的连接
            connection.connect();

            // 获取服务端响应，通过输入流来读取URL的响应
            InputStream is = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder result = new StringBuilder();
            String strRead = null;
            while ((strRead = reader.readLine()) != null) {
                result.append(strRead);
            }
            reader.close();

            // 将服务端返回的json数据进行转化
            JSONObject jsonObject = new JSONObject(result.toString());
            isLegal = jsonObject.getString("conclusionType");


            // 关闭连接
            connection.disconnect();

            // 打印读到的响应结果
            System.out.println("ChatActivity msg content: " + msgContent);
            System.out.println("ChatActivity msg conclusionType: " + isLegal);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {//关闭连接
                connection.disconnect();
            }
        }
        msgLegal = isLegal;
    }


    @Override
    public void onRefresh() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("ChatActivity call this function: getChatRecord");
                    System.out.println("ChatActivity time: " + time);
                    getRecordSize = chatViewModel.getChatRecord(userID, friendID, userShortToken, socket, time);
                    getRecordFlag = true;
                    System.out.println("ChatActivity getRecordSize: " + getRecordSize);
                    System.out.println("ChatActivity getRecordFlag: " + getRecordFlag);
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
        swipeRefreshLayout.setRefreshing(false);
    }
}