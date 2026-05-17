package com.dshare.model;

import java.io.Serializable;

public class Comment implements Serializable {
    private String commentId;
    private String postId;
    private String authorAddress;
    private String authorNickname;
    private String content;
    private long timestamp;
    private int likeCount;
    private int goldReward;
    private String signature;

    public Comment() {}

    public Comment(String commentId, String postId, String authorAddress, String content) {
        this.commentId = commentId;
        this.postId = postId;
        this.authorAddress = authorAddress;
        this.authorNickname = "";
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getAuthorAddress() { return authorAddress; }
    public void setAuthorAddress(String authorAddress) { this.authorAddress = authorAddress; }

    public String getAuthorNickname() { return authorNickname; }
    public void setAuthorNickname(String authorNickname) { this.authorNickname = authorNickname; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getGoldReward() { return goldReward; }
    public void setGoldReward(int goldReward) { this.goldReward = goldReward; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}