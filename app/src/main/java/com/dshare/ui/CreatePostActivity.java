package com.dshare.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dshare.DShareApplication;
import com.dshare.R;
import com.dshare.crypto.CryptoManager;
import com.dshare.model.MediaContent;
import com.dshare.model.Post;
import com.dshare.storage.ContentManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreatePostActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_VIDEO_REQUEST = 2;

    private static final int MAX_IMAGES = 6;
    private static final int MAX_VIDEOS = 2;

    private EditText etContent;
    private Button btnPickImage;
    private Button btnPickVideo;
    private androidx.recyclerview.widget.RecyclerView rvMediaPreview;
    private android.widget.Button btnSubmit;
    private PreviewMediaAdapter previewAdapter;
    private TextView tvMediaHint;

    private List<MediaItem> selectedMediaList = new ArrayList<>();
    private ContentManager contentManager;

    private static class MediaItem {
        Uri uri;
        String filePath;
        int type;

        MediaItem(Uri uri, String filePath, int type) {
            this.uri = uri;
            this.filePath = filePath;
            this.type = type;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);
        
        contentManager = DShareApplication.getInstance().getContentManager();
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.create_post_title);
        }

        etContent = findViewById(R.id.et_content);
        btnPickImage = findViewById(R.id.btn_pick_image);
        btnPickVideo = findViewById(R.id.btn_pick_video);
        rvMediaPreview = findViewById(R.id.rv_media_preview);
        btnSubmit = findViewById(R.id.btn_submit);
        
        rvMediaPreview.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 3));
        previewAdapter = new PreviewMediaAdapter(this, selectedMediaList);
        rvMediaPreview.setAdapter(previewAdapter);
        previewAdapter.setOnRemoveClickListener(new PreviewMediaAdapter.OnRemoveClickListener() {
            @Override
            public void onRemove(int position) {
                selectedMediaList.remove(position);
                previewAdapter.notifyDataSetChanged();
            }
        });

        btnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getImageCount() >= MAX_IMAGES) {
                    Toast.makeText(CreatePostActivity.this, R.string.image_limit_reached, Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            }
        });

        btnPickVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getVideoCount() >= MAX_VIDEOS) {
                    Toast.makeText(CreatePostActivity.this, R.string.video_limit_reached, Toast.LENGTH_SHORT).show();
                    return;
                }
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

    private int getImageCount() {
        int count = 0;
        for (MediaItem item : selectedMediaList) {
            if (item.type == MediaContent.TYPE_IMAGE) {
                count++;
            }
        }
        return count;
    }

    private int getVideoCount() {
        int count = 0;
        for (MediaItem item : selectedMediaList) {
            if (item.type == MediaContent.TYPE_VIDEO) {
                count++;
            }
        }
        return count;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            String filePath = getFilePathFromUri(uri);
            
            if (filePath != null) {
                MediaItem mediaItem = new MediaItem(uri, filePath, 
                    requestCode == PICK_IMAGE_REQUEST ? MediaContent.TYPE_IMAGE : MediaContent.TYPE_VIDEO);
                
                selectedMediaList.add(mediaItem);
                refreshMediaPreview();
            }
        }
    }

    private void refreshMediaPreview() {
        if (selectedMediaList.isEmpty()) {
            rvMediaPreview.setVisibility(View.GONE);
            return;
        }
        rvMediaPreview.setVisibility(View.VISIBLE);
        previewAdapter.notifyDataSetChanged();
    }
    
    private void submitPost() {
        String content = etContent.getText().toString().trim();
        if (TextUtils.isEmpty(content) && selectedMediaList.isEmpty()) {
            Toast.makeText(this, R.string.enter_content_or_media, Toast.LENGTH_SHORT).show();
            return;
        }

        DShareApplication app = DShareApplication.getInstance();
        if (app.getWallet() == null) {
            Toast.makeText(this, R.string.please_login_first, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            Post post = new Post();
            post.setPostId(UUID.randomUUID().toString());
            post.setAuthorAddress(app.getWallet().getAddress());
            
            if (app.getDatabase().getUser(app.getWallet().getAddress()) != null) {
                post.setAuthorNickname(app.getDatabase().getUser(app.getWallet().getAddress()).getNickname());
            }
            
            post.setContent(content);
            post.setTimestamp(System.currentTimeMillis());

            for (MediaItem mediaItem : selectedMediaList) {
                MediaContent media = saveMedia(mediaItem);
                if (media != null) {
                    post.addMedia(media);
                }
            }

            String dataToSign = post.getPostId() + post.getContent() + post.getTimestamp();
            post.setSignature(app.getWallet().sign(dataToSign));

            app.getDatabase().savePost(post);

            if (app.getP2PNode() != null && app.getP2PNode().isRunning()) {
                com.dshare.network.MessageProtocol msg = new com.dshare.network.MessageProtocol(
                    com.dshare.network.MessageProtocol.TYPE_POST,
                    app.getWallet().getAddress(),
                    post.getAuthorNickname()
                );
                app.getP2PNode().broadcastMessage(msg);
            }

            Toast.makeText(this, R.string.post_published_success, Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.publish_failed) + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private MediaContent saveMedia(MediaItem mediaItem) {
        try {
            if (mediaItem.type == MediaContent.TYPE_IMAGE) {
                Bitmap bitmap = BitmapFactory.decodeFile(mediaItem.filePath);
                if (bitmap != null) {
                    MediaContent media = contentManager.saveImage(bitmap);
                    if (media != null) {
                        return media;
                    }
                }
            } else if (mediaItem.type == MediaContent.TYPE_VIDEO) {
                InputStream videoStream = getContentResolver().openInputStream(mediaItem.uri);
                if (videoStream != null) {
                    MediaContent media = contentManager.saveVideo(videoStream);
                    videoStream.close();
                    if (media != null) {
                        return media;
                    }
                }
            }
            
            MediaContent fallbackMedia = new MediaContent(
                UUID.randomUUID().toString(),
                CryptoManager.generateContentHash(mediaItem.filePath.getBytes()),
                mediaItem.type
            );
            fallbackMedia.setFilePath(mediaItem.filePath);
            return fallbackMedia;
        } catch (Exception e) {
            MediaContent fallbackMedia = new MediaContent(
                UUID.randomUUID().toString(),
                CryptoManager.generateContentHash(mediaItem.filePath.getBytes()),
                mediaItem.type
            );
            fallbackMedia.setFilePath(mediaItem.filePath);
            return fallbackMedia;
        }
    }

    private String getFilePathFromUri(Uri uri) {
        String filePath = null;
        if (uri != null) {
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        filePath = cursor.getString(columnIndex);
                    }
                } catch (Exception e) {
                    cursor.close();
                    
                    projection = new String[]{MediaStore.Video.Media.DATA};
                    cursor = getContentResolver().query(uri, projection, null, null, null);
                    
                    if (cursor != null && cursor.moveToFirst()) {
                        try {
                            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                            filePath = cursor.getString(columnIndex);
                        } catch (Exception e2) {
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
        return filePath;
    }
}
