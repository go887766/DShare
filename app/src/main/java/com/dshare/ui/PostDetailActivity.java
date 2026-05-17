package com.dshare.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dshare.DShareApplication;
import com.dshare.R;
import com.dshare.model.Comment;
import com.dshare.model.Post;
import com.dshare.ui.adapters.CommentAdapter;

import java.util.List;
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
    private RecyclerView rvComments;
    private EditText etComment;
    private Button btnSubmitComment;
    private String postId;

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
        rvComments = findViewById(R.id.rv_comments);
        etComment = findViewById(R.id.et_comment);
        btnSubmitComment = findViewById(R.id.btn_submit_comment);

        loadPostDetails();
        loadComments();

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
            tvAuthor.setText("Author: " + post.getAuthorNickname() + " (" + post.getAuthorAddress().substring(0, 10) + "...)");
            tvContent.setText(post.getContent());
            tvTimestamp.setText("Posted: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(post.getTimestamp())));
            tvLikes.setText("Likes: " + post.getLikeCount());
            tvDislikes.setText("Dislikes: " + post.getDislikeCount());
            tvGoldReward.setText("Gold Reward: " + post.getGoldReward());
        }
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
            Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        DShareApplication app = DShareApplication.getInstance();
        if (app.getWallet() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Comment added!", Toast.LENGTH_SHORT).show();
            etComment.setText("");
            loadComments();
            loadPostDetails();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to add comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}