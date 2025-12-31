package com.example.home_study.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Model.Subject;
import com.example.home_study.R;

import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder> {

    public interface OnSubjectClickListener {
        void onSubjectClick(Subject subject);
    }

    private List<Subject> subjectList;
    private OnSubjectClickListener listener;

    public SubjectAdapter(List<Subject> subjectList, OnSubjectClickListener listener) {
        this.subjectList = subjectList;
        this.listener = listener;
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

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSubjectClick(subject);
            }
        });
    }

    @Override
    public int getItemCount() {
        return subjectList.size();
    }

    static class SubjectViewHolder extends RecyclerView.ViewHolder {

        TextView tvSubjectName, tvGradeSection;

        public SubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubjectName = itemView.findViewById(R.id.subjectName);
            tvGradeSection = itemView.findViewById(R.id.tvGradeSection);
        }
    }
}


