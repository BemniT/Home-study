package com.example.home_study.Adapter;

import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Model.Assessment;
import com.example.home_study.R;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

public class AssessmentAdapter
        extends RecyclerView.Adapter<AssessmentAdapter.Holder> {

    private List<Assessment> list;

    public AssessmentAdapter(List<Assessment> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_assessment, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int pos) {
        Assessment a = list.get(pos);

        holder.tvName.setText(a.name);
        holder.tvScore.setText(a.score + " / " + a.max);

        int percent = (int) ((a.score * 100f) / a.max);
        holder.progress.setProgress(0);

        ObjectAnimator.ofInt(holder.progress, "progress", percent).setDuration(600)
                .start();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvName, tvScore;
        LinearProgressIndicator progress;

        Holder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvAssessmentName);
            tvScore = v.findViewById(R.id.tvAssessmentScore);
//            tvTeacherName = itemView.findViewById(R.id.tvTeacherName);
            progress = v.findViewById(R.id.progressBar);
        }
    }
}

