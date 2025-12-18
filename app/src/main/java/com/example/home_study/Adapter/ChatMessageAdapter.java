package com.example.home_study.Adapter;

import static android.view.View.VISIBLE;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Model.Chat;
import com.example.home_study.Model.Message;
import com.example.home_study.R;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static  final int SENT = 1;
    private static final int RECEIVED = 2;

    private List<Message> messages;

    private String currentUserId;

    private OnMessageActionListener listener;

    public ChatMessageAdapter(List<Message> messages, String currentUserId, OnMessageActionListener listener) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getSenderId().equals(currentUserId)
                ?SENT
                :RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        int layout = viewType == SENT
                ? R.layout.sent_message_card
                :R.layout.received_message_card;

        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);

        return new MessageViewHolder(view);
    }

    public interface OnMessageActionListener{
        void onEdit(Message message, int postion);
        void onDelete(Message message, int postion);
    }
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        Message message = messages.get(position);


        if (message.getSenderId().equals(currentUserId)){
            if (message.isSeen()){
                ((MessageViewHolder) holder).seenIcon.setImageResource(R.drawable.double_check);
            } else {
                ((MessageViewHolder) holder).seenIcon.setImageResource(R.drawable.single_check);
            }
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (!message.getSenderId().equals(currentUserId)){ return true;}
            if (message.isDeleted()){
                Toast.makeText(v.getContext(), "Deleted messages cannot be edited", Toast.LENGTH_SHORT).show();
                return true;
            }

            new AlertDialog.Builder(v.getContext())
                    .setTitle("Message Options")
                    .setItems(new String[]{"Edit", "Delete"}, ((dialog, which) -> {
                        if (which == 0){
                            listener.onEdit(message, position);
                        } else {
                            listener.onDelete(message,position);
                        }
                    })).show();
//            if (message.getSenderId().equals(currentUserId)){
//                showDeleteDialog(messageKey);
//            }
            return true;
        });

        if (message.isDeleted()){
            ((MessageViewHolder) holder).messageText.setText("This message was deleted");
            ((MessageViewHolder) holder).messageText.setTextColor(Color.GRAY);
            ((MessageViewHolder) holder).messageText.setTypeface(null, Typeface.ITALIC);
        } else{
            ((MessageViewHolder) holder).messageText.setText(messages.get(position).getText());
            ((MessageViewHolder) holder).messageText.setTextColor(Color.BLACK);
        }
        if (message.isEdited()){
            ((MessageViewHolder) holder).editedText.setVisibility(View.VISIBLE);
            ((MessageViewHolder) holder).editedText.setText("edited");
        } else {
            ((MessageViewHolder) holder).editedText.setVisibility(View.GONE);
        }


    }

    public void animateChange(RecyclerView recyclerView, int position){
        RecyclerView.ViewHolder holder =
                recyclerView.findViewHolderForAdapterPosition(position);

        if (holder != null){
            holder.itemView.setAlpha(0.3f);
            holder.itemView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder{

        TextView messageText, editedText;
        ImageView seenIcon;
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textMessage);
            seenIcon = itemView.findViewById(R.id.seenIcon);
            editedText = itemView.findViewById(R.id.textEditedText);
        }
    }
}
