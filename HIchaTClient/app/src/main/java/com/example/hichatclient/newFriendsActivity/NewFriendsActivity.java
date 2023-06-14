package com.example.hichatclient.newFriendsActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import com.example.hichatclient.ApplicationUtil;
import com.example.hichatclient.R;
import com.example.hichatclient.baseActivity.ContactsFragment;
import com.example.hichatclient.data.entity.Friend;
import com.example.hichatclient.data.entity.MeToOthers;
import com.example.hichatclient.data.entity.OthersToMe;
import com.example.hichatclient.service.ChatService;
import com.example.hichatclient.viewModel.NewFriendsViewModel;

import java.util.List;

public class NewFriendsActivity extends AppCompatActivity {
    private RecyclerView recyclerViewMeToOthers;
    private RecyclerView recyclerViewOtherToMe;
    private MeToOthersAdapter meToOthersAdapter;
    private OthersToMeAdapter othersToMeAdapter;
    private NewFriendsViewModel newFriendsViewModel;
    private SharedPreferences sharedPreferences;
    private ApplicationUtil applicationUtil;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_friends);

        newFriendsViewModel = new ViewModelProvider(this).get(NewFriendsViewModel.class);
        recyclerViewMeToOthers = findViewById(R.id.recyclerViewMeToOthers);
        recyclerViewMeToOthers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMeToOthers.addItemDecoration(new SimpleDividerDecoration(this));
        recyclerViewMeToOthers.setItemAnimator( new DefaultItemAnimator());
        meToOthersAdapter = new MeToOthersAdapter();
        recyclerViewMeToOthers.setAdapter(meToOthersAdapter);
        recyclerViewOtherToMe = findViewById(R.id.recyclerViewOthersToMe);
        recyclerViewOtherToMe.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewOtherToMe.addItemDecoration(new SimpleDividerDecoration(this));
        recyclerViewOtherToMe.setItemAnimator( new DefaultItemAnimator());
        othersToMeAdapter = new OthersToMeAdapter();
        recyclerViewOtherToMe.setAdapter(othersToMeAdapter);

        applicationUtil = (ApplicationUtil) NewFriendsActivity.this.getApplication();

        // 获取Share Preferences中的数据
        sharedPreferences = getSharedPreferences("MY_DATA", Context.MODE_PRIVATE);
        final String userID = sharedPreferences.getString("userID", "fail");

        // 获取applicationUtil中的数据
        final String userShortToken = applicationUtil.getUserShortToken();

        // 观察数据库中MeToOthers的变化
        newFriendsViewModel.getAllMeToOthersFromSQL(userID).observe(this, new Observer<List<MeToOthers>>() {
            @Override
            public void onChanged(List<MeToOthers> meToOthers) {
                meToOthersAdapter.setAllMeToOthers(meToOthers);
                meToOthersAdapter.notifyDataSetChanged();
            }
        });

        // 观察数据库中OthersToMe的变化
        newFriendsViewModel.getAllOthersToMeFromSQL(userID).observe(this, new Observer<List<OthersToMe>>() {
            @Override
            public void onChanged(List<OthersToMe> othersToMes) {
                othersToMeAdapter.setAllOthersToMe(othersToMes);
                othersToMeAdapter.notifyDataSetChanged();
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