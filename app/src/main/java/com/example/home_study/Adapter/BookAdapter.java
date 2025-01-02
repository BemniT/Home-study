package com.example.home_study.Adapter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.util.ArrayList;
import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.ViewHolder> {

    private List<Book> bookList;
    private Activity bookContext;

    public BookAdapter(List<Book> bookList, Activity bookContext) {
        this.bookList = bookList;
        this.bookContext = bookContext;
    }

    @NonNull
    @Override
    public BookAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.book_card,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookAdapter.ViewHolder holder, int position) {

        Book book = bookList.get(position);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        Bitmap bitmap = BitmapFactory.decodeResource(bookContext.getResources(), book.getBookImage(), options);

        holder.bookImage.setImageBitmap(bitmap);
        holder.bookTitle.setText(book.getBookTitle());
        holder.bookGrade.setText(book.getBookGrade());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(bookContext, ContentActivity.class);
                intent.putExtra("bookTitle",book.getBookTitle());
                intent.putExtra("bookGrade", book.getBookGrade());
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
