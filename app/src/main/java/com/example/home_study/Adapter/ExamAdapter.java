package com.example.home_study.Adapter;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.home_study.ContentActivity;
import com.example.home_study.ExamContentActivity;
import com.example.home_study.Model.Book;
import com.example.home_study.R;

import java.util.List;

public class ExamAdapter extends RecyclerView.Adapter<ExamAdapter.ViewHolder> {

    private List<Book> bookList;
    private Activity bookContext;

    public ExamAdapter(List<Book> bookList, Activity bookContext) {
        this.bookList = bookList;
        this.bookContext = bookContext;
    }

    @NonNull
    @Override
    public ExamAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.exam_card,parent,false);
        return new ExamAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExamAdapter.ViewHolder holder, int position) {

        Book book = bookList.get(position);


        Glide.with(bookContext)
                .load(book.getBookImage())
//                .placeholder(R.drawable.placeholder)
//                .error(R.drawable.error_image)
                .into(holder.bookImage);

        holder.bookTitle.setText(book.getBookTitle());
        holder.bookGrade.setText(book.getBookGrade());

        Log.d("Book", "Book Title: "+ book.getBookTitle());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(bookContext, ExamContentActivity.class);
                intent.putExtra("examBookTitle",book.getBookTitle());
                intent.putExtra("examBookGrade", book.getBookGrade());
                bookContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView bookTitle, bookGrade;
        ImageView bookImage;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            bookTitle = itemView.findViewById(R.id.bookTitle);
            bookGrade = itemView.findViewById(R.id.bookGrade);
            bookImage = itemView.findViewById(R.id.bookImage);
        }
    }
}
