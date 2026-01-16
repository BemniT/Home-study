package com.example.home_study.Adapter;

import android.graphics.Bitmap;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.home_study.Model.Subject;
import com.example.home_study.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder> {

    public interface OnSubjectClickListener {
        void onSubjectClick(Subject subject);
    }

    private final SparseIntArray subjectIconMap = new SparseIntArray();
    private List<Subject> subjectList;
    private OnSubjectClickListener listener;

    public SubjectAdapter(List<Subject> subjectList, OnSubjectClickListener listener) {
        this.subjectList = subjectList;
        this.listener = listener;
    }

    public void setSubjects(List<Subject> newList) {
        this.subjectList = newList != null ? newList : subjectList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject, parent, false);
        return new SubjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
        Subject subject = subjectList.get(position);

        holder.tvSubjectName.setText(subject.getName());
        holder.tvGradeSection.setText("Grade " + subject.getGrade() + subject.getSection());

        // Load the icon safely with Picasso and resize to avoid decoding huge bitmaps.
        int resId = getSubjectIcon(subject.getName());

        // cancel any pending request for this view (important for recycled views)
        Picasso.get().cancelRequest(holder.imgSubject);
        holder.imgSubject.setImageDrawable(null);

        // compute a target px size based on density (adjust 72dp to suit your design)
        final int targetDp = 72;
        final float density = holder.imgSubject.getResources().getDisplayMetrics().density;
        final int targetPx = Math.round(targetDp * density);

        Picasso.get()
                .load(resId)
                .placeholder(R.drawable.examfill) // create a small placeholder drawable
                .resize(targetPx, targetPx)   // downsample to view size during decode
                .centerCrop()
                .config(Bitmap.Config.RGB_565) // reduce memory usage
                .onlyScaleDown()
                .into(holder.imgSubject);

        holder.itemView.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            if (listener != null) listener.onSubjectClick(subject);
        });
    }

    @Override
    public int getItemCount() {
        return subjectList != null ? subjectList.size() : 0;
    }

    private int getSubjectIcon(String name) {
        String key = name != null ? name.toLowerCase() : "";
        // Fallback mapping
        if (key.contains("math")) return R.drawable.math;
        if (key.contains("biology")) return R.drawable.biology;
        if (key.contains("chemistry")) return R.drawable.chemistry;
        if (key.contains("physics")) return R.drawable.physics;
        if (key.contains("english")) return R.drawable.english;
        if (key.contains("geography")) return R.drawable.geography;
        if (key.contains("history")) return R.drawable.history;
        if (key.contains("civics")) return R.drawable.civics;
        if (key.contains("ict")) return R.drawable.ict;
        return R.drawable.math;
    }

    static class SubjectViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubjectName, tvGradeSection;
        ImageView imgSubject;

        public SubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubjectName = itemView.findViewById(R.id.subjectName);
            tvGradeSection = itemView.findViewById(R.id.tvGradeSection);
            imgSubject = itemView.findViewById(R.id.subjectImage);
        }
    }
}