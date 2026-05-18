package com.dshare.ui;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.dshare.R;

import java.io.File;

public class FullscreenImageActivity extends AppCompatActivity {

    private ImageView ivFullscreen;
    private ImageButton btnClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        ivFullscreen = findViewById(R.id.iv_fullscreen);
        btnClose = findViewById(R.id.btn_close);

        String filePath = getIntent().getStringExtra("file_path");
        
        if (filePath != null) {
            try {
                android.graphics.Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                if (bitmap != null) {
                    ivFullscreen.setImageBitmap(bitmap);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ivFullscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
