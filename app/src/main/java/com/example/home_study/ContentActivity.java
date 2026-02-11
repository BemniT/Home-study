package com.example.home_study;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.home_study.Adapter.ContentAdapter;
import com.example.home_study.Model.Content;
import com.example.home_study.db.ChapterEntity;
import com.example.home_study.repo.ContentRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ContentActivity backed by Room:
 * - Observes chapters from Room for subjectKey (local source)
 * - If Room empty on first load, triggers repository.syncTextBookChapters / syncSubjectsForGrade to populate Room
 * - Downloads pdf to cache on first open and updates ChapterEntity.localPath via repository.updateChapterLocalPath
 */
public class ContentActivity extends AppCompatActivity {

    private static final String TAG = "ContentActivity";
    private ImageView back;
    private RecyclerView contentRecycler;
    private ContentAdapter contentAdapter;
    private List<Content> contentList;
    private String subjectKey;   // DB key for subject (preferred)
    private String gradeKey;     // e.g., "grade_7" optionally passed
    private String bookTitle;    // fallback display title
    private ExecutorService downloadExecutor;
    private ContentRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_content);

        downloadExecutor = Executors.newSingleThreadExecutor();
        repository = new ContentRepository(getApplicationContext());

        Intent intent = getIntent();
        subjectKey = intent.getStringExtra("subjectKey"); // prefer this
        gradeKey = intent.getStringExtra("gradeKey");     // optional, e.g., "grade_7"
        bookTitle = intent.getStringExtra("bookTitle");   // fallback display title

        if (gradeKey == null || gradeKey.trim().isEmpty()) gradeKey = "grade_7";

        contentRecycler = findViewById(R.id.contentRecycler);
        back = findViewById(R.id.backImage);
        back.setOnClickListener(v -> finish());

        contentRecycler.setLayoutManager(new LinearLayoutManager(this));
        contentList = new ArrayList<>();
        contentAdapter = new ContentAdapter(contentList, this::onContentSelected, this);
        contentRecycler.setAdapter(contentAdapter);

        observeLocalChapters();
    }

    private void observeLocalChapters() {
        if (subjectKey != null && !subjectKey.trim().isEmpty()) {
            repository.getChaptersForSubjectLive(subjectKey).observe(this, new Observer<List<ChapterEntity>>() {
                private boolean initialLoad = true;
                @Override
                public void onChanged(List<ChapterEntity> chapters) {
                    // Map ChapterEntity -> Content
                    contentList.clear();
                    if (chapters != null && !chapters.isEmpty()) {
                        for (ChapterEntity ce : chapters) {
                            Content c = mapChapterEntityToContent(ce);
                            contentList.add(c);
                        }
                        contentAdapter.notifyDataSetChanged();
                    } else {
                        // No local chapters yet: on first load try to sync from remote
                        if (initialLoad) {
                            initialLoad = false;
                            // Try Curriculum sync (preferred) then TextBooks fallback
                            repository.syncSubjectsForGrade(gradeKey); // ensures subjects/chapters may be populated
                            // also try textBooks sync for this specific subjectKey if needed
                            repository.syncTextBookChapters(subjectKey, subjectKey);
                            // The remote sync writes to Room; when that completes LiveData will update and UI will refresh.
                        } else {
                            // subsequent empties -> just show adapter empty
                            contentAdapter.notifyDataSetChanged();
                        }
                    }
                }
            });
        } else {
            // No subjectKey passed: fall back to legacy behavior (bookTitle static or firebase)
            // Attempt to fetch TextBooks/bookTitle; repository has method to sync, then fallback to static list
            repository.syncTextBookChapters(bookTitle, bookTitle); // will write to Room under subjectKey==bookTitle
            // Observe using bookTitle as subjectKey (Room will have entries with subjectKey==bookTitle if sync succeeded)
            repository.getChaptersForSubjectLive(bookTitle).observe(this, chapters -> {
                contentList.clear();
                if (chapters != null && !chapters.isEmpty()) {
                    for (ChapterEntity ce : chapters) contentList.add(mapChapterEntityToContent(ce));
                } else {
                    // fallback: static hardcoded list (only if Room empty)
                    loadStaticChaptersFallback();
                }
                contentAdapter.notifyDataSetChanged();
            });
        }
    }

    // Convert DB entity to your Content model used by the adapter
    private Content mapChapterEntityToContent(ChapterEntity ce) {
        int drawable = com.example.home_study.ResourceUtils.chooseDrawableForBook(ce.subjectKey != null ? ce.subjectKey : bookTitle);
        Content c = new Content(
                ce.title != null && !ce.title.isEmpty() ? ce.title : ce.id,
                ce.subjectKey != null ? ce.subjectKey : bookTitle,
                drawable,
                ce.pdfUrl
        );
        c.setMetaId(ce.id);
        c.setOrderIndex(ce.orderIndex);
        c.setHasExam(ce.hasExam);
        // If we have a local path, prefer it when opening later
        if (ce.localPath != null && !ce.localPath.isEmpty()) {
            // You may want to store the local path into the Content model (add a field if needed)
            // but adapter currently checks cache by filename. We'll rely on repository to update entity when it's downloaded.
        }
        return c;
    }

    private void loadStaticChaptersFallback() {
        contentList.clear();
        if (bookTitle != null && bookTitle.equalsIgnoreCase("Mathematics")) {
            contentList.add(new Content("Basic Concept of Sets", "Mathematics", R.drawable.math, null));
            contentList.add(new Content("Integers", "Mathematics", R.drawable.math, null));
            contentList.add(new Content("Linear Equation", "Mathematics", R.drawable.math, null));
            contentList.add(new Content("Ratio, Proportion and Percentage", "Mathematics", R.drawable.math, null));
            contentList.add(new Content("Perimeter and Area of Plane Figure", "Mathematics", R.drawable.math, null));
            contentList.add(new Content("Congruency of Plane Figure", "Mathematics", R.drawable.math, null));
            contentList.add(new Content("Data Handling", "Mathematics", R.drawable.math, null));
        }
        contentAdapter.notifyDataSetChanged();
    }

    // Called from adapter when user taps the action icon
    public void handleActionForContent(Content content, int position) {
        if (content == null) return;
        String pdfUrl = content.getPdfUrl();
        if (pdfUrl == null || pdfUrl.isEmpty()) {
            Toast.makeText(this, "PDF not available for this chapter", Toast.LENGTH_SHORT).show();
            return;
        }
        String fileName = pdfUrl.substring(pdfUrl.lastIndexOf('/') + 1);
        File pdfFile = new File(getCacheDir(), fileName);
        if (pdfFile.exists()) {
            openPdfViewActivity(pdfFile);
            return;
        }
        Toast.makeText(this, "Downloading...", Toast.LENGTH_SHORT).show();

        // download in background and update Room with localPath when done
        downloadExecutor.submit(() -> {
            boolean ok = downloadToFile(pdfUrl, pdfFile);
            if (ok) {
                // update ChapterEntity.localPath in Room (so offline opens later)
                ChapterEntity updated = new ChapterEntity();
                updated.id = content.getMetaId() != null ? content.getMetaId() : ""; // ensure non-null
                updated.localPath = pdfFile.getAbsolutePath();
                // Only update the fields that Room expects: fetch existing then set path would be safer.
                // We'll fetch existing entity, set localPath, and call repository.updateChapterLocalPath.
                // For simplicity: query once (synchronously) then update (repository handles IO thread).
                repository.updateChapterLocalPathFromId(updated.id, pdfFile.getAbsolutePath());
                runOnUiThread(() -> {
                    // refresh adapter item so icon switches to open
                    if (contentAdapter != null) contentAdapter.notifyItemChanged(position);
                    openPdfViewActivity(pdfFile);
                });
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // Reuse your existing download helper (kept unchanged)
    private boolean downloadToFile(String pdfUrl, File outFile) {
        try {
            URL url = new URL(pdfUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                connection.disconnect();
                return false;
            }
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }
            connection.disconnect();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "download error: " + e.getMessage(), e);
            if (outFile.exists()) outFile.delete();
            return false;
        }
    }

    private void onContentSelected(Content content) {
        // same behavior as handleActionForContent: open from cache or download then open
        handleActionForContent(content, contentList.indexOf(content));
    }

    private void openPdfViewActivity(File pdfFile) {
        Intent intent = new Intent(ContentActivity.this, PdfViewerActivity.class);
        intent.putExtra("pdfFilePath", pdfFile.getAbsolutePath());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downloadExecutor != null) downloadExecutor.shutdownNow();
    }
}