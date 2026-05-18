package com.dshare.ui.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dshare.R;

import java.util.List;

public class PreviewMediaAdapter extends RecyclerView.Adapter<PreviewMediaAdapter.ViewHolder> {

    public interface OnRemoveClickListener {
        void onRemove(int position);
    }

    private Context context;
    private List<com.dshare.ui.CreatePostActivity.MediaItem> mediaList;
    private OnRemoveClickListener removeListener;

    public PreviewMediaAdapter(Context context, List<com.dshare.ui.CreatePostActivity.MediaItem> mediaList) {
        this.context = context;
        this.mediaList = mediaList;
    }

    public void setOnRemoveClickListener(OnRemoveClickListener listener) {
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_preview_media, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        com.dshare.ui.CreatePostActivity.MediaItem item = mediaList.get(position);
        
        if (item.type == com.dshare.model.MediaContent.TYPE_IMAGE) {
            holder.ivMedia.setImageURI(item.uri);
            holder.ivPlayIcon.setVisibility(View.GONE);
        } else {
            try {
                Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(
                    item.filePath, MediaStore.Images.Thumbnails.MINI_KIND);
                if (bitmap != null) {
                    holder.ivMedia.setImageBitmap(bitmap);
                } else {
                    holder.ivMedia.setImageResource(android.R.drawable.ic_media_play);
                }
            } catch (Exception e) {
                holder.ivMedia.setImageResource(android.R.drawable.ic_media_play);
            }
            holder.ivPlayIcon.setVisibility(View.VISIBLE);
        }
        
        holder.btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (removeListener != null) {
                    removeListener.onRemove(holder.getAdapterPosition());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMedia;
        ImageView ivPlayIcon;
        ImageButton btnRemove;

        ViewHolder(View view) {
            super(view);
            ivMedia = view.findViewById(R.id.iv_media);
            ivPlayIcon = view.findViewById(R.id.iv_play_icon);
            btnRemove = view.findViewById(R.id.btn_remove);
        }
    }
}
