package com.example.hichatclient.chatActivity;

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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hichatclient.R;
import com.example.hichatclient.data.entity.ChattingContent;
import com.example.hichatclient.data.entity.Friend;
import com.example.hichatclient.data.entity.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<ChattingContent> allMsg = new ArrayList<>();
    private byte[] bytesLeft;
    private byte[] bytesRight;
    private SimpleDateFormat newSimpleDateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.getDefault());


    public void setAllMsg(List<ChattingContent> allMsg) {
        this.allMsg = allMsg;
    }

    public void setBytesLeft(byte[] bytesLeft) {
        this.bytesLeft = bytesLeft;
    }

    public void setBytesRight(byte[] bytesRight) {
        this.bytesRight = bytesRight;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View itemView = layoutInflater.inflate(R.layout.cell_message, parent, false);
        return new MessageViewHolder(itemView);
        //return null;
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {



        // position是当前子项在集合中的位置，通过position参数得到当前项的Msg实例
        ChattingContent chattingContent = allMsg.get(position);
        if (bytesLeft != null){
            holder.leftImageViewHead.setImageBitmap(toRoundCorner(BitmapFactory.decodeByteArray(bytesLeft, 0, bytesLeft.length), 2));
        }else {
            holder.leftImageViewHead.setImageResource(R.drawable.head);
        }

        if (bytesRight != null){
            holder.rightImageViewHead.setImageBitmap(toRoundCorner(BitmapFactory.decodeByteArray(bytesRight, 0, bytesRight.length), 2));
        }else {
            holder.rightImageViewHead.setImageResource(R.drawable.head);
        }

        if (chattingContent.getMsgType().equals("receive")) {
            //如果是收到的信息，则显示左边的布局信息，将右边的信息隐藏
            holder.rightImageViewHead.setVisibility(View.GONE);
            holder.rightImageViewRead.setVisibility(View.GONE);
            holder.rightLinearLayout.setVisibility(View.GONE);
            holder.rightMsg.setVisibility(View.GONE);


            holder.leftImageViewHead.setVisibility(View.VISIBLE);
            holder.leftImageViewSentiment.setVisibility(View.VISIBLE);
            if (chattingContent.getSentiment() == null){
                holder.leftImageViewSentiment.setImageResource(R.drawable.thinking);
            }else {
                if (chattingContent.getSentiment().equals("sad")){
                    holder.leftImageViewSentiment.setImageResource(R.drawable.sad);
                }else if (chattingContent.getSentiment().equals("neutral")){
                    holder.leftImageViewSentiment.setImageResource(R.drawable.peace);
                }else if (chattingContent.getSentiment().equals("happy")){
                    holder.leftImageViewSentiment.setImageResource(R.drawable.happy);
                }else if (chattingContent.getSentiment().equals("thinking")){
                    holder.leftImageViewSentiment.setImageResource(R.drawable.thinking);
                }else if (chattingContent.getSentiment().equals("like")){
                    holder.leftImageViewSentiment.setImageResource(R.drawable.like);
                }else if (chattingContent.getSentiment().equals("fearful")){
                    holder.leftImageViewSentiment.setImageResource(R.drawable.fearful);
                }else if (chattingContent.getSentiment().equals("angry")){
                    holder.leftImageViewSentiment.setImageResource(R.drawable.angry);
                }else if (chattingContent.getSentiment().equals("disgusting")){
                    holder.leftImageViewSentiment.setImageResource(R.drawable.disgusting);
                }
            }

            holder.leftLinearLayout.setVisibility(View.VISIBLE);
            holder.leftMsg.setVisibility(View.VISIBLE);
            holder.leftMsg.setText(chattingContent.getMsgContent());
        } else {
            // 若是发出的信息，则显示右边的布局信息，隐藏左边的布局信息
            holder.leftImageViewHead.setVisibility(View.GONE);
            holder.leftImageViewSentiment.setVisibility(View.GONE);
            holder.leftLinearLayout.setVisibility(View.GONE);
            holder.leftMsg.setVisibility(View.GONE);


            holder.rightImageViewHead.setVisibility(View.VISIBLE);
            if (chattingContent.isRead()){
                holder.rightImageViewRead.setVisibility(View.VISIBLE);
            }else if (!chattingContent.isRead()){
                holder.rightImageViewRead.setVisibility(View.GONE);
            }
            holder.rightLinearLayout.setVisibility(View.VISIBLE);
            holder.rightMsg.setVisibility(View.VISIBLE);
            holder.rightMsg.setText(chattingContent.getMsgContent());
        }


        holder.msgTime.setText(newSimpleDateFormat.format(chattingContent.getMsgTime()));

    }

    @Override
    public int getItemCount() {
        return allMsg.size();
    }



    static class MessageViewHolder extends RecyclerView.ViewHolder {
        ImageView leftImageViewHead;
        ImageView rightImageViewHead;
        ImageView leftImageViewSentiment;
        ImageView rightImageViewRead;
        LinearLayout leftLinearLayout;
        LinearLayout rightLinearLayout;
        TextView leftMsg;
        TextView rightMsg;
        TextView msgTime;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            leftImageViewHead = itemView.findViewById(R.id.left_head);
            leftLinearLayout = itemView.findViewById(R.id.left_ll);
            leftMsg = itemView.findViewById(R.id.left_tv);
            leftImageViewSentiment = itemView.findViewById(R.id.left_sentiment);

            rightImageViewHead = itemView.findViewById(R.id.right_head);
            rightLinearLayout = itemView.findViewById(R.id.right_ll);
            rightImageViewRead = itemView.findViewById(R.id.right_read);
            rightMsg = itemView.findViewById(R.id.right_tv);

            msgTime = itemView.findViewById(R.id.textViewMsgTime);
        }
    }


    public static Bitmap toRoundCorner(Bitmap bitmap, float ratio) {
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

        return output;

    }
}
