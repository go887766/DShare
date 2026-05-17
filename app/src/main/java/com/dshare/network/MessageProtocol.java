package com.dshare.network;

import java.io.Serializable;
import java.util.UUID;

public class MessageProtocol implements Serializable {

    private String messageId;
    private String type;
    private String senderAddress;
    private String senderNickname;
    private String payload;
    private long timestamp;
    private String signature;

    public static final String TYPE_DISCOVERY = "DISCOVERY";
    public static final String TYPE_DISCOVERY_RESPONSE = "DISCOVERY_RESPONSE";
    public static final String TYPE_POST = "POST";
    public static final String TYPE_POST_LIST = "POST_LIST";
    public static final String TYPE_POST_REQUEST = "POST_REQUEST";
    public static final String TYPE_POST_RESPONSE = "POST_RESPONSE";
    public static final String TYPE_COMMENT = "COMMENT";
    public static final String TYPE_LIKE = "LIKE";
    public static final String TYPE_DISLIKE = "DISLIKE";
    public static final String TYPE_TRANSACTION = "TRANSACTION";
    public static final String TYPE_BLOCK = "BLOCK";
    public static final String TYPE_BLOCKCHAIN_REQUEST = "BLOCKCHAIN_REQUEST";
    public static final String TYPE_BLOCKCHAIN_RESPONSE = "BLOCKCHAIN_RESPONSE";
    public static final String TYPE_USER_INFO = "USER_INFO";
    public static final String TYPE_FILE_REQUEST = "FILE_REQUEST";
    public static final String TYPE_FILE_RESPONSE = "FILE_RESPONSE";
    public static final String TYPE_PEER_LIST = "PEER_LIST";
    public static final String TYPE_PING = "PING";
    public static final String TYPE_PONG = "PONG";

    public MessageProtocol() {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public MessageProtocol(String type, String senderAddress, String payload) {
        this();
        this.type = type;
        this.senderAddress = senderAddress;
        this.payload = payload;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSenderAddress() { return senderAddress; }
    public void setSenderAddress(String senderAddress) { this.senderAddress = senderAddress; }

    public String getSenderNickname() { return senderNickname; }
    public void setSenderNickname(String senderNickname) { this.senderNickname = senderNickname; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String toJsonString() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"messageId\":\"").append(escapeJson(messageId)).append("\",");
        json.append("\"type\":\"").append(escapeJson(type)).append("\",");
        json.append("\"senderAddress\":\"").append(escapeJson(senderAddress)).append("\",");
        json.append("\"senderNickname\":\"").append(escapeJson(senderNickname)).append("\",");
        json.append("\"payload\":\"").append(escapeJson(payload)).append("\",");
        json.append("\"timestamp\":").append(timestamp).append(",");
        json.append("\"signature\":\"").append(escapeJson(signature)).append("\"");
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}