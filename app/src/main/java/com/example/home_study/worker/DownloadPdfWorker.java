package com.example.home_study.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.home_study.db.AppDatabase;
import com.example.home_study.db.ChapterEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadPdfWorker extends Worker {

    public static final String KEY_PDF_URL = "pdf_url";
    public static final String KEY_CHAPTER_ID = "chapter_id";

    public DownloadPdfWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String pdfUrl = getInputData().getString(KEY_PDF_URL);
        String chapterId = getInputData().getString(KEY_CHAPTER_ID);
        if (pdfUrl == null || chapterId == null) return Result.failure();

        try {
            URL url = new URL(pdfUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                conn.disconnect();
                return Result.retry();
            }

            InputStream in = conn.getInputStream();
            String fileName = pdfUrl.substring(pdfUrl.lastIndexOf('/') + 1);
            File out = new File(getApplicationContext().getCacheDir(), fileName);
            try (FileOutputStream fos = new FileOutputStream(out)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) fos.write(buf, 0, len);
            }
            conn.disconnect();

            // update Room
            AppDatabase db = AppDatabase.get(getApplicationContext());
            ChapterEntity ch = db.dao().getChapterOnce(chapterId);
            if (ch != null) {
                ch.localPath = out.getAbsolutePath();
                db.dao().updateChapter(ch);
            }

            Data output = new Data.Builder().putString("local_path", out.getAbsolutePath()).build();
            return Result.success(output);

        } catch (Exception e) {
            Log.e("DownloadPdfWorker", "err", e);
            return Result.retry();
        }
    }
}