package com.dshare.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dshare.DShareApplication;
import com.dshare.R;
import com.dshare.model.Comment;
import com.dshare.model.MediaContent;
import com.dshare.model.Post;
import com.dshare.ui.adapters.CommentAdapter;
import com.dshare.ui.adapters.DetailMediaAdapter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PostDetailActivity extends AppCompatActivity {

    private TextView tvAuthor;
    private TextView tvContent;
    private TextView tvTimestamp;
    private TextView tvLikes;
    private TextView tvDislikes;
    private TextView tvGoldReward;
    private Button btnLike;
    private Button btnDislike;
    private Button btnBack;
    private ImageButton btnCopyQq;
    private ImageButton btnCopyWechat;
    private ImageButton btnCopyPhone;
    private RecyclerView rvComments;
    private EditText etComment;
    private Button btnSubmitComment;
    private RecyclerView rvMediaDetail;
    private String postId;
    private String authorQq;
    private String authorWechat;
    private String authorPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        postId = getIntent().getStringExtra("post_id");

        tvAuthor = findViewById(R.id.tv_author);
        tvContent = findViewById(R.id.tv_content_detail);
        tvTimestamp = findViewById(R.id.tv_timestamp);
        tvLikes = findViewById(R.id.tv_likes);
        tvDislikes = findViewById(R.id.tv_dislikes);
        tvGoldReward = findViewById(R.id.tv_gold_reward);
        btnLike = findViewById(R.id.btn_like);
        btnDislike = findViewById(R.id.btn_dislike);
        btnBack = findViewById(R.id.btn_back);
        btnCopyQq = findViewById(R.id.btn_copy_qq);
        btnCopyWechat = findViewById(R.id.btn_copy_wechat);
        btnCopyPhone = findViewById(R.id.btn_copy_phone);
        rvComments = findViewById(R.id.rv_comments);
        etComment = findViewById(R.id.et_comment);
        btnSubmitComment = findViewById(R.id.btn_submit_comment);
        rvMediaDetail = findViewById(R.id.rv_media_detail);

        loadPostDetails();
        loadComments();

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLike();
            }
        });

        btnDislike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleDislike();
            }
        });

        btnCopyQq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard("QQ", authorQq);
            }
        });

        btnCopyWechat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard("微信", authorWechat);
            }
        });

        btnCopyPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard("电话", authorPhone);
            }
        });

        btnSubmitComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitComment();
            }
        });
    }

    private void loadPostDetails() {
        DShareApplication app = DShareApplication.getInstance();
        Post post = app.getDatabase().getPost(postId);
        if (post != null) {
            String authorDisplay = post.getAuthorNickname();
            if (authorDisplay == null || authorDisplay.isEmpty()) {
                authorDisplay = post.getAuthorAddress().substring(0, 10) + "...";
            }
            tvAuthor.setText(getString(R.string.author_prefix) + authorDisplay);
            tvContent.setText(post.getContent());
            tvTimestamp.setText(getString(R.string.posted_prefix) + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new java.util.Date(post.getTimestamp())));
            tvLikes.setText(getString(R.string.likes_prefix) + post.getLikeCount());
            tvDislikes.setText(getString(R.string.dislikes_prefix) + post.getDislikeCount());
            tvGoldReward.setText(getString(R.string.gold_reward_prefix) + post.getGoldReward());
            
            // 加载作者信息
            com.dshare.model.User author = app.getDatabase().getUser(post.getAuthorAddress());
            if (author != null) {
                authorQq = author.getQq();
                authorWechat = author.getWechat();
                authorPhone = author.getPhone();
                
                // 显示或隐藏复制按钮
                if (authorQq != null && !authorQq.isEmpty()) {
                    btnCopyQq.setVisibility(View.VISIBLE);
                } else {
                    btnCopyQq.setVisibility(View.GONE);
                }
                
                if (authorWechat != null && !authorWechat.isEmpty()) {
                    btnCopyWechat.setVisibility(View.VISIBLE);
                } else {
                    btnCopyWechat.setVisibility(View.GONE);
                }
                
                if (authorPhone != null && !authorPhone.isEmpty()) {
                    btnCopyPhone.setVisibility(View.VISIBLE);
                } else {
                    btnCopyPhone.setVisibility(View.GONE);
                }
            }
            
            loadMedia(post);
        }
    }
    
    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, label + "已复制", Toast.LENGTH_SHORT).show();
    }

    private void loadMedia(Post post) {
        rvMediaDetail.setVisibility(View.GONE);
        
        if (post.getMediaList() == null || post.getMediaList().isEmpty()) {
            return;
        }
        
        rvMediaDetail.setVisibility(View.VISIBLE);
        rvMediaDetail.setLayoutManager(new LinearLayoutManager(this));
        
        DetailMediaAdapter mediaAdapter = new DetailMediaAdapter(this, post.getMediaList());
        rvMediaDetail.setAdapter(mediaAdapter);
    }

    private void loadComments() {
        DShareApplication app = DShareApplication.getInstance();
        List<Comment> comments = app.getDatabase().getCommentsForPost(postId);
        CommentAdapter adapter = new CommentAdapter(this, comments);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(adapter);
    }

    private void handleLike() {
        DShareApplication app = DShareApplication.getInstance();
        Post post = app.getDatabase().getPost(postId);
        if (post != null && app.getCurrentUserAddress() != null) {
            String addr = app.getCurrentUserAddress();
            if (post.getLikedBy().contains(addr)) {
                post.getLikedBy().remove(addr);
                post.setLikeCount(post.getLikeCount() - 1);
            } else {
                if (post.getDislikedBy().contains(addr)) {
                    post.getDislikedBy().remove(addr);
                    post.setDislikeCount(post.getDislikeCount() - 1);
                }
                post.getLikedBy().add(addr);
                post.setLikeCount(post.getLikeCount() + 1);
            }
            app.getDatabase().savePost(post);
            loadPostDetails();
        }
    }

    private void handleDislike() {
        DShareApplication app = DShareApplication.getInstance();
        Post post = app.getDatabase().getPost(postId);
        if (post != null && app.getCurrentUserAddress() != null) {
            String addr = app.getCurrentUserAddress();
            if (post.getDislikedBy().contains(addr)) {
                post.getDislikedBy().remove(addr);
                post.setDislikeCount(post.getDislikeCount() - 1);
            } else {
                if (post.getLikedBy().contains(addr)) {
                    post.getLikedBy().remove(addr);
                    post.setLikeCount(post.getLikeCount() - 1);
                }
                post.getDislikedBy().add(addr);
                post.setDislikeCount(post.getDislikeCount() + 1);
            }
            app.getDatabase().savePost(post);
            loadPostDetails();
        }
    }

    private void submitComment() {
        String commentText = etComment.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, R.string.enter_comment, Toast.LENGTH_SHORT).show();
            return;
        }

        DShareApplication app = DShareApplication.getInstance();
        if (app.getWallet() == null) {
            Toast.makeText(this, R.string.please_login_first, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Comment comment = new Comment(
                UUID.randomUUID().toString(),
                postId,
                app.getWallet().getAddress(),
                commentText
            );
            comment.setAuthorNickname(app.getDatabase().getUser(app.getWallet().getAddress()).getNickname());

            String dataToSign = comment.getCommentId() + comment.getContent() + comment.getTimestamp();
            comment.setSignature(app.getWallet().sign(dataToSign));

            app.getDatabase().saveComment(comment);
            Toast.makeText(this, R.string.comment_added, Toast.LENGTH_SHORT).show();
            etComment.setText("");
            loadComments();
            loadPostDetails();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.add_comment_failed) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
