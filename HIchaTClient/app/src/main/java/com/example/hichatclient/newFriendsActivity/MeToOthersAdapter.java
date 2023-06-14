package com.example.hichatclient.newFriendsActivity;

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
import com.example.hichatclient.data.entity.MeToOthers;

import java.util.ArrayList;
import java.util.List;

public class MeToOthersAdapter extends RecyclerView.Adapter<MeToOthersAdapter.MeToOthersViewHolder> {
    List<MeToOthers> allMeToOthers = new ArrayList<>();

    public void setAllMeToOthers(List<MeToOthers> allMeToOthers) {
        this.allMeToOthers = allMeToOthers;
    }

    @NonNull
    @Override
    public MeToOthersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View itemView = layoutInflater.inflate(R.layout.cell_me_to_others, parent, false);
        return new MeToOthersViewHolder(itemView);
    }
    static class MeToOthersViewHolder extends RecyclerView.ViewHolder{
        TextView textViewNewFriendID, textViewNewFriendName, textViewNewFriendRep;
        ImageView imageViewFriendProfile;


        public MeToOthersViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNewFriendID = itemView.findViewById(R.id.textViewNewFriendID);
            textViewNewFriendName = itemView.findViewById(R.id.textViewNewFriendName);
            textViewNewFriendRep = itemView.findViewById(R.id.textViewNewFriendRep);
            imageViewFriendProfile = itemView.findViewById(R.id.imageView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MeToOthersViewHolder holder, int position) {
        MeToOthers meToOthers = allMeToOthers.get(position);
        holder.textViewNewFriendID.setText(meToOthers.getObjectID());
        holder.textViewNewFriendName.setText(meToOthers.getObjectName());
        if (meToOthers.getObjectResponse().equals("wait")){
            holder.textViewNewFriendRep.setText("对方未回应");
        }else if (meToOthers.getObjectResponse().equals("agree")){
            holder.textViewNewFriendRep.setText("对方已同意");
        }else if (meToOthers.getObjectResponse().equals("refuse")){
            holder.textViewNewFriendRep.setText("对方已拒绝");
        }

        if (meToOthers.getObjectProfile() != null){
            holder.imageViewFriendProfile.setImageBitmap(toRoundCorner(BitmapFactory.decodeByteArray(meToOthers.getObjectProfile(), 0, meToOthers.getObjectProfile().length), 2));
        }else {
            holder.imageViewFriendProfile.setImageResource(R.drawable.head);
        }


    }

    @Override
    public int getItemCount() {
        return allMeToOthers.size();
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
