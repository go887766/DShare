package com.dshare.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dshare.DShareApplication;
import com.dshare.R;
import com.dshare.model.Post;
import com.dshare.ui.adapters.FeedAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainFeedActivity extends AppCompatActivity {

    private RecyclerView rvFeed;
    private FeedAdapter feedAdapter;
    private List<Post> postList;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_feed);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("DShare - Global Feed");

        rvFeed = findViewById(R.id.rv_feed);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);

        postList = new ArrayList<>();
        feedAdapter = new FeedAdapter(this, postList);
        rvFeed.setLayoutManager(new LinearLayoutManager(this));
        rvFeed.setAdapter(feedAdapter);

        feedAdapter.setOnPostClickListener(new FeedAdapter.OnPostClickListener() {
            @Override
            public void onPostClick(Post post) {
                Intent intent = new Intent(MainFeedActivity.this, PostDetailActivity.class);
                intent.putExtra("post_id", post.getPostId());
                startActivity(intent);
            }

            @Override
            public void onLikeClick(Post post) {
                handleLike(post);
            }

            @Override
            public void onCommentClick(Post post) {
                Intent intent = new Intent(MainFeedActivity.this, PostDetailActivity.class);
                intent.putExtra("post_id", post.getPostId());
                startActivity(intent);
            }
        });

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadPosts();
            }
        });

        loadPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPosts();
    }

    private void loadPosts() {
        progressBar.setVisibility(View.VISIBLE);
        DShareApplication app = DShareApplication.getInstance();
        try {
            List<Post> dbPosts = app.getDatabase().getAllPosts();
            postList.clear();
            postList.addAll(dbPosts);
            feedAdapter.notifyDataSetChanged();
            if (postList.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
            } else {
                tvEmpty.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading posts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }

    private void handleLike(Post post) {
        DShareApplication app = DShareApplication.getInstance();
        String currentAddress = app.getCurrentUserAddress();
        if (currentAddress == null) return;

        if (post.getLikedBy().contains(currentAddress)) {
            post.getLikedBy().remove(currentAddress);
            post.setLikeCount(post.getLikeCount() - 1);
        } else {
            if (post.getDislikedBy().contains(currentAddress)) {
                post.getDislikedBy().remove(currentAddress);
                post.setDislikeCount(post.getDislikeCount() - 1);
            }
            post.getLikedBy().add(currentAddress);
            post.setLikeCount(post.getLikeCount() + 1);
        }
        app.getDatabase().savePost(post);
        feedAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_create_post) {
            startActivity(new Intent(this, CreatePostActivity.class));
            return true;
        } else if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_wallet) {
            startActivity(new Intent(this, WalletActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            DShareApplication.getInstance().logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}