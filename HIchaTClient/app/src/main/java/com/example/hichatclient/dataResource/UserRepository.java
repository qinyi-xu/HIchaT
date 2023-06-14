package com.example.hichatclient.dataResource;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.hichatclient.Test;
import com.example.hichatclient.data.ChatDatabase;
import com.example.hichatclient.data.dao.ChattingContentDao;
import com.example.hichatclient.data.dao.ChattingFriendDao;
import com.example.hichatclient.data.dao.FriendDao;
import com.example.hichatclient.data.dao.MeToOthersDao;
import com.example.hichatclient.data.dao.OthersToMeDao;
import com.example.hichatclient.data.entity.ChattingContent;
import com.example.hichatclient.data.entity.ChattingFriend;
import com.example.hichatclient.data.entity.Friend;
import com.example.hichatclient.data.entity.MeToOthers;
import com.example.hichatclient.data.entity.OthersToMe;
import com.example.hichatclient.data.entity.User;
import com.example.hichatclient.data.dao.UserDao;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {
    private UserDao userDao;
    private FriendDao friendDao;
    private MeToOthersDao meToOthersDao;
    private OthersToMeDao othersToMeDao;
    private ChattingContentDao chattingContentDao;
    private ChattingFriendDao chattingFriendDao;

    public UserRepository(Context context) {
        ChatDatabase chatDatabase = ChatDatabase.getDatabase(context.getApplicationContext());
        userDao = chatDatabase.getUserDao();
        friendDao = chatDatabase.getFriendDao();
        meToOthersDao = chatDatabase.getMeToOthersDao();
        othersToMeDao = chatDatabase.getOthersToMeDao();
        chattingContentDao = chatDatabase.getChattingContentDao();
        chattingFriendDao = chatDatabase.getChattingFriendDao();
    }

    public static final int PACKET_HEAD_LENGTH = 4;//从服务器接收的数据包头长度
    //处理粘包、半包问题使用的数组合并函数
    public static byte[] mergebyte(byte[] a, byte[] b, int begin, int end) {
        byte[] add = new byte[a.length + end - begin];
        int i = 0;
        for (i = 0; i < a.length; i++) {
            add[i] = a[i];
        }
        for (int k = begin; k < end; k++, i++) {
            add[i] = b[k];
        }
        return add;
    }



    public void insertLogInMsg(List<Friend> friends, List<MeToOthers> meToOthers, List<OthersToMe> othersToMes, List<ChattingContent> chattingContents, List<String> deleteFriends, String userID){
        if (!friends.isEmpty()){
            new insertFriendsTread(friendDao, friends).start();
        }
        if (!meToOthers.isEmpty()){
            new updateMeToOtherThread(meToOthersDao, meToOthers).start();
        }
        if (!othersToMes.isEmpty()){
            new updateOthersToMeThread(othersToMeDao, othersToMes).start();
        }
        if (!chattingContents.isEmpty()){
            new insertMessageThread(chattingContentDao, chattingContents).start();
        }
        if (!deleteFriends.isEmpty()){
            for(int i = 0; i < deleteFriends.size(); i++){
                String deleteMeId = deleteFriends.get(i);
                System.out.println("ChatService deleteMeId: " + deleteMeId);
//            deleteMe.add(deleteMeId);
                deleteFriendInSQL(userID, deleteMeId);
                ChattingFriend chattingFriend1 = new ChattingFriend(userID, deleteMeId, "Unknown", null, "You are deleted!", 0);
                updateChattingFriendIntoSQL(chattingFriend1);

            }
        }
    }

    // 登录（用于本地测试）
//    public User sendIDAndLogInTest(String userID, String userPassword){
//        int isLogIn = 1;
//        if (isLogIn == 1){
////            User user = new User(userID, userPassword, "jane", "111", "123", "123");
//            return user;
//        }else {
//            return null;
//        }
//    }
    // 登录
    public Map<Integer,User> sendIDAndLogIn(String userID, String userPassword, Socket oldSocket) throws InterruptedException {
        Map<Integer, User> map = new HashMap<>();
        SendIDAndLogInThread sendIDAndLogInThread = new SendIDAndLogInThread(userID, userPassword, oldSocket);
        System.out.println("UserRepository userID: " + userID);
        System.out.println("UserRepository userPassword: " + userPassword);

        sendIDAndLogInThread.start();
        sendIDAndLogInThread.join();
//        System.out.println("UserRepository userShortToken: " + sendIDAndLogInThread.user.getUserShortToken());
        map.put(sendIDAndLogInThread.isLogIn, sendIDAndLogInThread.user);
        insertLogInMsg(sendIDAndLogInThread.friends, sendIDAndLogInThread.meToOthers, sendIDAndLogInThread.othersToMes, sendIDAndLogInThread.chattingContents, sendIDAndLogInThread.deleteFriends, sendIDAndLogInThread.userID);
        return map;
    }
    static class SendIDAndLogInThread extends Thread {
        private String userID;
        private String userPassword;
        private List<OthersToMe> othersToMes;
        private List<MeToOthers> meToOthers;
        private List<ChattingContent> chattingContents;
        private List<String> deleteFriends;
        private List<Friend> friends;


        private User user;  // 0: user=null; 1: user=user信息; 2: user=null
        private Integer isLogIn;  // 0: 登录失败；1: 登录成功且获取了全部信息；2: 登录成功但是没有获取FriendList


        public SendIDAndLogInThread(String userID, String userPassword, Socket oldSocket){
            this.userID = userID;
            this.userPassword = userPassword;
        }

        @Override
        public void run() {
            super.run();
            int isLogIn = 0;
            String userName = null;
            byte[] userProfile = null;
//        Bitmap userHeadPic = null;
            String shortToken = null;
            String longToken = null;
            int ip = 0;
            int port = 0;


            List<OthersToMe> othersToMes = new ArrayList<> ();
            List<MeToOthers> meToOthers = new ArrayList<>();
            List<ChattingContent> chattingContents = new ArrayList<>();
            List<String> deleteFriends = new ArrayList<>();
            List<String> seenFriendID = new ArrayList<>();
            List <String> seenTime = new ArrayList<>();
            List<Friend> friends = new ArrayList<>();

            Socket socket = null;
            try {
                socket = new Socket("49.234.105.69", 20001);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(socket.isConnected());
            // **********发送"用户名和密码"***********cc
            Test.Login.Req.Builder loginRequest = Test.Login.Req.newBuilder();
            loginRequest.setId(Integer.parseInt(userID));
            // 求id加password的Md5
            String idPassword = userID+userPassword;
//            loginRequest.setPassword(userPassword);
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            md.update(idPassword.getBytes());
            byte[] digest = md.digest();
            BigInteger bi = new BigInteger(1, digest);
            String hashText = bi.toString(16);
            while(hashText.length() < 32){
                hashText = "0" + hashText;
            }
            loginRequest.setPassword(hashText);
            System.out.println("UserRepository hashText: " + hashText);
            port = socket.getLocalPort();
            loginRequest.setInPort(port);
            ip = 1234567;
//        ip = Integer.parseInt(socket.getLocalAddress().getHostAddress().toString());
            loginRequest.setInIp(ip);
            Test.ReqToServer.Builder reqToServer = Test.ReqToServer.newBuilder();
            reqToServer.setLoginReq(loginRequest);
            byte[] request = reqToServer.build().toByteArray();
            byte[] len = new byte[4];
            for (int i = 0;  i < 4;  i++)
            {
                len[3-i] = (byte)((request.length >> (8 * i)) & 0xFF);
            }
            byte[] send_data = new byte[request.length + len.length];
            System.arraycopy(len, 0, send_data, 0, len.length);
            System.arraycopy(request, 0, send_data, len.length, request.length);

            OutputStream outputStream = null;
            try {
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outputStream.write(send_data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // **********接收"是否登录成功"***********
            int errorFlag = 0;
            byte[] bytes = new byte[0];
            while(socket.isConnected()){
                InputStream is = null;
                try {
                    socket.setSoTimeout(1000);//  0.5秒就退出read()方法的阻塞
                    is = socket.getInputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (bytes.length < PACKET_HEAD_LENGTH) {
                    byte[] head = new byte[PACKET_HEAD_LENGTH - bytes.length];
                    int couter = 0;
//                    System.out.println(bytes.length);
                    try {
                        couter = is.read(head);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
//                    System.out.println(PACKET_HEAD_LENGTH);
                    if (couter <= 0) {
                        errorFlag++;
                        break;//continue;
                    }
                    bytes = mergebyte(bytes, head, 0, couter);
                    if (couter < PACKET_HEAD_LENGTH) {
                        continue;
                    }
                }
                // 下面这个值请注意，一定要取4长度的字节子数组作为报文长度
                byte[] temp = new byte[0];
                temp = mergebyte(temp, bytes, 0, PACKET_HEAD_LENGTH);
                int bodylength = 0; //包体长度
                for(int i=0;i<temp.length;i++){
                    bodylength += (temp[i] & 0xff) << ((3-i)*8);
                }
                if (bytes.length - PACKET_HEAD_LENGTH < bodylength) {//不够一个包
                    byte[] body;
                    if((bodylength + PACKET_HEAD_LENGTH - bytes.length) > 2530686){
                        body = new byte[2530686];
                    }
                    else{
                        body = new byte[bodylength + PACKET_HEAD_LENGTH - bytes.length];//剩下应该读的字节(凑一个包)
                    }
                    int couter = 0;
                    try {
                        couter = is.read(body,0,body.length);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (couter <= 0) {
                        errorFlag++;
                        break;//continue;
                    }
                    bytes = mergebyte(bytes, body, 0, couter);
                    continue;
//                    if (couter < bodylength + PACKET_HEAD_LENGTH - bytes.length) {
//                        continue;
//                    }
                }
                byte[] body = new byte[0];
                body = mergebyte(body, bytes, PACKET_HEAD_LENGTH, bytes.length);
                bytes = new byte[0];
                Test.RspToClient response = null;
                try {
                    response = Test.RspToClient.parseFrom(body);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                Test.RspToClient.RspCase type = response.getRspCase();
                int num;  //接收列表计数
                System.out.println("LogInFragment type: " + type);
                switch (type){
                    case LOGIN_RES:
                        isLogIn = 2;
                        errorFlag++;
                        userName = response.getLoginRes().getName();
                        userProfile = response.getLoginRes().getHeadpic().toByteArray();
                        if (userProfile.length < 10){
                            userProfile =  null;
                        }
//                picBytes = response.getLoginRes().getHeadpic().toByteArray();
//                if (picBytes.length != 0) {
//                    userHeadPic = BitmapFactory.decodeByteArray(picBytes, 0, b.length);
//                } else {
//                    return null;
//                }
                        shortToken = response.getLoginRes().getShortToken();
                        System.out.println("UserRepository userShortToken into: " + shortToken);
                        longToken = response.getLoginRes().getLongToken();
                        break;
                    case ADD_FRIEND_FROM_OTHER_RSP:
                        num = response.getAddFriendFromOtherRsp().getUserCount();
                        for(int i = 0;i < num; i++){
                            Test.People reqOtheri = response.getAddFriendFromOtherRsp().getUser(i);
                            OthersToMe othersToMe = new OthersToMe(userID,Integer.toString(reqOtheri.getId()),reqOtheri.getName(),reqOtheri.getHeadpic().toByteArray(),"wait");
                            if (reqOtheri.getHeadpic().toByteArray().length < 10){
                                othersToMe.setObjectProfile(null);
                            }
                            othersToMes.add(othersToMe);
                        }
                        break;
                    case ADD_FRIEND_FROM_SELF_RSP:
//                        System.out.println("add_friend_from_rsp");
                        num = response.getAddFriendFromSelfRsp().getRequestsCount();
                        for(int i = 0; i < num; i++){
                            Test.AddFriendFromSelf.Rsp.RequestFromSelf reqMei = response.getAddFriendFromSelfRsp().getRequests(i);
                            System.out.println("UserRepository addFriendFromSelfRsp: " + reqMei.getStatus().toString());
                            byte[] status = reqMei.getStatus().toByteArray();
                            int a = 0;
                            for(int j = status.length-1;j>=0;j--){
                                a+=status[j] * Math.pow(0xFF, status.length - j - 1);
                            }
                            if(a == 0){
                                MeToOthers meToOther = new MeToOthers(userID,Integer.toString(reqMei.getObjUser().getId()),reqMei.getObjUser().getName(),reqMei.getObjUser().getHeadpic().toByteArray(),"wait");
                                if (reqMei.getObjUser().getHeadpic().toByteArray().length  < 10){
                                    meToOther.setObjectProfile(null);
                                }
                                meToOthers.add(meToOther);
                            }
                            else if(a == 1){
                                MeToOthers meToOther = new MeToOthers(userID,Integer.toString(reqMei.getObjUser().getId()),reqMei.getObjUser().getName(),reqMei.getObjUser().getHeadpic().toByteArray(),"agree");
                                if (reqMei.getObjUser().getHeadpic().toByteArray().length  < 10){
                                    meToOther.setObjectProfile(null);
                                }
                                meToOthers.add(meToOther);
                            }
                            else if(a == 2){
                                MeToOthers meToOther = new MeToOthers(userID,Integer.toString(reqMei.getObjUser().getId()),reqMei.getObjUser().getName(),reqMei.getObjUser().getHeadpic().toByteArray(),"refuse");
                                if (reqMei.getObjUser().getHeadpic().toByteArray().length  < 10){
                                    meToOther.setObjectProfile(null);
                                }
                                meToOthers.add(meToOther);
                            }

                        }
                        break;
                    case UNRECEIVED_MSG_RES:
//                        System.out.println("LogInFragment unreceived_msg_res");
                        num = response.getUnreceivedMsgRes().getMsgCount();
                        for(int i = 0; i < num; i++){
                            Test.UnreceivedMsg.Res.Msg unreceivedMsgi = response.getUnreceivedMsgRes().getMsg(i);
                            ChattingContent chattingContent = new ChattingContent(userID,Integer.toString(unreceivedMsgi.getOtherId()),"receive",unreceivedMsgi.getTime(),unreceivedMsgi.getContent(), false, null);
                            System.out.println("LogInFragment content: " + chattingContent.getMsgContent());
                            chattingContents.add(chattingContent);
                        }
                        break;
                    case SEEN_SERVER_TO_B:
                        num = response.getSeenServerToB().getSeenInfoCount();
                        for(int i = 0; i < num; i++){
                            Test.Seen.ServerToB.SeenInfo seenInfoi = response.getSeenServerToB().getSeenInfo(i);
                            seenFriendID.add(Integer.toString(seenInfoi.getSrcId()));
                            seenTime.add(Long.toString(seenInfoi.getTime()));
                        }
                        break;
                    case DELETE_FRIEND_SERVER_TO_B:
//                        System.out.println("LogInFragment delete_friend_server_to_b");
                        num = response.getDeleteFriendServerToB().getSrcIdCount();
                        for(int i = 0; i < num; i++){
                            deleteFriends.add(Integer.toString(response.getDeleteFriendServerToB().getSrcId(i)));
                        }
                        break;
                    case FRIENDLIST_RES:
                        isLogIn = 1;
                        num = response.getFriendlistRes().getFriendListCount();
                        for(int i = 0; i < num; i++)
                        {
                            Test.People friendi = response.getFriendlistRes().getFriendList(i);
                            Friend friend = new Friend(userID, Integer.toString(friendi.getId()), friendi.getName(), friendi.getHeadpic().toByteArray(), "123", "123");
                            if (friendi.getHeadpic().toByteArray().length < 10){
                                friend.setFriendProfile(null);
                            }
                            friends.add(friend);
                        }
                        break;
                    case ERROR:
                        System.out.println(response.getError().getErrorType());
                        if (response.getError().getErrorType() == Test.Error.Error_type.ID_OR_PSW_WRONG) {
                            isLogIn = 0;
                        } else if(response.getError().getErrorType() == Test.Error.Error_type.UNKNOWN_LOGIN_ERR){
                            isLogIn = 3;
                        } else {
                            isLogIn = 2;
                        }
                        System.out.println("Fail!!!!");
                        break;
                }
                if(type == Test.RspToClient.RspCase.FRIENDLIST_RES){
                    isLogIn = 1;
                    errorFlag = 0;
                    break;
                }
            }

            if(errorFlag == 1 && isLogIn != 3){
                isLogIn = 0;
            }

            if(errorFlag == 2){
                isLogIn = 2;
            }

            if (isLogIn == 1 || isLogIn == 2){
                User user = new User(userID, userPassword, userName, userProfile, shortToken, longToken);
//                User user = new User(userID, userPassword, "123", "111", "123", "123");
                this.user = user;
            } else {
                this.user = null;
            }

            this.isLogIn = isLogIn;
            this.othersToMes = othersToMes;
            this.meToOthers = meToOthers;
            this.chattingContents = chattingContents;
            this.deleteFriends = deleteFriends;
            this.friends = friends;

        }
    }



    static class insertFriendsTread extends Thread {
        FriendDao friendDao;
        List<Friend> friends;

        public insertFriendsTread(FriendDao friendDao, List<Friend> friends){
            this.friendDao = friendDao;
            this.friends = friends;
        }

        @Override
        public void run() {
            super.run();
            friendDao.insertAllFriend(friends);
        }
    }

    static class updateMeToOtherThread extends Thread{
        MeToOthersDao meToOthersDao;
        List<MeToOthers> meToOthers;

        public updateMeToOtherThread(MeToOthersDao meToOthersDao, List<MeToOthers> meToOthers){
            this.meToOthersDao = meToOthersDao;
            this.meToOthers = meToOthers;
        }

        @Override
        public void run() {
            super.run();
            meToOthersDao.insertAllMeToOthers(meToOthers);
        }
    }

    static class updateOthersToMeThread extends Thread{
        OthersToMeDao othersToMeDao;
        List<OthersToMe> othersToMes;

        public updateOthersToMeThread(OthersToMeDao othersToMeDao, List<OthersToMe> othersToMes){
            this.othersToMeDao = othersToMeDao;
            this.othersToMes = othersToMes;
        }

        @Override
        public void run() {
            super.run();
            othersToMeDao.insertAllOthersToMe(othersToMes);
        }
    }

    static class insertMessageThread extends Thread{
        ChattingContentDao chattingContentDao;
        List<ChattingContent> chattingContent;

        public insertMessageThread(ChattingContentDao chattingContentDao, List<ChattingContent> chattingContent) {
            this.chattingContentDao = chattingContentDao;
            this.chattingContent = chattingContent;
        }

        @Override
        public void run() {
            super.run();
            chattingContentDao.insertAllContent(chattingContent);
        }
    }


    // 注册
    public String signUp(String userName, String userPassword, Bitmap bitmapImage, Socket oldSocket) throws InterruptedException {
        SignUpThread signUpThread = new SignUpThread(userName, userPassword, bitmapImage, oldSocket);
//        System.out.println("repository");
        signUpThread.start();
        signUpThread.join();
        return signUpThread.userID;
    }
    static class SignUpThread extends Thread {
        private String userName;
        private String userPassword;
        private String userID;
        private Bitmap bitmapImage;


        public SignUpThread(String userName, String userPassword, Bitmap bitmapImage, Socket oldSocket){
            this.userName = userName;
            this.userPassword = userPassword;
            this.bitmapImage = bitmapImage;
        }



        @Override
        public void run() {
            super.run();
            System.out.println("Thread-run");
            Socket socket = null;
            try {
                socket = new Socket("49.234.105.69", 20001);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String userID = null;
            System.out.println(socket.isConnected());
            // **********发送"昵称和密码"***********
            Test.Register.Req.Builder registerRequest = Test.Register.Req.newBuilder();
            registerRequest.setName(userName);
            registerRequest.setPassword(userPassword);
            if(bitmapImage != null){
                ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
                bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, imageBytes);
                registerRequest.setHeadpic(ByteString.copyFrom(imageBytes.toByteArray()));
            }
            else{
                String headpic = "null";
                registerRequest.setHeadpic(ByteString.copyFrom(headpic.getBytes()));
            }

            Test.ReqToServer.Builder reqToServer = Test.ReqToServer.newBuilder();
            reqToServer.setRegisterReq(registerRequest);
            byte[] request = reqToServer.build().toByteArray();
            byte[] len = new byte[4];
            for (int i = 0;  i < 4;  i++)
            {
                len[3-i] = (byte)((request.length >> (8 * i)) & 0xFF);
            }
            byte[] send_data = new byte[request.length + len.length];
            System.arraycopy(len, 0, send_data, 0, len.length);
            System.arraycopy(request, 0, send_data, len.length, request.length);
            OutputStream outputStream = null;
            try {
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outputStream.write(send_data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // **********接收"是否连接成功和用户ID"***********
            byte[] bytes = new byte[0];
            while(socket.isConnected())
            {
                InputStream is = null;
                try {
                    is = socket.getInputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (bytes.length < PACKET_HEAD_LENGTH) {
                    byte[] head = new byte[PACKET_HEAD_LENGTH - bytes.length];
                    int couter = 0;
                    try {
                        couter = is.read(head);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (couter < 0) {
                        continue;
                    }
                    bytes = mergebyte(bytes, head, 0, couter);
                    if (couter < PACKET_HEAD_LENGTH) {
                        continue;
                    }
                }
                // 下面这个值请注意，一定要取4长度的字节子数组作为报文长度
                byte[] temp = new byte[0];
                temp = mergebyte(temp, bytes, 0, PACKET_HEAD_LENGTH);
                int bodylength = 0; //包体长度
                for(int i=0;i<temp.length;i++){
                    bodylength += (temp[i] & 0xff) << ((3-i)*8);
                }
                if (bytes.length - PACKET_HEAD_LENGTH < bodylength) {//不够一个包
                    byte[] body;
                    if((bodylength + PACKET_HEAD_LENGTH - bytes.length) > 2530686){
                        body = new byte[2530686];
                    }
                    else{
                        body = new byte[bodylength + PACKET_HEAD_LENGTH - bytes.length];//剩下应该读的字节(凑一个包)
                    }
                    int couter = 0;
                    try {
                        couter = is.read(body);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (couter < 0) {
                        continue;
                    }
                    bytes = mergebyte(bytes, body, 0, couter);
                    if (couter < bodylength + PACKET_HEAD_LENGTH) {
                        continue;
                    }
                }
                byte[] body = new byte[0];
                body = mergebyte(body, bytes, PACKET_HEAD_LENGTH, bytes.length);
                bytes = new byte[0];
                Test.RspToClient response = null;
                try {
                    response = Test.RspToClient.parseFrom(body);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                Test.RspToClient.RspCase type = response.getRspCase();
                switch (type){
                    case REGISTER_RES:
                        userID = Integer.toString(response.getRegisterRes().getId());
                        break;
                    case ERROR:
                        System.out.println("Fail!!!!");
                        break;
                }
                break;
            }
            System.out.println(userID);
            this.userID = userID;
        }
    }



    // 向数据库添加登录成功的用户信息
    public void insertUser(User user) throws InterruptedException {
        new InsertUserThread(userDao, user).start();
    }

    static class InsertUserThread extends Thread {
        UserDao userDao;
        User user;

        public InsertUserThread(UserDao userDao, User user) {
            this.userDao = userDao;
            this.user = user;
        }

        @Override
        public void run() {
            super.run();
            userDao.insertUser(user);
        }
    }


    // 向服务器发送更新用户头像
    public int updateUserProfileToServer(String userShortToken, byte[] userNewProfile,Socket oldSocket) throws IOException {
        int flag = 1;
        Socket socket = new Socket("49.234.105.69", 20001);
        Test.ChangeHeadpic.Req.Builder changeHeadpicReq = Test.ChangeHeadpic.Req.newBuilder();
        changeHeadpicReq.setShortToken(userShortToken);
        changeHeadpicReq.setNewHeadpic(ByteString.copyFrom(userNewProfile));
        Test.ReqToServer.Builder reqToServer = Test.ReqToServer.newBuilder();
        reqToServer.setChangeHeadpicReq(changeHeadpicReq);
        byte[] request = reqToServer.build().toByteArray();
        byte[] len = new byte[4];
        for (int i = 0;  i < 4;  i++)
        {
            len[3-i] = (byte)((request.length >> (8 * i)) & 0xFF);
        }
        byte[] send_data = new byte[request.length + len.length];
        System.arraycopy(len, 0, send_data, 0, len.length);
        System.arraycopy(request, 0, send_data, len.length, request.length);

        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(send_data);
        outputStream.flush();

        byte[] bytes = new byte[0];
        while(socket.isConnected()){
            InputStream is = socket.getInputStream();
            if (bytes.length < PACKET_HEAD_LENGTH) {
                byte[] head = new byte[PACKET_HEAD_LENGTH - bytes.length];
                int couter = is.read(head);
                if (couter < 0) {
                    continue;
                }
                bytes = mergebyte(bytes, head, 0, couter);
                if (couter < PACKET_HEAD_LENGTH) {
                    continue;
                }
            }
            // 下面这个值请注意，一定要取4长度的字节子数组作为报文长度
            byte[] temp = new byte[0];
            temp = mergebyte(temp, bytes, 0, PACKET_HEAD_LENGTH);
            int bodylength = 0; //包体长度
            for(int i=0;i<temp.length;i++){
                bodylength += (temp[i] & 0xff) << ((3-i)*8);
            }
            if (bytes.length - PACKET_HEAD_LENGTH < bodylength) {//不够一个包
                byte[] body = new byte[bodylength + PACKET_HEAD_LENGTH - bytes.length];//剩下应该读的字节(凑一个包)
                int couter = is.read(body);
                if (couter < 0) {
                    continue;
                }
                bytes = mergebyte(bytes, body, 0, couter);
                if (couter < bodylength + PACKET_HEAD_LENGTH) {
                    continue;
                }
            }
            byte[] body = new byte[0];
            body = mergebyte(body, bytes, PACKET_HEAD_LENGTH, bytes.length);
            bytes = new byte[0];
            Test.RspToClient response = Test.RspToClient.parseFrom(body);
            Test.RspToClient.RspCase type = response.getRspCase();
            switch (type){
                case CHANGE_HEADPIC_RSP:
                    flag = 1;
                    break;
                case ERROR:
                    flag = 0;
                    break;
            }
            break;
        }
        return flag;
    }

    // 删掉数据库中的某好友
    public void deleteFriendInSQL(String userID, String friendID){
        new FriendsRepository.deleteFriendInSQLThread(friendDao, userID, friendID).start();
    }

    static class deleteFriendInSQLThread extends Thread{
        FriendDao friendDao;
        String userID;
        String friendID;


        public deleteFriendInSQLThread(FriendDao friendDao, String userID, String friendID) {
            this.friendDao = friendDao;
            this.userID = userID;
            this.friendID = friendID;
        }

        @Override
        public void run() {
            super.run();
            friendDao.deleteOneFriend(userID, friendID);
        }
    }

    // 更新数据库中的聊天框
    public void updateChattingFriendIntoSQL(ChattingFriend chattingFriend){
        new MessageRepository.updateChattingFriendIntoSQLThread(chattingFriendDao, chattingFriend).start();
    }
    static class updateChattingFriendIntoSQLThread extends Thread{
        ChattingFriendDao chattingFriendDao;
        ChattingFriend chattingFriend;

        public updateChattingFriendIntoSQLThread(ChattingFriendDao chattingFriendDao, ChattingFriend chattingFriend) {
            this.chattingFriendDao = chattingFriendDao;
            this.chattingFriend = chattingFriend;
        }

        @Override
        public void run() {
            super.run();
            chattingFriendDao.insertChattingFriend(chattingFriend);
        }
    }

}
