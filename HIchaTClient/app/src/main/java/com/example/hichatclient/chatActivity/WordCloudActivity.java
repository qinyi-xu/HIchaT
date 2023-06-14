package com.example.hichatclient.chatActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.hichatclient.R;
import com.example.hichatclient.data.entity.ChattingContent;
import com.example.hichatclient.viewModel.ChatViewModel;
import com.example.hichatclient.viewModel.WordCloudViewModel;
import com.mordred.wordcloud.WordCloud;
import com.mordred.wordcloud.WordFrequency;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jackmego.com.jieba_android.JiebaSegmenter;

public class WordCloudActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private WordCloudViewModel wordCloudViewModel;

    private String userID;
    private String friendID;

    private Button buttonGetWordCloud;
    private ImageView imageViewWordCloud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_cloud);

        buttonGetWordCloud = findViewById(R.id.buttonGetWordCloud);
        imageViewWordCloud = findViewById(R.id.imageView8);


        // 获取Share Preferences中的数据
        sharedPreferences = getSharedPreferences("MY_DATA",Context.MODE_PRIVATE);
        userID = sharedPreferences.getString("userID", "fail");

        // 接收ChatActivity传来的参数
        friendID = getIntent().getStringExtra("friendID");

        wordCloudViewModel = new ViewModelProvider(this).get(WordCloudViewModel.class);


        wordCloudViewModel.getAllMessageLive(userID, friendID).observe(this, new Observer<List<ChattingContent>>() {
            @Override
            public void onChanged(final List<ChattingContent> chattingContents) {
                buttonGetWordCloud.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        StringBuilder query = new StringBuilder();
                        if (chattingContents != null){
                            for (int i=0; i<chattingContents.size(); i++){
                                query.append(" ");
                                query.append(chattingContents.get(i).getMsgContent());
                            }
                            // 分词
                            ArrayList<String> result = JiebaSegmenter.getJiebaSegmenterSingleton().getDividedString(query.toString());
                            System.out.println("result" + result);
                            StringBuilder sentence = new StringBuilder();
                            for (int i=0; i<result.size(); i++){
                                sentence.append(result.get(i));
                                sentence.append(" ");
                            }
                            System.out.println("sentence: " + sentence.toString());


                            WordFrequency wordFrequency = new WordFrequency();
                            wordFrequency.insertWordNonNormalized(sentence.toString());
                            Map<String, Integer> wordMap  = wordFrequency.generate();
                            System.out.println("wordMap: "+ wordMap.toString());

                            WordCloud wd = new WordCloud(wordMap, 250, 250, 0xFF1F6ED4, Color.WHITE);
                            wd.setWordColorOpacityAuto(true);
                            Bitmap generatedWordCloudBmp = wd.generate();
                            imageViewWordCloud.setImageBitmap(generatedWordCloudBmp);
                        }else {
                            AlertDialog.Builder builder= new AlertDialog.Builder(v.getContext(), R.style.Theme_AppCompat_Light_Dialog_Alert);
                            builder.setTitle("您与该好友没有聊天记录！");
                            builder.setNeutralButton("知道了", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                            builder.create();
                            builder.show();
                        }


                    }
                });
            }
        });

//        String query = "我我我我我要去五道口吃肯德基，吃肯德基，吃肯德基，吃肯德基";
//        ArrayList<String> result = JiebaSegmenter.getJiebaSegmenterSingleton().getDividedString(query);
//        System.out.println("result" + result);
//        StringBuilder sentence = new StringBuilder();
//        for (int i=0; i<result.size(); i++){
//            sentence.append(result.get(i));
//            sentence.append(" ");
//        }
//        System.out.println("sentence: " + sentence.toString());
//
//
//        WordFrequency wordFrequency = new WordFrequency();
//        wordFrequency.insertWordNonNormalized(sentence.toString());
//        Map<String, Integer> wordMap  = wordFrequency.generate();
//        System.out.println("wordMap: "+ wordMap.toString());
//
//        WordCloud wd = new WordCloud(wordMap, 250, 250, 0xFF1F6ED4,Color.WHITE);
//        wd.setWordColorOpacityAuto(true);
//        Bitmap generatedWordCloudBmp = wd.generate();
//        imageButtonHeadPortrait.setImageBitmap(generatedWordCloudBmp);

    }





}