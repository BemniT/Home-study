package com.example.home_study.Adapter;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Model.Message;
import com.example.home_study.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ChatMessageAdapter with image display tuned to:
 * - clip image corners to the card (MaterialCardView)
 * - keep bubble size wrap_content while capping image/text width to MAX_PERCENT of screen
 * - load images with Picasso using resize(...).onlyScaleDown().centerInside() so images never exceed cap
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int SENT = 1;
    private static final int RECEIVED = 2;

    // maximum percentage of screen width a bubble / image can occupy
    private static final float MAX_PERCENT = 0.80f; // 80%
    // optional: maximum percent of screen height for image height
    private static final float MAX_IMAGE_HEIGHT_PERCENT = 0.60f; // 60%

    private List<Message> messages;
    private String currentUserId;
    private OnMessageActionListener listener;

    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("h:mm a", Locale.getDefault());

    public ChatMessageAdapter(List<Message> messages, String currentUserId, OnMessageActionListener listener) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public interface OnMessageActionListener {
        void onEdit(Message message, int postion);
        void onDelete(Message message, int postion);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getSenderId().equals(currentUserId) ? SENT : RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // keep your layouts (replace paths if different)
        int layout = viewType == SENT ? R.layout.sent_message_card : R.layout.received_message_card;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder0, int position) {
        Message message = messages.get(position);
        MessageViewHolder holder = (MessageViewHolder) holder0;

        // Format timestamp (defensive about seconds vs millis)
        long ts = message.getTimeStamp();
        if (ts > 0 && ts < 1_000_000_000_000L) ts = ts * 1000L;
        String formatted = TIME_FORMAT.format(new Date(ts));
        holder.textDateTime.setText(formatted);

        // Determine if this message contains an image
        boolean isImage = "image".equalsIgnoreCase(message.getType()) ||
                (message.getImageUrl() != null && !message.getImageUrl().isEmpty());

        if (isImage) {
            // show image container and hide text
            holder.imageCard.setVisibility(View.VISIBLE);
            holder.imageAttachment.setVisibility(View.VISIBLE);
            holder.textMessage.setVisibility(View.GONE);

            // compute caps
            int screenWidth = holder.itemView.getResources().getDisplayMetrics().widthPixels;
            int maxWidth = (int) (screenWidth * MAX_PERCENT);

            int screenHeight = holder.itemView.getResources().getDisplayMetrics().heightPixels;
            int maxHeight = (int) (screenHeight * MAX_IMAGE_HEIGHT_PERCENT);

            // apply caps to the ImageView (so layout won't exceed)
            holder.imageAttachment.setMaxWidth(maxWidth);
            holder.imageAttachment.setMaxHeight(maxHeight);

            // Picasso: resize to (maxWidth x maxWidth) onlyScaleDown + centerInside to preserve aspect ratio
            // This ensures the loaded bitmap never exceeds the cap; small images are not upscaled.
            RequestCreator req = Picasso.get()
                    .load(message.getImageUrl())
                    .placeholder(R.drawable.math) // your placeholder
                    .error(R.drawable.illustration_chat_empty) // your error drawable
                    .resize(maxWidth, maxWidth)
                    .onlyScaleDown()
                    .centerInside();

            req.into(holder.imageAttachment);

            // full preview on click
            holder.imageAttachment.setOnClickListener(v -> {
                ImageView iv = new ImageView(v.getContext());
                iv.setAdjustViewBounds(true);
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                Picasso.get().load(message.getImageUrl()).into(iv);
                new AlertDialog.Builder(v.getContext()).setView(iv).setPositiveButton("Close", null).show();
            });

        } else {
            // text message - hide image container
            holder.imageCard.setVisibility(View.GONE);
            holder.textMessage.setVisibility(View.VISIBLE);

            if (message.isDeleted()) {
                holder.textMessage.setText("This message was deleted");
                // keep color/typography consistent with your design
                if (message.getSenderId().equals(currentUserId)) {
                    holder.textMessage.setTextColor(holder.itemView.getResources().getColor(R.color.white));
                } else {
                    holder.textMessage.setTextColor(holder.itemView.getResources().getColor(R.color.black));
                }
                holder.textMessage.setTypeface(null, Typeface.ITALIC);
            } else {
                holder.textMessage.setText(message.getText());
                if (message.getSenderId().equals(currentUserId)) {
                    holder.textMessage.setTextColor(holder.itemView.getResources().getColor(R.color.white));
                } else {
                    holder.textMessage.setTextColor(holder.itemView.getResources().getColor(R.color.black));
                }
                holder.textMessage.setTypeface(null, Typeface.NORMAL);
            }
        }

        // seen icon only visible for sent messages
        if (holder.seenIcon != null) {
            if (message.getSenderId().equals(currentUserId)) {
                holder.seenIcon.setVisibility(View.VISIBLE);
                holder.seenIcon.setImageResource(message.isSeen() ? R.drawable.double_check : R.drawable.single_check);
            } else {
                holder.seenIcon.setVisibility(View.GONE);
            }
        }

        // long-click for edit/delete (your existing behavior)
        holder.itemView.setOnLongClickListener(v -> {
            if (!message.getSenderId().equals(currentUserId)) return true;
            if (message.isDeleted()) return true;
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Message Options")
                    .setItems(new String[]{"Edit", "Delete"}, (dialog, which) -> {
                        if (which == 0) listener.onEdit(message, position);
                        else listener.onDelete(message, position);
                    }).show();
            return true;
        });
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
    public int getItemCount() { return messages.size(); }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textDateTime;
        ImageView seenIcon;
        ImageView imageAttachment;
        com.google.android.material.card.MaterialCardView imageCard;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textDateTime = itemView.findViewById(R.id.textDateTime);
            textMessage = itemView.findViewById(R.id.textMessage);
            seenIcon = itemView.findViewById(R.id.seenIcon);
            imageAttachment = itemView.findViewById(R.id.imageAttachment);
            imageCard = itemView.findViewById(R.id.imageCard);

            // compute max pixel width (80% of screen)
            int screenWidth = itemView.getResources().getDisplayMetrics().widthPixels;
            int max = (int) (screenWidth * MAX_PERCENT);
            int screenHeight = itemView.getResources().getDisplayMetrics().heightPixels;
            int maxImageHeight = (int) (screenHeight * MAX_IMAGE_HEIGHT_PERCENT);

            // Apply caps so layout measures correctly without the need for guideline
            if (textMessage != null) textMessage.setMaxWidth(max);
            if (imageAttachment != null) {
                imageAttachment.setMaxWidth(max);
                imageAttachment.setMaxHeight(maxImageHeight);
            }

            // Optional: give the card a small elevation programmatically (if not set in XML)
            if (imageCard != null) {
                imageCard.setCardElevation(itemView.getResources().getDimension(com.intuit.sdp.R.dimen._1sdp));
                imageCard.setUseCompatPadding(false);
                imageCard.setPreventCornerOverlap(false);
            }
        }
    }
}