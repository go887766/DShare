package com.dshare.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dshare.R;
import com.dshare.model.MediaContent;
import com.dshare.ui.FullscreenImageActivity;
import com.dshare.ui.FullscreenVideoActivity;

import java.io.File;
import java.util.List;

public class DetailMediaAdapter extends RecyclerView.Adapter<DetailMediaAdapter.ViewHolder> {

    private Context context;
    private List<MediaContent> mediaList;

    public DetailMediaAdapter(Context context, List<MediaContent> mediaList) {
        this.context = context;
        this.mediaList = mediaList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_media_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MediaContent media = mediaList.get(position);

        if (media.getType() == MediaContent.TYPE_IMAGE) {
            loadImage(holder.ivMedia, media.getFilePath());
            holder.ivPlayIcon.setVisibility(View.GONE);

            holder.ivMedia.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, FullscreenImageActivity.class);
                    intent.putExtra("file_path", media.getFilePath());
                    context.startActivity(intent);
                }
            });
        } else if (media.getType() == MediaContent.TYPE_VIDEO) {
            loadVideoThumbnail(holder.ivMedia, media.getFilePath());
            holder.ivPlayIcon.setVisibility(View.VISIBLE);

            holder.ivMedia.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, FullscreenVideoActivity.class);
                    intent.putExtra("file_path", media.getFilePath());
                    context.startActivity(intent);
                }
            });
        }
    }

    private void loadImage(ImageView imageView, String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    return;
                }
            } catch (Exception e) {
            }
        }
        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
    }

    private void loadVideoThumbnail(ImageView imageView, String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            try {
                Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(
                    filePath, MediaStore.Images.Thumbnails.MINI_KIND);
                if (thumbnail != null) {
                    imageView.setImageBitmap(thumbnail);
                    return;
                }
            } catch (Exception e) {
            }
        }
        imageView.setImageResource(android.R.drawable.ic_media_play);
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMedia;
        ImageView ivPlayIcon;

        ViewHolder(View view) {
            super(view);
            ivMedia = view.findViewById(R.id.iv_media);
            ivPlayIcon = view.findViewById(R.id.iv_play_icon);
        }
    }
}
