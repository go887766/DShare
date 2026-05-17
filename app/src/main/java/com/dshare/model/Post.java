package com.dshare.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Post implements Serializable {
    private String postId;
    private String authorAddress;
    private String authorNickname;
    private String content;
    private List<MediaContent> mediaList;
    private long timestamp;
    private int likeCount;
    private int dislikeCount;
    private int commentCount;
    private int goldReward;
    private List<String> likedBy;
    private List<String> dislikedBy;
    private String signature;

    public Post() {
        this.mediaList = new ArrayList<>();
        this.likedBy = new ArrayList<>();
        this.dislikedBy = new ArrayList<>();
    }

    public Post(String postId, String authorAddress, String authorNickname, String content) {
        this();
        this.postId = postId;
        this.authorAddress = authorAddress;
        this.authorNickname = authorNickname;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getAuthorAddress() { return authorAddress; }
    public void setAuthorAddress(String authorAddress) { this.authorAddress = authorAddress; }

    public String getAuthorNickname() { return authorNickname; }
    public void setAuthorNickname(String authorNickname) { this.authorNickname = authorNickname; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<MediaContent> getMediaList() { return mediaList; }
    public void setMediaList(List<MediaContent> mediaList) { this.mediaList = mediaList; }

    public void addMedia(MediaContent media) { this.mediaList.add(media); }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getDislikeCount() { return dislikeCount; }
    public void setDislikeCount(int dislikeCount) { this.dislikeCount = dislikeCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public int getGoldReward() { return goldReward; }
    public void setGoldReward(int goldReward) { this.goldReward = goldReward; }

    public List<String> getLikedBy() { return likedBy; }
    public void setLikedBy(List<String> likedBy) { this.likedBy = likedBy; }

    public List<String> getDislikedBy() { return dislikedBy; }
    public void setDislikedBy(List<String> dislikedBy) { this.dislikedBy = dislikedBy; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}