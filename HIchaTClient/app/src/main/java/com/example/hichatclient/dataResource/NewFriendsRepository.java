package com.example.hichatclient.dataResource;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.hichatclient.Test;
import com.example.hichatclient.data.ChatDatabase;
import com.example.hichatclient.data.dao.MeToOthersDao;
import com.example.hichatclient.data.dao.OthersToMeDao;
import com.example.hichatclient.data.dao.UserDao;
import com.example.hichatclient.data.entity.MeToOthers;
import com.example.hichatclient.data.entity.OthersToMe;
import com.example.hichatclient.data.entity.SearchResult;
import com.example.hichatclient.data.entity.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

public class NewFriendsRepository {
    private MeToOthersDao meToOthersDao;
    private OthersToMeDao othersToMeDao;

    public NewFriendsRepository(Context context){
        ChatDatabase chatDatabase = ChatDatabase.getDatabase(context.getApplicationContext());
        meToOthersDao = chatDatabase.getMeToOthersDao();
        othersToMeDao = chatDatabase.getOthersToMeDao();
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

    // 用户发送ID，服务器返回搜索结果
    public SearchResult searchPeopleFromID (String personID, String userShortToken, Socket oldSocket) throws IOException {
        SearchResult result = null;
        Socket socket = new Socket("49.234.105.69", 20001);
        Test.SearchUser.Req.Builder searchUserReq = Test.SearchUser.Req.newBuilder();
        searchUserReq.setShortToken(userShortToken);
        searchUserReq.setObjId(Integer.parseInt(personID));
        Test.ReqToServer.Builder reqToServer = Test.ReqToServer.newBuilder();
        reqToServer.setSearchUserReq(searchUserReq);
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

        // **********接收结果***********
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
                case SEARCH_USER_RES:
                    Test.People searchUserResult = response.getSearchUserRes().getUser();
                    result = new SearchResult(Integer.toString(searchUserResult.getId()), searchUserResult.getName(), searchUserResult.getHeadpic().toByteArray());
                    if (searchUserResult.getHeadpic().toByteArray().length < 10){
                        result.setResultProfile(null);
                    }
                    break;
                case ERROR:
                    result = null;
                    System.out.println("Fail!!!!");
                    break;
            }
            break;
        }
        return result;
    }




    // 向服务器提交对别人好友请求的回应
    public void othersToMeResponseToServer(String userShortToken, String objectID, boolean refuse, Socket oldSocket) throws IOException {
        System.out.println("NewFriendRepository");
        Socket socket = new Socket("49.234.105.69", 20001);
        Test.AddFriend.BToServer.Builder othersToMeRsp = Test.AddFriend.BToServer.newBuilder();
        othersToMeRsp.setAId(Integer.parseInt(objectID));
        othersToMeRsp.setBShortToken(userShortToken);
        othersToMeRsp.setRefuse(refuse);
        Test.ReqToServer.Builder reqToServer = Test.ReqToServer.newBuilder();
        reqToServer.setAddFriendBToServer(othersToMeRsp);
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

    // 向服务器发送好友请求，1表示发送成功
    public int addFriend (String personID, String userShortToken, Socket oldSocket) throws IOException {
        Test.AddFriend.AToServer.Builder addFriendReq = Test.AddFriend.AToServer.newBuilder();
        Socket socket = new Socket("49.234.105.69", 20001);
        addFriendReq.setBId(Integer.parseInt(personID));
        addFriendReq.setAShortToken(userShortToken);
        Test.ReqToServer.Builder reqToServer = Test.ReqToServer.newBuilder();
        reqToServer.setAddFriendAToServer(addFriendReq);
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
        return 1;
    }



    // 当用户回应别人的好友请求时，更新数据库中的信息
    public void updateOthersToMeResponse(OthersToMe othersToMe){
        new updateOthersToMeResponseThread(othersToMeDao, othersToMe).start();
    }

    // 当用户发送好友请求时，更新数据库中的信息
    public void updateMeToOthersSend(MeToOthers meToOthers){
        new updateMeToOthersSendThread(meToOthersDao, meToOthers).start();
    }

    public LiveData<List<MeToOthers>> getAllMeToOthersFromSQL(String userID){
        return meToOthersDao.getAllMeToOthers(userID);
    }

    public LiveData<List<OthersToMe>> getAllOthersToMeFromSQL(String userID){
        return othersToMeDao.getAllOthersToMe(userID);
    }

    public void updateMeToOthers(List<MeToOthers> meToOthers){
        new updateMeToOtherThread(meToOthersDao, meToOthers).start();
    }

    public void updateOthersToMe(List<OthersToMe> othersToMes){
        new updateOthersToMeThread(othersToMeDao, othersToMes).start();
    }

    public OthersToMe getOthersToMeByObjectID(String userID, String objectID) throws InterruptedException {
        GetOthersToMeByObjectIDThread getOthersToMeByObjectIDThread = new GetOthersToMeByObjectIDThread(othersToMeDao, userID, objectID);
        getOthersToMeByObjectIDThread.start();
        getOthersToMeByObjectIDThread.join();
        return getOthersToMeByObjectIDThread.othersToMe;
    }

    static class updateMeToOthersSendThread extends Thread{
        MeToOthersDao meToOthersDao;
        MeToOthers meToOthers;

        public updateMeToOthersSendThread(MeToOthersDao meToOthersDao, MeToOthers meToOthers) {
            this.meToOthersDao = meToOthersDao;
            this.meToOthers = meToOthers;
        }

        @Override
        public void run() {
            super.run();
            meToOthersDao.insertMeToOthers(meToOthers);
        }
    }


    static class updateOthersToMeResponseThread extends Thread{
        OthersToMeDao othersToMeDao;
        OthersToMe othersToMe;

        public updateOthersToMeResponseThread(OthersToMeDao othersToMeDao, OthersToMe othersToMe) {
            this.othersToMeDao = othersToMeDao;
            this.othersToMe = othersToMe;
        }

        @Override
        public void run() {
            super.run();
            othersToMeDao.updateOthersToMe(othersToMe);
        }
    }


    static class GetOthersToMeByObjectIDThread extends Thread{
        OthersToMeDao othersToMeDao;
        String userID;
        String objectID;
        OthersToMe othersToMe;

        public GetOthersToMeByObjectIDThread(OthersToMeDao othersToMeDao, String userID, String objectID) {
            this.othersToMeDao = othersToMeDao;
            this.userID = userID;
            this.objectID = objectID;
        }

        @Override
        public void run() {
            super.run();
            this.othersToMe = othersToMeDao.getOthersToMeByObjectID(userID, objectID);
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


}
