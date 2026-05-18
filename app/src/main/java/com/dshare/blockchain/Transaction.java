package com.dshare.blockchain;

import java.io.Serializable;

public class Transaction implements Serializable {
    private String txId;
    private String fromAddress;
    private String toAddress;
    private long amount;
    private long timestamp;
    private String type;
    private String dataHash;
    private String signature;

    public static final String TYPE_REWARD = "REWARD";
    public static final String TYPE_TRANSFER = "TRANSFER";
    public static final String TYPE_MINING = "MINING";
    public static final String TYPE_POST_REWARD = "POST_REWARD";

    public Transaction() {}

    public Transaction(String txId, String fromAddress, String toAddress, long amount, String type) {
        this.txId = txId;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.amount = amount;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public String getTxId() { return txId; }
    public void setTxId(String txId) { this.txId = txId; }

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

    public String getToAddress() { return toAddress; }
    public void setToAddress(String toAddress) { this.toAddress = toAddress; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDataHash() { return dataHash; }
    public void setDataHash(String dataHash) { this.dataHash = dataHash; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String toHashString() {
        return txId + fromAddress + toAddress + amount + timestamp + type;
    }
    
    public String getTypeDisplayName() {
        if (TYPE_REWARD.equals(type)) return "奖励";
        if (TYPE_TRANSFER.equals(type)) return "转账";
        if (TYPE_MINING.equals(type)) return "挖矿";
        if (TYPE_POST_REWARD.equals(type)) return "发帖奖励";
        return type;
    }
}