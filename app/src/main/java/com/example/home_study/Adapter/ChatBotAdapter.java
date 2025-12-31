package com.example.home_study.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Model.BotMessage;
import com.example.home_study.R;

import java.util.List;
public class ChatBotAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<BotMessage> messageList;

    public ChatBotAdapter(List<BotMessage> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == BotMessage.TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chatbot_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chatbot_bot, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        BotMessage message = messageList.get(position);

        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).txtMessage.setText(message.getMessage());
        } else {
            ((BotViewHolder) holder).txtMessage.setText(message.getMessage());
        }



    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage;

        UserViewHolder(View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txtMessage);
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage;


        BotViewHolder(View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txtMessage);

        }
    }
}