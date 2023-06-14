package com.example.hichatclient.dataResource;

import android.content.Context;
import android.icu.text.SimpleDateFormat;

import androidx.lifecycle.LiveData;

import com.example.hichatclient.Test;
import com.example.hichatclient.data.ChatDatabase;
import com.example.hichatclient.data.dao.ChattingContentDao;
import com.example.hichatclient.data.dao.ChattingFriendDao;
import com.example.hichatclient.data.dao.FriendDao;
import com.example.hichatclient.data.entity.ChattingContent;
import com.example.hichatclient.data.entity.ChattingFriend;
import com.example.hichatclient.data.entity.Friend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private ChattingContentDao chattingContentDao;
    private ChattingFriendDao chattingFriendDao;


    public MessageRepository(Context context) {
        ChatDatabase chatDatabase = ChatDatabase.getDatabase(context.getApplicationContext());
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

    // 向服务器请求该用户与某个好友历史记录
    public int getChatRecord (String userID, String friendID, String userShortToken, Socket oldSocket, Long chatRecordTime) throws IOException {
        List<ChattingContent> chatRecords = new ArrayList<>();
        int flag = 0;
        Socket socket = new Socket("49.234.105.69", 20001);
        Test.ChatRecord.Req.Builder chatRecordReq = Test.ChatRecord.Req.newBuilder();
        chatRecordReq.setObjId(Integer.parseInt(friendID));
        chatRecordReq.setShortToken(userShortToken);
        chatRecordReq.setTime(chatRecordTime);
        Test.ReqToServer.Builder reqToServer = Test.ReqToServer.newBuilder();
        reqToServer.setChatRecordReq(chatRecordReq);
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
        while(socket.isConnected()) {
            InputStream is = socket.getInputStream();
            assert bytes != null;
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
            for (int i = 0; i < temp.length; i++) {
                bodylength += (temp[i] & 0xff) << ((3 - i) * 8);
            }
            if (bytes.length - PACKET_HEAD_LENGTH < bodylength) {//不够一个包
                byte[] body;
                if ((bodylength + PACKET_HEAD_LENGTH - bytes.length) > 2530686) {
                    body = new byte[2530686];
                } else {
                    body = new byte[bodylength + PACKET_HEAD_LENGTH - bytes.length];//剩下应该读的字节(凑一个包)
                }
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
            System.out.println("Message Repository type: " + type);
            switch (type) {
                case CHAT_RECORD_RSP:
                    int num = response.getChatRecordRsp().getMsgCount();

                    for(int i = 0;i < num; i++){
                        Test.ChatRecord.Rsp.Msg chattingRecord = response.getChatRecordRsp().getMsg(i);
                        if(Integer.toString(chattingRecord.getSender()).equals(friendID)){
                            ChattingContent chattingContent = new ChattingContent(userID,friendID,"receive",chattingRecord.getTime(),chattingRecord.getContent(),true,null);
                            chatRecords.add(chattingContent);
                        }
                        else{
                            ChattingContent chattingContent = new ChattingContent(userID,friendID,"send",chattingRecord.getTime(),chattingRecord.getContent(),true,null);
                            chatRecords.add(chattingContent);
                        }
                    }
                    if(num == 0){
                        flag = 0;  // 没有更早的历史记录了，返回0
                    }else{
                        flag = 1;  // 拉取成功且有历史记录，返回1
                    }
                    break;
                case ERROR:
                    flag = 2;  // 拉取失败，返回2
                    System.out.println("Fail!!!!");
                    break;
            }
            break;
        }
        if (flag == 1){ // 拉取成功且有历史记录
            System.out.println("Message Repository flag: "+flag);
            System.out.println("Message Repository chatRecords size: " + chatRecords.size());
            // 插入数据库
            new UserRepository.insertMessageThread(chattingContentDao, chatRecords).start();
        }
        return flag;
    }


    // 通过服务器发给好友消息
    public boolean sendMessageToServer(ChattingContent chattingContent, String userShortToken, Socket oldSocket) throws IOException {
        boolean flag = true;
        Socket socket = new Socket("49.234.105.69", 20001);
        System.out.println("MessageRepository userShortToken: " + userShortToken);
        System.out.println("MessageRepository content: " + chattingContent.getMsgTime() + chattingContent.getMsgType() + chattingContent.getMsgContent());
        if(socket.isConnected()){
            Test.ChatWithServer.Req.Builder chatWithServerReq = Test.ChatWithServer.Req.newBuilder();
            chatWithServerReq.setShortToken(userShortToken);
            chatWithServerReq.setObjId(Integer.parseInt(chattingContent.getFriendID()));
            chatWithServerReq.setTime(chattingContent.getMsgTime());


            chatWithServerReq.setContent(chattingContent.getMsgContent());

            Test.ReqToServer.Builder reqToServer = Test.ReqToServer.newBuilder();
            reqToServer.setChatWithServerReq(chatWithServerReq);
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
            System.out.println("MessageRepository senddatalen:" +send_data.length);
        }

        return flag;
    }

    // 给服务器发送已读消息
    public void sendReadMsgToServer(String userShortToken, String friendID, long time, Socket oldSocket) throws IOException {
        Socket socket = new Socket("49.234.105.69", 20001);
        Test.Seen.AToServer.Builder seenAToServer = Test.Seen.AToServer.newBuilder();
        seenAToServer.setShortToken(userShortToken);
        seenAToServer.setObjId(Integer.parseInt(friendID));
        seenAToServer.setTime(time);

        Test.ReqToServer.Builder reqToServer = Test.ReqToServer.newBuilder();
        reqToServer.setSeenAToServer(seenAToServer);
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

    // 从数据库中获取用户和某好友的聊天记录
    public LiveData<List<ChattingContent>> getChattingContentFromSQL(String userID, String friendID) {
        return chattingContentDao.findAllContent(userID, friendID);
    }

    public LiveData<List<ChattingContent>> getAllReceiveMsgLive(String userID, String friendID, String type) {
        return chattingContentDao.getAllReceiveMsgLive(userID, friendID, type);
    }

    // 从数据库获取用户和某好友的某个时间之前的未读的聊天记录
    public List<ChattingContent> findAllContentNotRead(String userID, String friendID, boolean isRead, long time, String type) throws InterruptedException {
        FindAllContentNotReadThread thread = new FindAllContentNotReadThread(chattingContentDao, userID, friendID, isRead, time, type);
        thread.start();
        thread.join();
        return thread.chattingContents;
    }
    static class FindAllContentNotReadThread extends Thread{
        ChattingContentDao chattingContentDao;
        String userID;
        String friendID;
        boolean isRead;
        long time;
        String type;

        List<ChattingContent> chattingContents;


        public FindAllContentNotReadThread(ChattingContentDao chattingContentDao, String userID, String friendID, boolean isRead, long time, String type) {
            this.chattingContentDao = chattingContentDao;
            this.userID = userID;
            this.friendID = friendID;
            this.isRead = isRead;
            this.time = time;
            this.type = type;
        }

        @Override
        public void run() {
            super.run();
            chattingContents = chattingContentDao.findAllContentNotRead(userID, friendID, isRead, time, type);
        }
    }


    public ChattingFriend findOneChattingFriend(String userID, String friendID) throws InterruptedException {
        FindOneChattingFriendThread thread = new FindOneChattingFriendThread(chattingFriendDao, userID, friendID);
        thread.start();
        thread.join();
        return thread.chattingFriend;
    }
    static class FindOneChattingFriendThread extends Thread{
        ChattingFriendDao chattingFriendDao;
        ChattingFriend chattingFriend;
        String userID;
        String friendID;


        public FindOneChattingFriendThread(ChattingFriendDao chattingFriendDao, String userID, String friendID) {
            this.chattingFriendDao = chattingFriendDao;
            this.userID = userID;
            this.friendID = friendID;
        }

        @Override
        public void run() {
            super.run();
            chattingFriend = chattingFriendDao.findOneChattingFriend(userID, friendID);
        }
    }


    // 从数据库获取用户正在聊天的好友列表
    public LiveData<List<ChattingFriend>> getAllChattingFriendFromSQL(String userID){
        return chattingFriendDao.findAllChattingFriend(userID);
    }

    // 更新数据库中的聊天框
    public void updateChattingFriendIntoSQL(ChattingFriend chattingFriend){
        new updateChattingFriendIntoSQLThread(chattingFriendDao, chattingFriend).start();
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


    // 往数据库更新很多条聊天信息
    public void updateAllMessageIntoSQL(List<ChattingContent> chattingContents){
        new updateAllMessageThread(chattingContentDao, chattingContents);
    }

    static class updateAllMessageThread extends Thread{
        ChattingContentDao chattingContentDao;
        List<ChattingContent> chattingContents;

        public updateAllMessageThread(ChattingContentDao chattingContentDao, List<ChattingContent> chattingContents) {
            this.chattingContentDao = chattingContentDao;
            this.chattingContents = chattingContents;
        }

        @Override
        public void run() {
            super.run();
            chattingContentDao.updateAllContent(chattingContents);
        }
    }


    // 删除数据库中聊天的好友
    public void deleteChattingFriend(ChattingFriend chattingFriend){
        new deleteChattingFriendThread(chattingFriend, chattingFriendDao).start();
    }

    static class deleteChattingFriendThread extends Thread{
        ChattingFriend chattingFriend;
        ChattingFriendDao chattingFriendDao;


        public deleteChattingFriendThread(ChattingFriend chattingFriend, ChattingFriendDao chattingFriendDao) {
            this.chattingFriend = chattingFriend;
            this.chattingFriendDao = chattingFriendDao;
        }

        @Override
        public void run() {
            super.run();
            chattingFriendDao.deleteChattingFriend(chattingFriend);
        }
    }



    // 往数据库添加一条聊天信息
    public void insertOneMessageIntoSQL(ChattingContent chattingContent){
        new insertOneMessageIntoSQLThread(chattingContentDao, chattingContent).start();
    }

    static class insertOneMessageIntoSQLThread extends Thread{
        ChattingContentDao chattingContentDao;
        ChattingContent chattingContent;

        public insertOneMessageIntoSQLThread(ChattingContentDao chattingContentDao, ChattingContent chattingContent) {
            this.chattingContentDao = chattingContentDao;
            this.chattingContent = chattingContent;
        }

        @Override
        public void run() {
            super.run();
            chattingContentDao.insertContent(chattingContent);
        }
    }

    // 往数据库更新一条聊天信息
    public void updateOneMessageIntoSQL(ChattingContent chattingContent){
        new updateOneMessageIntoSQLThread(chattingContentDao, chattingContent).start();
    }

    static class updateOneMessageIntoSQLThread extends Thread{
        ChattingContentDao chattingContentDao;
        ChattingContent chattingContent;

        public updateOneMessageIntoSQLThread(ChattingContentDao chattingContentDao, ChattingContent chattingContent) {
            this.chattingContentDao = chattingContentDao;
            this.chattingContent = chattingContent;
        }

        @Override
        public void run() {
            super.run();
            chattingContentDao.updateOneContent(chattingContent);
        }
    }

}
