package com.example.hichatclient.baseActivity;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.hichatclient.ApplicationUtil;
import com.example.hichatclient.R;
import com.example.hichatclient.data.entity.ChattingFriend;
import com.example.hichatclient.data.entity.Friend;
import com.example.hichatclient.viewModel.ChatsViewModel;

import java.util.List;

public class ChatsFragment extends Fragment {
    private ChatsViewModel chatsViewModel;
    private FragmentActivity activity;
    private SharedPreferences sharedPreferences;
    private ApplicationUtil applicationUtil;

    private List<ChattingFriend> allChattingFriend;

    private ChatAdapter chatAdapter;
    private RecyclerView recyclerView;

    public static ChatsFragment newInstance() {
        return new ChatsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.chats_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = requireActivity();
        chatsViewModel = new ViewModelProvider(activity).get(ChatsViewModel.class);
        applicationUtil = (ApplicationUtil) activity.getApplication();

        // 获取Share Preferences中的数据
        sharedPreferences = activity.getSharedPreferences("MY_DATA", Context.MODE_PRIVATE);
        final String userID = sharedPreferences.getString("userID", "fail");

        recyclerView = activity.findViewById(R.id.recyclerViewChats);
        recyclerView.addItemDecoration(new SimpleDividerDecoration(activity));
        recyclerView.setItemAnimator( new DefaultItemAnimator());
        chatAdapter = new ChatAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(chatAdapter);


        chatsViewModel.getAllChattingFriendFromSQL(userID).observe(activity, new Observer<List<ChattingFriend>>() {
            @Override
            public void onChanged(List<ChattingFriend> chattingFriends) {
                chatAdapter.setChattingFriends(chattingFriends);
                chatAdapter.notifyDataSetChanged();
            }
        });
    }






    public class SimpleDividerDecoration extends RecyclerView.ItemDecoration {

        private int dividerHeight;
        private Paint dividerPaint;

        public SimpleDividerDecoration(Context context) {
            dividerPaint = new Paint();
            dividerPaint.setColor(context.getResources().getColor(R.color.divider));

            dividerHeight = 1;
        }


        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.bottom = dividerHeight;
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int childCount = parent.getChildCount();
            int left = parent.getPaddingLeft() + 200;
            int right = parent.getWidth() - parent.getPaddingRight() - 50;

            for (int i = 0; i < childCount - 1; i++) {
                View view = parent.getChildAt(i);
                float top = view.getBottom();
                float bottom = view.getBottom() + dividerHeight;
                c.drawRect(left, top, right, bottom, dividerPaint);
            }
        }
    }

}