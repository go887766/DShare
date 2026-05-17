package com.dshare.storage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.dshare.crypto.CryptoManager;
import com.dshare.model.MediaContent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class ContentManager {

    private static final String TAG = "ContentManager";
    private Context context;
    private File contentDir;

    public ContentManager(Context context) {
        this.context = context;
        this.contentDir = new File(context.getFilesDir(), "shared_content");
        if (!contentDir.exists()) {
            contentDir.mkdirs();
        }
    }

    public MediaContent saveImage(Bitmap bitmap) {
        try {
            String mediaId = UUID.randomUUID().toString();
            File imageFile = new File(contentDir, mediaId + ".jpg");
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();

            String contentHash = CryptoManager.generateContentHash(
                    readFileBytes(imageFile));

            MediaContent media = new MediaContent(mediaId, contentHash, MediaContent.TYPE_IMAGE);
            media.setFilePath(imageFile.getAbsolutePath());
            media.setFileSize(imageFile.length());
            media.setMimeType("image/jpeg");
            return media;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save image", e);
            return null;
        }
    }

    public MediaContent saveVideo(InputStream videoStream) {
        try {
            String mediaId = UUID.randomUUID().toString();
            File videoFile = new File(contentDir, mediaId + ".mp4");
            FileOutputStream fos = new FileOutputStream(videoFile);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = videoStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.flush();
            fos.close();

            String contentHash = CryptoManager.generateContentHash(
                    readFileBytes(videoFile));

            MediaContent media = new MediaContent(mediaId, contentHash, MediaContent.TYPE_VIDEO);
            media.setFilePath(videoFile.getAbsolutePath());
            media.setFileSize(videoFile.length());
            media.setMimeType("video/mp4");
            return media;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save video", e);
            return null;
        }
    }

    public File getContentFile(MediaContent media) {
        File file = new File(media.getFilePath());
        if (file.exists()) {
            return file;
        }
        return null;
    }

    public Bitmap loadImage(MediaContent media) {
        File file = getContentFile(media);
        if (file != null) {
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        return null;
    }

    public boolean deleteContent(MediaContent media) {
        File file = new File(media.getFilePath());
        return file.delete();
    }

    public long getTotalContentSize() {
        long size = 0;
        File[] files = contentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                size += file.length();
            }
        }
        return size;
    }

    public File getContentDir() {
        return contentDir;
    }

    private byte[] readFileBytes(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            fis.close();
            return baos.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "Failed to read file", e);
            return new byte[0];
        }
    }

    public void cleanupOldContent(long maxAgeMs) {
        long now = System.currentTimeMillis();
        File[] files = contentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (now - file.lastModified() > maxAgeMs) {
                    file.delete();
                }
            }
        }
    }
}