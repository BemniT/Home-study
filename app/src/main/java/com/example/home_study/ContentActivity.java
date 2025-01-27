package com.example.home_study;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.home_study.Adapter.ContentAdapter;
import com.example.home_study.Model.Content;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ContentActivity extends AppCompatActivity {

    private Content content;
    private ImageView back;
    private RecyclerView contentRecycler;
    private ContentAdapter contentAdapter;
    private List<Content> contentList;
    private String bookTitle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        Intent intent = getIntent();
        bookTitle = intent.getStringExtra("bookTitle").toString();
        contentRecycler = findViewById(R.id.contentRecycler);
        back = findViewById(R.id.backImage);
        back.setOnClickListener(v -> finish());
        contentRecycler.setLayoutManager(new LinearLayoutManager(this));

        contentRecycler = findViewById(R.id.contentRecycler);
        contentList = new ArrayList<>();
        loadStaticChapters(bookTitle);
        contentAdapter = new ContentAdapter(contentList, this::onContentSelected, this);

        contentRecycler.setAdapter(contentAdapter);

        Log.d("BookTitle", "Book: "+ bookTitle);

    }

    private void loadStaticChapters(String bookTitle)
    {
        contentList.clear();

        if (bookTitle.equals("Biology")) {
            contentList.add(new Content("Introduction to Biology", "Biology", R.drawable.biology, null));
            contentList.add(new Content("Cell Structure and Function", "Biology", R.drawable.biology, null));
            contentList.add(new Content("Genetics", "Biology", R.drawable.biology, null));
        } else if (bookTitle.equalsIgnoreCase("Physics")) {
            contentList.add(new Content("Introduction to Physics", "Physics", R.drawable.physics, null));
            contentList.add(new Content("Motion and Forces", "Physics", R.drawable.physics, null));
            contentList.add(new Content("Energy and Work", "Physics", R.drawable.physics, null));
        } else {
//            contentList.add(new Content("Default Chapter 1", bookTitle, R.drawable.default_image, null));
//            contentList.add(new Content("Default Chapter 2", bookTitle, R.drawable.default_image, null));
        }

        fetchChapters(bookTitle);

    }
    private void fetchChapters(String chapter) {
        DatabaseReference contentRe = FirebaseDatabase.getInstance().getReference()
                .child("TextBooks")
                .child(chapter)
                .child("Content");

        contentRe.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        String unitTitle = dataSnapshot.getKey(); // Chapter title
                        String pdfUrl = dataSnapshot.child("pdfUrl").getValue(String.class); // PDF URL
                        Log.e("Unit", "Title: " + unitTitle + " PDF URL: " + pdfUrl);

                        // Match unitTitle with contentList
                        boolean found = false;
                        for (Content content : contentList) {
                            if (unitTitle != null && content.getContentName().equalsIgnoreCase(unitTitle)) {
                                content.setPdfUrl(pdfUrl); // Set PDF URL for the Content object
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            // If no match, create a new Content and add it to the list
                            contentList.add(new Content(unitTitle, chapter, R.drawable.math, pdfUrl));
                        }
                    }
                    contentAdapter.notifyDataSetChanged(); // Notify adapter of data changes
                } else {
                    Log.e("Firebase", "No content found for chapter: " + chapter);
                    Toast.makeText(ContentActivity.this, "No chapters available for " + chapter, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ContentActivity.this, "Failed to load content: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void onContentSelected(Content content) {
        String pdfUrl = content.getPdfUrl();
        if (pdfUrl != null){
            downloadAndOpenPdf(pdfUrl);
        }else {
            Toast.makeText(this, "PDF not available for this chapter", Toast.LENGTH_SHORT).show();
        }

    }

    private void downloadAndOpenPdf(String pdfUrl)
    {
        String fileName = pdfUrl.substring(pdfUrl.lastIndexOf("/") + 1);
        File pdfFile = new File(getCacheDir(), fileName);

        if (pdfFile.exists())
        {
            openPdfViewActivity(pdfFile);
        }else {
            new Thread(()->{

                try {
                    URL url = new URL(pdfUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                    {
                        InputStream inputStream = connection.getInputStream();

//                        File cacheDir = getCacheDir();
//                        File pdfFile = new File(cacheDir, "temp.pdf");
                        FileOutputStream outputStream =  new FileOutputStream(pdfFile);

                        byte[] buffer = new byte[1024];
                        int len;
                        while((len = inputStream.read(buffer)) != -1)
                        {
                            outputStream.write(buffer, 0, len);
                        }

                        outputStream.close();
                        inputStream.close();

                        runOnUiThread(()->{

                            openPdfViewActivity(pdfFile);
                        });
                    }else {
                        runOnUiThread(()->{
                            Toast.makeText(this, "Failed to download PDF", Toast.LENGTH_SHORT).show();
                        });
                    }
                }catch (Exception e){

                    Log.e("PDF download","error"+e.getMessage());
                    runOnUiThread(()->{
                        Toast.makeText(this, "Error downloading PDF", Toast.LENGTH_SHORT).show();
                    });

                }

            }).start();
        }

    }

    private void openPdfViewActivity(File pdfFile) {
        Intent intent  = new Intent(ContentActivity.this, PdfViewerActivity.class);
        intent.putExtra("pdfFilePath", pdfFile.getAbsolutePath());
        startActivity(intent);
    }

}