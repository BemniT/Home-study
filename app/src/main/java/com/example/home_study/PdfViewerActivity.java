package com.example.home_study;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.util.FitPolicy;

import java.io.File;

public class PdfViewerActivity extends AppCompatActivity {

    private PDFView pdfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        pdfView = findViewById(R.id.pdfView);

        String pdfUrl = getIntent().getStringExtra("pdfFilePath");

        if (pdfUrl != null){
            File filePath = new File(pdfUrl);
            if (filePath.exists()){
                pdfView.fromFile(filePath)
                        .enableSwipe(true)
                        .defaultPage(0)
                        .enableDoubletap(true)
                        .swipeHorizontal(false)
                        .spacing(10)
                        .scrollHandle(new DefaultScrollHandle(this))
                        .pageFitPolicy(FitPolicy.WIDTH)
                        .load();
            }
        }
    }
}