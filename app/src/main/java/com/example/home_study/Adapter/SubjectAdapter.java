package com.example.home_study.Adapter;

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

//        subjectIconMap.put("math".hashCode(),R.drawable.math);
//        subjectIconMap.put("biology".hashCode(),R.drawable.biology);
//        subjectIconMap.put("chemistry".hashCode(),R.drawable.chemistry);
//        subjectIconMap.put("physics".hashCode(),R.drawable.physics);
//        subjectIconMap.put("english".hashCode(),R.drawable.english);
//        subjectIconMap.put("geography".hashCode(),R.drawable.geography);
//        subjectIconMap.put("history".hashCode(),R.drawable.history);
//        subjectIconMap.put("civics".hashCode(),R.drawable.civics);
//        subjectIconMap.put("ict".hashCode(),R.drawable.ict);

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
        holder.tvGradeSection.setText(
                "Grade " + subject.getGrade() + subject.getSection()
        );

        holder.imgSubject.setImageResource(getSubjectIcon(subject.getName()));
        holder.itemView.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            if (listener != null) listener.onSubjectClick(subject);
        });
    }

    @Override
    public int getItemCount() {
        return subjectList.size();
    }
    private int getSubjectIcon(String name) {
        String key = name.toLowerCase();
        for (int i = 0; i < subjectIconMap.size(); i++) {
            if (key.contains(String.valueOf(subjectIconMap.keyAt(i)).replaceAll("\\D",""))) {
                return subjectIconMap.valueAt(i);
            }
        }

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


