package com.example.hichatclient.baseActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hichatclient.R;
import com.example.hichatclient.chatActivity.ChatActivity;
import com.example.hichatclient.data.entity.ChattingContent;
import com.example.hichatclient.data.entity.ChattingFriend;
import com.example.hichatclient.data.entity.Friend;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<ChattingFriend> chattingFriends = new ArrayList<>();

    public void setChattingFriends(List<ChattingFriend> chattingFriends) {
        this.chattingFriends = chattingFriends;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View itemView = layoutInflater.inflate(R.layout.cell_chat, parent, false);
        return new ChatViewHolder(itemView);
        //return null;
    }

    @Override
    public void onBindViewHolder(@NonNull final ChatViewHolder holder, int position) {
        final ChattingFriend chattingFriend = chattingFriends.get(position);
        if (chattingFriend.getFriendProfile() != null){
            holder.imageViewChatFriendImage.setImageBitmap(toRoundCorner(BitmapFactory.decodeByteArray(chattingFriend.getFriendProfile(), 0, chattingFriend.getFriendProfile().length), 2));
        }else {
            holder.imageViewChatFriendImage.setImageResource(R.drawable.head);
        }

        holder.textViewChatFriendName.setText(chattingFriend.getFriendName());
        holder.textViewChatNewContent.setText(chattingFriend.getTheLastMsg());

        if (chattingFriend.getTime() == 0){
            holder.itemView.setEnabled(false);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(holder.itemView.getContext(), ChatActivity.class);
                intent.putExtra("friendID", chattingFriend.getFriendID());
                intent.putExtra("friendName", chattingFriend.getFriendName());
                holder.itemView.getContext().startActivity(intent);

            }
        });
    }

    @Override
    public int getItemCount() {
        return chattingFriends.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder{
        TextView textViewChatFriendName, textViewChatNewContent;
        ImageView imageViewChatFriendImage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewChatFriendName = itemView.findViewById(R.id.chatFriendName);
            textViewChatNewContent = itemView.findViewById(R.id.chatNewContent);
            imageViewChatFriendImage = itemView.findViewById(R.id.imageViewChatFriend);
        }
    }

    public static Bitmap toRoundCorner(Bitmap bitmap, float ratio) {
        System.out.println("图片是否变成圆形模式了+++++++++++++");
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rectF, bitmap.getWidth() / ratio,
                bitmap.getHeight() / ratio, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        System.out.println("pixels+++++++" + String.valueOf(ratio));

        return output;

    }

}
