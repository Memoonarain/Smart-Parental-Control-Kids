package com.example.smartparentalcontrolkids;

import android.icu.text.SimpleDateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<MessageModel> messages;
    private String currentUserId;

    public MessageAdapter(List<MessageModel> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        MessageModel message = messages.get(position);

        holder.messageText.setText(message.getText());

        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(new Date(message.getTimestamp()));
        holder.messageTime.setText(time);

        // Align message left or right
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.container.getLayoutParams();
        params.gravity = message.getSenderId().equals(currentUserId) ? Gravity.START : Gravity.END;
        holder.container.setLayoutParams(params);

        if (message.getSenderId().equals(currentUserId)) {
            holder.messageText.setBackgroundResource(R.drawable.bg_message_sent);
            holder.messageText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));

            params.gravity = Gravity.END;
        } else {
            holder.messageText.setBackgroundResource(R.drawable.bg_message_received);
            holder.messageText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));

            params.gravity = Gravity.START;
        }
        holder.container.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, messageTime;
        LinearLayout container;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            messageTime = itemView.findViewById(R.id.messageTime);
            container = itemView.findViewById(R.id.messageContainer);
        }
    }
}
