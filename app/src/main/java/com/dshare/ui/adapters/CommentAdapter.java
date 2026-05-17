package com.dshare.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.dshare.R;
import com.dshare.model.Comment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private Context context;
    private List<Comment> commentList;

    public CommentAdapter(Context context, List<Comment> commentList) {
        this.context = context;
        this.commentList = commentList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        String displayName = comment.getAuthorNickname() != null && !comment.getAuthorNickname().isEmpty()
                ? comment.getAuthorNickname()
                : comment.getAuthorAddress().substring(0, 8) + "...";
        holder.tvAuthor.setText(displayName);
        holder.tvContent.setText(comment.getContent());
        holder.tvTimestamp.setText(sdf.format(new Date(comment.getTimestamp())));
        holder.tvLikes.setText("Likes: " + comment.getLikeCount());
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvContent, tvTimestamp, tvLikes;

        ViewHolder(View view) {
            super(view);
            tvAuthor = view.findViewById(R.id.tv_comment_author);
            tvContent = view.findViewById(R.id.tv_comment_content);
            tvTimestamp = view.findViewById(R.id.tv_comment_timestamp);
            tvLikes = view.findViewById(R.id.tv_comment_likes);
        }
    }
}