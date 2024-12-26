package com.example.home_study.Adapter;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.home_study.ContentActivity;
import com.example.home_study.Model.Book;
import com.example.home_study.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    private List<Book> bookList;
    private Activity bookActivity;

    public BookAdapter(List<Book> bookList, Activity bookActivity) {
        this.bookList = bookList;
        this.bookActivity = bookActivity;
    }

    @NonNull
    @Override
    public BookAdapter.BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view  = LayoutInflater.from(parent.getContext()).inflate(R.layout.book_card,parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookAdapter.BookViewHolder holder, int position) {

        Book book = bookList.get(position);

        Picasso.get().load(book.getBookImage()).placeholder(R.drawable.placeholderbook).into(holder.bookImage);
        holder.bookGrade.setText(book.getBookGrade());
        holder.bookTitle.setText(book.getBookTitle());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bookActivity != null)
                {
                    Intent intent = new Intent(bookActivity, ContentActivity.class);
                    bookActivity.startActivity(intent);
                }


            }
        });
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    public class BookViewHolder extends RecyclerView.ViewHolder {

        private TextView bookTitle, bookGrade;
        private ImageView bookImage;
        public BookViewHolder(@NonNull View itemView) {

            super(itemView);

            bookGrade = itemView.findViewById(R.id.book_grade);
            bookTitle = itemView.findViewById(R.id.book_title);
            bookImage = itemView.findViewById(R.id.bookImage);
        }
    }
}
