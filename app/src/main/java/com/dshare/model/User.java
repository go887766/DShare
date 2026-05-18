package com.dshare.model;

import java.io.Serializable;

public class User implements Serializable {
    private String address;
    private String publicKeyBase64;
    private String encryptedPrivateKey;
    private String nickname;
    private String bio;
    private String avatarHash;
    private String qq;
    private String wechat;
    private String phone;
    private long createdAt;
    private long lastLoginAt;

    public User() {}

    public User(String address, String publicKeyBase64) {
        this.address = address;
        this.publicKeyBase64 = publicKeyBase64;
        this.nickname = "User_" + address.substring(0, 8);
        this.bio = "";
        this.qq = "";
        this.wechat = "";
        this.phone = "";
        this.createdAt = System.currentTimeMillis();
        this.lastLoginAt = System.currentTimeMillis();
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPublicKeyBase64() { return publicKeyBase64; }
    public void setPublicKeyBase64(String publicKeyBase64) { this.publicKeyBase64 = publicKeyBase64; }

    public String getEncryptedPrivateKey() { return encryptedPrivateKey; }
    public void setEncryptedPrivateKey(String encryptedPrivateKey) { this.encryptedPrivateKey = encryptedPrivateKey; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarHash() { return avatarHash; }
    public void setAvatarHash(String avatarHash) { this.avatarHash = avatarHash; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(long lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getQq() { return qq; }
    public void setQq(String qq) { this.qq = qq; }

    public String getWechat() { return wechat; }
    public void setWechat(String wechat) { this.wechat = wechat; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}