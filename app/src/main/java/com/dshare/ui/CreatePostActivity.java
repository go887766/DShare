package com.dshare.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dshare.DShareApplication;
import com.dshare.R;
import com.dshare.crypto.CryptoManager;
import com.dshare.model.MediaContent;
import com.dshare.model.Post;

import java.util.UUID;

public class CreatePostActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_VIDEO_REQUEST = 2;

    private EditText etContent;
    private Button btnPickImage;
    private Button btnPickVideo;
    private ImageView ivPreview;
    private Button btnSubmit;
    private Uri selectedMediaUri;
    private int selectedMediaType = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        etContent = findViewById(R.id.et_content);
        btnPickImage = findViewById(R.id.btn_pick_image);
        btnPickVideo = findViewById(R.id.btn_pick_video);
        ivPreview = findViewById(R.id.iv_preview);
        btnSubmit = findViewById(R.id.btn_submit);

        btnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            }
        });

        btnPickVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_VIDEO_REQUEST);
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitPost();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedMediaUri = data.getData();
            if (requestCode == PICK_IMAGE_REQUEST) {
                selectedMediaType = MediaContent.TYPE_IMAGE;
                ivPreview.setImageURI(selectedMediaUri);
                ivPreview.setVisibility(View.VISIBLE);
            } else if (requestCode == PICK_VIDEO_REQUEST) {
                selectedMediaType = MediaContent.TYPE_VIDEO;
                ivPreview.setVisibility(View.GONE);
                Toast.makeText(this, "Video selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void submitPost() {
        String content = etContent.getText().toString().trim();
        if (TextUtils.isEmpty(content) && selectedMediaUri == null) {
            Toast.makeText(this, "Please enter content or select media", Toast.LENGTH_SHORT).show();
            return;
        }

        DShareApplication app = DShareApplication.getInstance();
        if (app.getWallet() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            Post post = new Post();
            post.setPostId(UUID.randomUUID().toString());
            post.setAuthorAddress(app.getWallet().getAddress());
            post.setContent(content);
            post.setTimestamp(System.currentTimeMillis());

            if (selectedMediaUri != null && selectedMediaType > 0) {
                String filePath = getFilePathFromUri(selectedMediaUri);
                if (filePath != null) {
                    MediaContent media = new MediaContent(
                        UUID.randomUUID().toString(),
                        CryptoManager.generateContentHash(filePath.getBytes()),
                        selectedMediaType
                    );
                    media.setFilePath(filePath);
                    post.addMedia(media);
                }
            }

            String dataToSign = post.getPostId() + post.getContent() + post.getTimestamp();
            post.setSignature(app.getWallet().sign(dataToSign));

            app.getDatabase().savePost(post);

            // Broadcast post to P2P network
            if (app.getP2PNode() != null && app.getP2PNode().isRunning()) {
                com.dshare.network.MessageProtocol msg = new com.dshare.network.MessageProtocol(
                    com.dshare.network.MessageProtocol.TYPE_POST,
                    app.getWallet().getAddress(),
                    app.getDatabase().getUser(app.getWallet().getAddress()).getNickname()
                );
                app.getP2PNode().broadcastMessage(msg);
            }

            Toast.makeText(this, "Post published successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to publish: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getFilePathFromUri(Uri uri) {
        String filePath = null;
        if (uri != null) {
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    filePath = cursor.getString(columnIndex);
                }
                cursor.close();
            }
        }
        return filePath;
    }
}