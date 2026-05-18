package com.dshare.blockchain;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class Block implements Serializable {
    private long index;
    private String previousHash;
    private String hash;
    private long timestamp;
    private List<Transaction> transactions;
    private int nonce;
    private String merkleRoot;

    public Block() {
        this.transactions = new ArrayList<>();
    }

    public Block(long index, String previousHash) {
        this.index = index;
        this.previousHash = previousHash;
        this.timestamp = System.currentTimeMillis();
        this.transactions = new ArrayList<>();
        this.nonce = 0;
    }

    public String calculateHash() {
        try {
            String data = index + previousHash + timestamp + nonce + merkleRoot;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate hash", e);
        }
    }

    public void mineBlock(int difficulty) {
        String target = new String(new char[difficulty]).replace('\0', '0');
        hash = calculateHash();
        while (!hash.substring(0, difficulty).equals(target)) {
            nonce++;
            hash = calculateHash();
        }
    }

    public boolean addTransaction(Transaction tx) {
        if (tx == null) return false;
        if (!previousHash.equals("0") && tx.getSignature() == null) return false;
        transactions.add(tx);
        calculateMerkleRoot();
        return true;
    }

    private String calculateMerkleRoot() {
        if (transactions.isEmpty()) {
            merkleRoot = "0";
            return merkleRoot;
        }
        List<String> hashes = new ArrayList<>();
        for (Transaction tx : transactions) {
            hashes.add(sha256Hash(tx.toHashString()));
        }
        while (hashes.size() > 1) {
            List<String> newHashes = new ArrayList<>();
            for (int i = 0; i < hashes.size(); i += 2) {
                if (i + 1 < hashes.size()) {
                    newHashes.add(sha256Hash(hashes.get(i) + hashes.get(i + 1)));
                } else {
                    newHashes.add(hashes.get(i));
                }
            }
            hashes = newHashes;
        }
        merkleRoot = hashes.get(0);
        return merkleRoot;
    }

    private String sha256Hash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long getIndex() { return index; }
    public void setIndex(long index) { this.index = index; }

    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }

    public int getNonce() { return nonce; }
    public void setNonce(int nonce) { this.nonce = nonce; }
}