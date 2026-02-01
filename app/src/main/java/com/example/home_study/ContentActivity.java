package com.example.home_study;

import androidx.activity.EdgeToEdge;
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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_content);

        Intent intent = getIntent();
        bookTitle = intent.getStringExtra("bookTitle");
        if (bookTitle == null) bookTitle = "";

        contentRecycler = findViewById(R.id.contentRecycler);
        back = findViewById(R.id.backImage);
        back.setOnClickListener(v -> finish());

        contentRecycler.setLayoutManager(new LinearLayoutManager(this));
        contentList = new ArrayList<>();

        // populate static entries then try to enrich from remote curriculum/textbooks
        fetchCurriculumChapters(bookTitle);

        contentAdapter = new ContentAdapter(contentList, this::onContentSelected, this);
        contentRecycler.setAdapter(contentAdapter);

        Log.d("BookTitle", "Book: " + bookTitle);
    }

//    private void loadStaticChapters(String bookTitle) {
//        contentList.clear();
//
//        if (bookTitle.equalsIgnoreCase("English")) {
//            contentList.add(new Content("Content", "English", R.drawable.english, null));
//            contentList.add(new Content("Learning to learn", "UNIT 1", R.drawable.english, null));
//            contentList.add(new Content("Places to visit", "UNIT 2", R.drawable.english, null));
//            contentList.add(new Content("Hobbies and crafts", "UNIT 3", R.drawable.english, null));
//            contentList.add(new Content("Revision 1 (Units 1–3)", "REVISION", R.drawable.english, null));
//            contentList.add(new Content("Food for health", "UNIT 4", R.drawable.english, null));
//            contentList.add(new Content("HIV and AIDS", "UNIT 5", R.drawable.english, null));
//            contentList.add(new Content("Media, TV and Radio", "UNIT 6", R.drawable.english, null));
//            contentList.add(new Content("Revision 2 (Units 4–6)", "REVISION", R.drawable.english, null));
//            contentList.add(new Content("Cities of the future", "UNIT 7", R.drawable.english, null));
//            contentList.add(new Content("Money and finance", "UNIT 8", R.drawable.english, null));
//            contentList.add(new Content("People and traditional culture", "UNIT 9", R.drawable.english, null));
//            contentList.add(new Content("Revision 3 (Units 7–9)", "REVISION", R.drawable.english, null));
//            contentList.add(new Content("Newspapers and magazines", "UNIT 10", R.drawable.english, null));
//            contentList.add(new Content("Endangered animals", "UNIT 11", R.drawable.english, null));
//            contentList.add(new Content("Stigma and discrimination", "UNIT 12", R.drawable.english, null));
//            contentList.add(new Content("Revision 4 (Units 10–12)", "REVISION", R.drawable.english, null));
//
//        } else if (bookTitle.equalsIgnoreCase("Mathematics")) {
//            contentList.add(new Content("Biology and technology", " ", R.drawable.math, null));
//            contentList.add(new Content("Motion and Forces", " ", R.drawable.math, null));
//            contentList.add(new Content("Energy and Work", "  ", R.drawable.math, null));
//            contentList.add(new Content("Work", "History", R.drawable.math, null));
//
//        } else if (bookTitle.equalsIgnoreCase("Physics")) {
//            contentList.add(new Content("Introduction to Physics", "History", R.drawable.physics, null));
//            contentList.add(new Content("Motion and Forces", "History", R.drawable.physics, null));
//            contentList.add(new Content("Energy and Work", "History", R.drawable.physics, null));
//            contentList.add(new Content("Work", "History", R.drawable.physics, null));
//
//        } else if (bookTitle.equalsIgnoreCase("Biology")) {
//            contentList.add(new Content("Content", "Biology", R.drawable.biology, null));
//            contentList.add(new Content("Biology and technology", "UNIT 1", R.drawable.biology, null));
//            contentList.add(new Content("Cell biology", "UNIT 2", R.drawable.biology, null));
//            contentList.add(new Content("Human biology and health", "UNIT 3", R.drawable.biology, null));
//            contentList.add(new Content("Micro-organisms and disease", "UNIT 4", R.drawable.biology, null));
//            contentList.add(new Content("Classification", "UNIT 5", R.drawable.biology, null));
//            contentList.add(new Content("Environment", "UNIT 6", R.drawable.biology, null));
//
//        } else if (bookTitle.equalsIgnoreCase("Chemistry")) {
//            contentList.add(new Content("Content", "Chemistry", R.drawable.chemistry, null));
//            contentList.add(new Content("Structure of the Atom", "UNIT 1", R.drawable.chemistry, null));
//            contentList.add(new Content("Periodic Classification of the Elements", "UNIT 2", R.drawable.chemistry, null));
//            contentList.add(new Content("Chemical Bonding and Intermolecular Forces", "UNIT 3", R.drawable.chemistry, null));
//            contentList.add(new Content("Chemical Reaction and Stoichiometery", "UNIT 4", R.drawable.chemistry, null));
//            contentList.add(new Content("Physical States of Matter", "UNIT 5", R.drawable.chemistry, null));
//
//        } else if (bookTitle.equalsIgnoreCase("Geography")) {
//            contentList.add(new Content("Introduction to Geography", " ", R.drawable.geography, null));
//            contentList.add(new Content("Maps and Landscapes", " ", R.drawable.geography, null));
//            contentList.add(new Content("Weather and Climate", " ", R.drawable.geography, null));
//            contentList.add(new Content("Human Environments", " ", R.drawable.geography, null));
//
//        } else if (bookTitle.equalsIgnoreCase("History")) {
//            contentList.add(new Content("Introduction to History", " ", R.drawable.history, null));
//            contentList.add(new Content("Early Civilizations", "", R.drawable.history, null));
//            contentList.add(new Content("Medieval Times", " ", R.drawable.history, null));
//            contentList.add(new Content("Modern History", " ", R.drawable.history, null));
//        } else {
//            // default: leave list empty or keep as-is
//        }

        // Try to enrich the static list with Curriculum content; falls back to TextBooks if not found.
//        fetchCurriculumChapters(bookTitle);
//    }

//    /**
//     * Try loading chapters from Curriculum/grade_*/<bookKey>/chapters
//     * If bookKey not found, falls back to fetchChapters (TextBooks).
//            */
    private void fetchCurriculumChapters(String bookTitle) {
        if (bookTitle == null || bookTitle.trim().isEmpty()) {
            // fallback
            fetchChapters(bookTitle);
            return;
        }

        final String normalizedKey = normalizeKey(bookTitle);

        DatabaseReference curriculumRef = FirebaseDatabase.getInstance().getReference("Curriculum");
        curriculumRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                // fallback
                fetchChapters(bookTitle);
                return;
            }

            boolean found = false;
            for (DataSnapshot gradeSnap : snapshot.getChildren()) {
                if (gradeSnap == null) continue;
                // Look for a child whose key matches the normalized name
                if (gradeSnap.hasChild(normalizedKey)) {
                    DataSnapshot bookSnap = gradeSnap.child(normalizedKey);
                    DataSnapshot chapters = bookSnap.child("chapters");
                    if (chapters != null && chapters.exists()) {
                        found = true;
                        // iterate chapters
                        for (DataSnapshot ch : chapters.getChildren()) {
                            String unitId = ch.getKey();
                            String title = ch.child("title").getValue(String.class);
                            String pdfUrl = ch.child("contentUrl").getValue(String.class); // your db sample uses contentUrl
                            // try to match with existing contentList entries by title (case-insensitive)
                            boolean matched = false;
                            if (title != null) {
                                for (Content c : contentList) {
                                    if (c.getContentName() != null && c.getContentName().equalsIgnoreCase(title)) {
                                        c.setPdfUrl(pdfUrl);
                                        matched = true;
                                        break;
                                    }
                                }
                            }
                            if (!matched) {
                                // add new Content entry (use a generic drawable if you don't know which)
                                int drawable = chooseDrawableForBook(bookTitle);
                                String displayTitle = title != null ? title : unitId;
                                contentList.add(new Content(displayTitle, bookTitle, drawable, pdfUrl));
                            }
                        }
                        // notify adapter after processing
                        if (contentAdapter != null) contentAdapter.notifyDataSetChanged();
                    }
                    break; // found the book, stop searching other grades
                }
            }

            if (!found) {
                // fallback to your previous source
                fetchChapters(bookTitle);
            }
        }).addOnFailureListener(e -> {
            // fallback on error
            fetchChapters(bookTitle);
        });
    }

    // Helper to normalize book titles to keys used in your DB (very conservative)
    private String normalizeKey(String s) {
        if (s == null) return "";
        // lowercase, remove non-alphanumeric, replace spaces by underscore
        String t = s.trim().toLowerCase();
        t = t.replaceAll("[^a-z0-9]+", "_");
        // remove leading/trailing underscores
        t = t.replaceAll("^_+|_+$", "");
        return t;
    }

    // Choose a sensible drawable for a book title; extend mapping if needed
    private int chooseDrawableForBook(String book) {
        if (book == null) return R.drawable.math;
        String k = book.trim().toLowerCase();
        if (k.contains("english")) return R.drawable.english;
        if (k.contains("math") || k.contains("mathematics")) return R.drawable.math;
        if (k.contains("physics")) return R.drawable.physics;
        if (k.contains("biology")) return R.drawable.biology;
        if (k.contains("chemistry")) return R.drawable.chemistry;
        if (k.contains("geography")) return R.drawable.geography;
        if (k.contains("history")) return R.drawable.history;
        if (k.contains("ict")) return R.drawable.ict;
        if (k.contains("physical")) return R.drawable.hpe;
        if (k.contains("language") || k.contains("oromifa")) return R.drawable.language;
        // default
        return R.drawable.math;
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
                        if (unitTitle != null) {
                            for (Content content : contentList) {
                                if (content.getContentName() != null && content.getContentName().equalsIgnoreCase(unitTitle)) {
                                    content.setPdfUrl(pdfUrl); // Set PDF URL for the Content object
                                    found = true;
                                    break;
                                }
                            }
                        }

                        if (!found) {
                            // If no match, create a new Content and add it to the list
                            contentList.add(new Content(unitTitle, chapter, chooseDrawableForBook(chapter), pdfUrl));
                        }
                    }
                    if (contentAdapter != null) contentAdapter.notifyDataSetChanged(); // Notify adapter of data changes
                } else {
                    Log.e("Firebase", "No content found for chapter: " + chapter);
                    // don't show toast here — silent fallback may be better
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
        if (pdfUrl != null) {
            downloadAndOpenPdf(pdfUrl);
        } else {
            Toast.makeText(this, "PDF not available for this chapter", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadAndOpenPdf(String pdfUrl) {
        String fileName = pdfUrl.substring(pdfUrl.lastIndexOf("/") + 1);
        File pdfFile = new File(getCacheDir(), fileName);

        if (pdfFile.exists()) {
            openPdfViewActivity(pdfFile);
        } else {
            new Thread(() -> {
                try {
                    URL url = new URL(pdfUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = connection.getInputStream();
                        FileOutputStream outputStream = new FileOutputStream(pdfFile);

                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, len);
                        }

                        outputStream.close();
                        inputStream.close();

                        runOnUiThread(() -> openPdfViewActivity(pdfFile));
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Failed to download PDF", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    Log.e("PDF download", "error " + e.getMessage(), e);
                    runOnUiThread(() -> Toast.makeText(this, "Error downloading PDF", Toast.LENGTH_SHORT).show());
                }
            }).start();
        }
    }

    private void openPdfViewActivity(File pdfFile) {
        Intent intent = new Intent(ContentActivity.this, PdfViewerActivity.class);
        intent.putExtra("pdfFilePath", pdfFile.getAbsolutePath());
        startActivity(intent);
    }
}