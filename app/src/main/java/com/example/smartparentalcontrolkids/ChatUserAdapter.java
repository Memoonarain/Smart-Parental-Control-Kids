package com.example.smartparentalcontrolkids;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;

public class ChatUserAdapter extends RecyclerView.Adapter<ChatUserAdapter.ViewHolder> {

    private Context context;
    private List<ChatUserModel> userList;

    public ChatUserAdapter(Context context, List<ChatUserModel> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatUserModel user = userList.get(position);
        Log.w("TAG", "onBindViewHolder: "+user.getDeviceName());
        holder.deviceName.setText(user.getDeviceName());
        holder.lastMsg.setText(user.getLastMessage());
        holder.lastMsgTime.setText(user.getLastMessageTime());
        Log.w("TAG", "onBindViewHolder: "+user.getImageUrl());

        Glide.with(context)
                .load(user.getImageUrl())
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        // If image fails to load, set fallback style
                        holder.userImg.setBackgroundResource(R.drawable.bg_circle);
                        holder.userImg.setImageResource(R.drawable.ic_tab_user);
                        holder.userImg.setPadding(5, 5, 5, 5);
                        return false; // Let Glide handle placeholder or error image if set
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
                                                   DataSource dataSource, boolean isFirstResource) {
                        // If successful, no need for fallback styling
                        return false;
                    }
                })
                .into(holder.userImg);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatsActivity.class);
            intent.putExtra("parentId", user.getDeviceId());
            intent.putExtra("deviceName", user.getDeviceName());
            context.startActivity(intent);
        });


        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatsActivity.class);
            intent.putExtra("parentId", user.getDeviceId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName, lastMsg, lastMsgTime;
        ImageView userImg;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.txt_Device_Name);
            lastMsg = itemView.findViewById(R.id.txt_last_msg);
            lastMsgTime = itemView.findViewById(R.id.txt_last_msg_time);
            userImg = itemView.findViewById(R.id.userImg);
        }
    }
}
