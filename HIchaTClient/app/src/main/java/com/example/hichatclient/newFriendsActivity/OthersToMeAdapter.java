package com.example.hichatclient.newFriendsActivity;

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
import android.widget.Adapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hichatclient.R;
import com.example.hichatclient.chatActivity.ChatActivity;
import com.example.hichatclient.data.entity.OthersToMe;

import java.util.ArrayList;
import java.util.List;

public class OthersToMeAdapter extends RecyclerView.Adapter<OthersToMeAdapter.OthersToMeViewHolder> {
    List<OthersToMe> allOthersToMe = new ArrayList<>();

    public void setAllOthersToMe(List<OthersToMe> allOthersToMe) {
        this.allOthersToMe = allOthersToMe;
    }

    @NonNull
    @Override
    public OthersToMeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View itemView = layoutInflater.inflate(R.layout.cell_others_to_me, parent, false);
        return new OthersToMeViewHolder(itemView);
    }

    static class OthersToMeViewHolder extends RecyclerView.ViewHolder {
        TextView textViewObjectID, textViewObjectName, textViewUserResponse;
        ImageView imageViewObjectProfile;

        public OthersToMeViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewObjectID = itemView.findViewById(R.id.textViewObjectID);
            textViewObjectName = itemView.findViewById(R.id.textViewObjectName);
            textViewUserResponse = itemView.findViewById(R.id.textViewUserRep);
            imageViewObjectProfile = itemView.findViewById(R.id.imageView3);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final OthersToMeViewHolder holder, int position) {
        final OthersToMe othersToMe = allOthersToMe.get(position);
        holder.textViewObjectID.setText(othersToMe.getObjectID());
        holder.textViewObjectName.setText(othersToMe.getObjectName());
        if (othersToMe.getUserResponse().equals("wait")){
            holder.textViewUserResponse.setText("您未回应");
        }else if (othersToMe.getUserResponse().equals("refuse")){
            holder.textViewUserResponse.setText("您已拒绝");
        }else if (othersToMe.getUserResponse().equals("agree")){
            holder.textViewUserResponse.setText("您已同意");
        }

        if (othersToMe.getObjectProfile() != null){
            holder.imageViewObjectProfile.setImageBitmap(toRoundCorner(BitmapFactory.decodeByteArray(othersToMe.getObjectProfile(), 0, othersToMe.getObjectProfile().length), 2));
        }else {
            holder.imageViewObjectProfile.setImageResource(R.drawable.head);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(holder.itemView.getContext(), OthersRequestActivity.class);
                intent.putExtra("objectID", othersToMe.getObjectID());
                holder.itemView.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return allOthersToMe.size();
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
