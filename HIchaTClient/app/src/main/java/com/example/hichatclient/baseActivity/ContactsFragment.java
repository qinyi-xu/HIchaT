package com.example.hichatclient.baseActivity;

import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;

import com.example.hichatclient.ApplicationUtil;
import com.example.hichatclient.R;
import com.example.hichatclient.data.entity.Friend;
import com.example.hichatclient.newFriendsActivity.NewFriendsActivity;
import com.example.hichatclient.newFriendsActivity.OthersRequestActivity;
import com.example.hichatclient.newFriendsActivity.SearchFriendActivity;
import com.example.hichatclient.viewModel.ContactsViewModel;

import java.util.List;

public class ContactsFragment extends Fragment {
    private ContactsViewModel contactsViewModel;
    private FragmentActivity activity;
    private SharedPreferences sharedPreferences;
    private ApplicationUtil applicationUtil;

    private RecyclerView recyclerView;
    private FriendAdapter friendAdapter;

    private Button buttonAddFriend;
    private Button buttonNewFriends;

    private LiveData<List<Friend>> friendsLive;


    public ContactsFragment() {
        setHasOptionsMenu(true);  // 强制顶部工具条显示
    }

    public static ContactsFragment newInstance() {
        return new ContactsFragment();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.contacts_fragment, container, false);
    }

    // 通讯录界面的顶部工具条
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.contacts_menu, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.app_bar_search).getActionView();
        searchView.setMaxWidth(900);

    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = requireActivity();
        contactsViewModel = new ViewModelProvider(activity).get(ContactsViewModel.class);
        applicationUtil = (ApplicationUtil) activity.getApplication();

        // 获取Share Preferences中的数据
        sharedPreferences = activity.getSharedPreferences("MY_DATA", Context.MODE_PRIVATE);
        final String userID = sharedPreferences.getString("userID", "fail");


        recyclerView = activity.findViewById(R.id.recyclerFriends);
        recyclerView.addItemDecoration(new SimpleDividerDecoration(activity));
        recyclerView.setItemAnimator( new DefaultItemAnimator());
        friendAdapter = new FriendAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(friendAdapter);


        friendsLive = contactsViewModel.getUserFriendsFromSQL(userID);

        friendsLive.observe(activity, new Observer<List<Friend>>() {
            @Override
            public void onChanged(List<Friend> friends) {
                friendAdapter.setAllFriends(friends);
                friendAdapter.notifyDataSetChanged();

            }
        });

        // 跳转搜索新的好友界面
        buttonAddFriend = activity.findViewById(R.id.buttonAddFriend);
        buttonAddFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(activity, SearchFriendActivity.class);
                startActivity(intent);
            }
        });

        // 跳转新的朋友界面(NewFriendsActivity)
        buttonNewFriends = activity.findViewById(R.id.buttonNewFriends);
        buttonNewFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(activity, NewFriendsActivity.class);
                startActivity(intent);
            }
        });

        // TODO: Use the ViewModel
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