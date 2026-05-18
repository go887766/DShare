package com.dshare.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dshare.R;
import com.dshare.model.Post;
import com.dshare.ui.adapters.MediaAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {

    private Context context;
    private List<Post> postList;
    private OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(Post post);
        void onLikeClick(Post post);
        void onCommentClick(Post post);
    }

    public FeedAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    public void setOnPostClickListener(OnPostClickListener listener) {
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_feed, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Post post = postList.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        holder.tvAuthor.setText(post.getAuthorNickname() != null && !post.getAuthorNickname().isEmpty()
                ? post.getAuthorNickname().substring(0, Math.min(8, post.getAuthorNickname().length())) + "..."
                : post.getAuthorAddress());
        holder.tvContent.setText(post.getContent());
        holder.tvTimestamp.setText(sdf.format(new Date(post.getTimestamp())));
        holder.tvLikes.setText(context.getString(R.string.likes_colon_feed) + post.getLikeCount());
        holder.tvDislikes.setText(context.getString(R.string.dislikes_colon_feed) + post.getDislikeCount());
        holder.tvComments.setText(context.getString(R.string.comments_colon_feed) + post.getCommentCount());
        holder.tvGoldReward.setText(context.getString(R.string.gold_colon_feed) + post.getGoldReward());
        
        // Load media
        loadMedia(holder, post);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onPostClick(post);
            }
        });

        holder.btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onLikeClick(post);
            }
        });

        holder.btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onCommentClick(post);
            }
        });
    }
    
    private void loadMedia(ViewHolder holder, Post post) {
        holder.rvMedia.setVisibility(View.GONE);
        
        if (post.getMediaList() == null || post.getMediaList().isEmpty()) {
            return;
        }
        
        holder.rvMedia.setVisibility(View.VISIBLE);
        holder.rvMedia.setLayoutManager(new GridLayoutManager(context, 3));
        
        MediaAdapter mediaAdapter = new MediaAdapter(context, post.getMediaList());
        holder.rvMedia.setAdapter(mediaAdapter);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvContent, tvTimestamp, tvLikes, tvDislikes, tvComments, tvGoldReward;
        Button btnLike, btnComment;
        RecyclerView rvMedia;

        ViewHolder(View view) {
            super(view);
            tvAuthor = view.findViewById(R.id.tv_author);
            tvContent = view.findViewById(R.id.tv_content);
            tvTimestamp = view.findViewById(R.id.tv_timestamp);
            tvLikes = view.findViewById(R.id.tv_likes);
            tvDislikes = view.findViewById(R.id.tv_dislikes);
            tvComments = view.findViewById(R.id.tv_comments);
            tvGoldReward = view.findViewById(R.id.tv_gold_reward);
            btnLike = view.findViewById(R.id.btn_like);
            btnComment = view.findViewById(R.id.btn_comment);
            rvMedia = view.findViewById(R.id.rv_media);
        }
    }
}