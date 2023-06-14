package com.example.hichatclient.dataResource;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.hichatclient.Test;
import com.example.hichatclient.data.ChatDatabase;
import com.example.hichatclient.data.entity.Friend;
import com.example.hichatclient.data.dao.FriendDao;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class FriendsRepository {
    private FriendDao friendDao;

    public FriendsRepository(Context context) {
        ChatDatabase chatDatabase = ChatDatabase.getDatabase(context.getApplicationContext());
        friendDao = chatDatabase.getFriendDao();
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

    // 向服务器发送删好友请求
    public void deleteFriendToServer(String friendID, String userShortToken, Socket oldSocket) throws IOException {
        Socket socket = new Socket("49.234.105.69", 20001);
        Test.DeleteFriend.AToServer.Builder deleteFriendAToServer = Test.DeleteFriend.AToServer.newBuilder();
        deleteFriendAToServer.setObjId(Integer.parseInt(friendID));
        deleteFriendAToServer.setShortToken(userShortToken);
        Test.ReqToServer.Builder reqToServer = Test.ReqToServer.newBuilder();
        reqToServer.setDeleteFriendAToServer(deleteFriendAToServer);
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
    }



    // 从服务器获取好友列表，在Base Activity中执行
    public List<Friend> getUserFriendsFromServer(String userID, String userShortToken, Socket oldSocket) throws IOException {
        List<Friend> friends = new ArrayList<>();
        Socket socket = new Socket("49.234.105.69", 20001);
        System.out.println(socket.isConnected());
        //发送好友列表请求
        Test.FriendList.Req.Builder friendListReq = Test.FriendList.Req.newBuilder();
        friendListReq.setToken(userShortToken);
        Test.ReqToServer.Builder reqToServer = Test.ReqToServer.newBuilder();
        reqToServer.setFriendlistReq(friendListReq);
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
        //接收好友列表
        while(socket.isConnected()){
            InputStream is = socket.getInputStream();
            byte[] bytes = new byte[0];
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
            Test.RspToClient response = Test.RspToClient.parseFrom(body);
            Test.RspToClient.RspCase type = response.getRspCase();
            System.out.println("type" + type);
            switch (type) {
                case FRIENDLIST_RES:
                    int num = response.getFriendlistRes().getFriendListCount();
                    System.out.println("num" + num);
                    for(int i = 0; i < num; i++)
                    {
                        System.out.println("num" + num);
                        Test.People friendi = response.getFriendlistRes().getFriendList(i);
                        Friend friend = new Friend(userID, Integer.toString(friendi.getId()), friendi.getName(), friendi.getHeadpic().toByteArray(), "123", "123");
                        friends.add(friend);
                    }
                    break;
                case ERROR:
                    System.out.println("Fail!!!!");
                    break;
            }
            break;
        }
        System.out.println(friends.size());
        insertFriends(friends); // 将用户的好友信息插入数据库
        return friends;
    }


    // 将从服务器获取的好友列表存入到数据库中
    public void insertFriends (List<Friend> friends){
        new insertFriendsTread(friendDao, friends).start();
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
            for (int j=0; j<friends.size(); j++){
                friendDao.insertFriend(friends.get(j));
            }
        }
    }


    // 从数据库中获取好友列表，在SearchFriendActivity中执行
    public List<Friend> getUserFriendsInfoFromSQL(String userID) throws InterruptedException {
        GetUserFriendsInfoFromSQLThread thread = new GetUserFriendsInfoFromSQLThread(friendDao, userID);
        thread.start();
        thread.join();
        return thread.friends;
    }

    static class GetUserFriendsInfoFromSQLThread extends Thread {
        FriendDao friendDao;
        String userID;
        List<Friend> friends;

        public GetUserFriendsInfoFromSQLThread(FriendDao friendDao, String userID){
            this.friendDao = friendDao;
            this.userID = userID;
        }

        @Override
        public void run() {
            super.run();
            friends = friendDao.getUserFriendsInfoFromSQL(userID);
        }
    }




    // 从数据库中获取好友列表，在ContactsFragments中执行
    public LiveData<List<Friend>> getUserFriendsFromSQL (String userID){
        // 当返回值是LiveData的时候，系统自动放在副线程执行，不用另外写副线程类
        LiveData<List<Friend>> friends;
        friends = friendDao.getAllUserFriend(userID);
        return friends;
    }

    // 根据userID和friendID从数据库中获取好友信息
    public LiveData<Friend> getFriendInfo(String userID, String friendID){
        return friendDao.getFriendInfo(userID, friendID);
    }

    // 从数据库中搜索好友
    public LiveData<List<Friend>> findFriendsWithPatten(String patten){
        return friendDao.findFriendsWithPatten( " %" + patten + "%");  // 加上通配符，模糊匹配
    }


    // 把新的好友添加到数据库中
    public void insertNewFriendIntoSQL(Friend friend){
        new insertNewFriendIntoSQLThread(friendDao, friend).start();
    }

    static class insertNewFriendIntoSQLThread extends Thread {
        FriendDao friendDao;
        Friend friend;

        public insertNewFriendIntoSQLThread(FriendDao friendDao, Friend friend) {
            this.friendDao = friendDao;
            this.friend = friend;
        }

        @Override
        public void run() {
            super.run();
            friendDao.insertFriend(friend);
        }
    }

    // 删掉数据库中的某好友
    public void deleteFriendInSQL(String userID, String friendID){
        new deleteFriendInSQLThread(friendDao, userID, friendID).start();
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


}
