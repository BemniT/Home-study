package com.example.home_study.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.Model.ExamCenter;
import com.example.home_study.R;

import java.util.List;

public class ExamCenterAdapter extends RecyclerView.Adapter<ExamCenterAdapter.ViewHolder> {

    private List<ExamCenter> questionList;

    public ExamCenterAdapter(List<ExamCenter> questionList) {
        this.questionList = questionList;
    }

    @NonNull
    @Override
    public ExamCenterAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.exam_center_card,parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExamCenterAdapter.ViewHolder holder, int position) {

        ExamCenter questions = questionList.get(position);
        holder.question.setText(questions.getQuestion());
        holder.chooses.removeAllViews();
        for (String option : questions.getOptions()){
            RadioButton radioButton = new RadioButton(holder.itemView.getContext());
            radioButton.setText(option);
            holder.chooses.addView(radioButton);
        }

        holder.chooses.setOnCheckedChangeListener((group, checkedId) ->{
                    RadioButton selectedRadioButton = group.findViewById(checkedId);

                    if (selectedRadioButton != null){
                        String selectedAnswer = selectedRadioButton.getText().toString();

                        if (selectedAnswer.equals(questions.getCorrectAnswer()))
                        {
                            holder.explanation.setText("✅ Correct! " + questions.getExplanation());
//                            holder.explanation.setTextColor(R.color.Green);
                        }else {
                            holder.explanation.setText("❌ Incorrect! " + questions.getExplanation());
                        }
                    }
                });

    }

    @Override
    public int getItemCount() {
        return questionList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView question, explanation;
        private RadioGroup chooses;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            question = itemView.findViewById(R.id.question);
            explanation = itemView.findViewById(R.id.explanation);

            chooses = itemView.findViewById(R.id.questionChoose);
        }
    }
}
