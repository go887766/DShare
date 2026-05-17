package com.dshare.model;

import java.io.Serializable;

public class MediaContent implements Serializable {
    private String mediaId;
    private String contentHash;
    private int mediaType;
    private String filePath;
    private long fileSize;
    private String mimeType;

    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VIDEO = 2;
    public static final int TYPE_TEXT = 3;

    public MediaContent() {}

    public MediaContent(String mediaId, String contentHash, int mediaType) {
        this.mediaId = mediaId;
        this.contentHash = contentHash;
        this.mediaType = mediaType;
    }

    public String getMediaId() { return mediaId; }
    public void setMediaId(String mediaId) { this.mediaId = mediaId; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public int getMediaType() { return mediaType; }
    public void setMediaType(int mediaType) { this.mediaType = mediaType; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
}