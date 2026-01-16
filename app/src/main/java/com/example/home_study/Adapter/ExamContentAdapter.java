package com.example.home_study.Adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.home_study.Model.ExamContent;
import com.example.home_study.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ExamContentAdapter extends RecyclerView.Adapter<ExamContentAdapter.VH> {

    public interface OnExamContentClickListener {
        void onExamContentSelected(ExamContent content);
    }

    private final List<ExamContent> list;
    private final OnExamContentClickListener listener;

    public ExamContentAdapter(List<ExamContent> list, OnExamContentClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exam_content, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ExamContent c = list.get(position);
        holder.title.setText(c.getTitle());

        if (c.getChapterImage() != null && !c.getChapterImage().isEmpty()) {
            Glide.with(holder.icon.getContext())
                    .load(c.getChapterImage())
                    .centerCrop()
                    .placeholder(R.drawable.math) // create placeholder drawable
                    .error(R.drawable.math)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(holder.icon);
        } else {
            // fallback icon
            holder.icon.setImageResource(R.drawable.math);
        }
        if (c.hasExam()) {
            holder.examTag.setVisibility(View.VISIBLE);
            holder.examTag.setText(c.isExamPublished() ? "Exam (Published)" : "Exam (Unpublished)");
            holder.meta.setText(c.getTotalQuestions() > 0 ?
                    c.getTotalQuestions() + " Q • " + c.getDurationMinutes() + "m" :
                    "Exam info not available");
            holder.meta.setTextColor(Color.DKGRAY);
        } else {
            holder.examTag.setVisibility(View.GONE);
            holder.meta.setText("No exam for this chapter");
            holder.meta.setTextColor(Color.GRAY);
        }

        if (c.isUserTaken()) {
            holder.result.setVisibility(View.VISIBLE);
            holder.result.setText(String.format("Score: %d/%d %s", c.getUserScore(), c.getUserTotal(),
                    c.isUserPassed() ? "(Passed)" : "(Failed)"));
            holder.result.setTextColor(c.isUserPassed() ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));
        } else {
            holder.result.setVisibility(View.GONE);
        }

        // click handling
        holder.card.setOnClickListener(v -> {
            if (c.hasExam() && c.isExamPublished()) {
                listener.onExamContentSelected(c);
            } else if (c.hasExam()) {
                // exam exists but not published
                // provide a simple feedback (could be snackbar)
                android.widget.Toast.makeText(v.getContext(), "Exam is not published yet", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                android.widget.Toast.makeText(v.getContext(), "No exam for this chapter", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, meta, examTag, result;
        CardView card;
        CircleImageView icon;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.chapterTitle);
            meta = itemView.findViewById(R.id.chapterMeta);
            examTag = itemView.findViewById(R.id.chapterExamTag);
            result = itemView.findViewById(R.id.chapterResult);
            card = itemView.findViewById(R.id.chapterCard);
            icon = itemView.findViewById(R.id.chapterIcon);
        }
    }
}