package com.example.hichatclient.dataResource;

import android.content.Context;
import android.widget.TextView;

import androidx.lifecycle.LiveData;

import com.example.hichatclient.Test;
import com.example.hichatclient.data.ChatDatabase;
import com.example.hichatclient.data.dao.UserDao;
import com.example.hichatclient.data.entity.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

public class MeRepository {
    private UserDao userDao;

    public MeRepository(Context context) {
        ChatDatabase chatDatabase = ChatDatabase.getDatabase(context.getApplicationContext());
        userDao = chatDatabase.getUserDao();
    }

    public LiveData<User> getUserInfo(String userID){
        return userDao.getUserInfo(userID);
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

    // 向服务器发送更新用户昵称的请求
    public int updateUserNameToServer(String userShortToken, String userNewName,Socket oldSocket) throws IOException {
        int flag = 1;
        Socket socket = new Socket("49.234.105.69", 20001);
        Test.ChangeName.Req.Builder changeNameReq = Test.ChangeName.Req.newBuilder();
        changeNameReq.setShortToken(userShortToken);
        changeNameReq.setNewName(userNewName);
        Test.ReqToServer.Builder reqToServer = Test.ReqToServer.newBuilder();
        reqToServer.setChangeNameReq(changeNameReq);
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
                case CHANGE_NAME_RSP:
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



    // 向服务器发送更新用户密码的请求
    public int updateUserPasswordToServer(String userShortToken, String userNewPassword,Socket oldSocket) throws IOException {
        int flag = 1;
        Socket socket = new Socket("49.234.105.69", 20001);
        Test.ChangePassword.Req.Builder changePasswordReq = Test.ChangePassword.Req.newBuilder();
        changePasswordReq.setShortToken(userShortToken);
        changePasswordReq.setNewPassword(userNewPassword);
        Test.ReqToServer.Builder reqToServer = Test.ReqToServer.newBuilder();
        reqToServer.setChangePasswordReq(changePasswordReq);
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
                case CHANGE_PASSWORD_RSP:
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


    // 更新数据库中的用户信息
    public void updateUserInfoInSQL(User user){
        new UpdateUserInfoSQLThread(userDao, user).start();
    }

    static class UpdateUserInfoSQLThread extends Thread {
        UserDao userDao;
        User user;

        public UpdateUserInfoSQLThread(UserDao userDao, User user){
            this.userDao = userDao;
            this.user = user;
        }

        @Override
        public void run() {
            super.run();
            userDao.updateUser(user);
        }
    }

    //通过用户ID获取数据库中的的用户信息
    public LiveData<User> getLiveUserInfoByUserID(String userID){
        return userDao.getLiveUserByUserID(userID);
    }

    //通过用户ID获取数据库中的的用户信息
    public User getUserInfoByUserID(String userID) throws InterruptedException {
        GetUserInfoByUserIDThread getUserInfoByUserIDThread = new GetUserInfoByUserIDThread(userDao, userID);
        getUserInfoByUserIDThread.start();
        getUserInfoByUserIDThread.join();
        return getUserInfoByUserIDThread.user;
    }

    static class GetUserInfoByUserIDThread extends Thread {
        UserDao userDao;
        String userID;
        User user;
        public GetUserInfoByUserIDThread(UserDao userDao, String userID){
            this.userDao = userDao;
            this.userID = userID;
        }

        @Override
        public void run() {
            super.run();
            user = userDao.getUserByUserID(userID);
        }
    }


}
