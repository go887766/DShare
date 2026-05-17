package com.dshare.blockchain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Blockchain implements Serializable {
    private List<Block> chain;
    private int difficulty;
    private List<Transaction> pendingTransactions;
    private long miningReward;

    public Blockchain() {
        this.chain = new ArrayList<>();
        this.pendingTransactions = new ArrayList<>();
        this.difficulty = 4;
        this.miningReward = 10;
        chain.add(createGenesisBlock());
    }

    private Block createGenesisBlock() {
        Block genesis = new Block(0, "0");
        genesis.setHash(genesis.calculateHash());
        Transaction genesisTx = new Transaction(
            "genesis", "0", "0", 1000000, Transaction.TYPE_REWARD
        );
        genesis.addTransaction(genesisTx);
        return genesis;
    }

    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    public void addBlock(Block newBlock) {
        newBlock.setPreviousHash(getLatestBlock().getHash());
        newBlock.mineBlock(difficulty);
        chain.add(newBlock);
    }

    public boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);
            if (!current.getHash().equals(current.calculateHash())) return false;
            if (!current.getPreviousHash().equals(previous.getHash())) return false;
        }
        return true;
    }

    public String addTransaction(Transaction transaction) {
        if (transaction.getFromAddress() == null || transaction.getToAddress() == null) {
            return "Transaction must have from and to addresses";
        }
        if (!transaction.getType().equals(Transaction.TYPE_REWARD)) {
            long balance = getBalance(transaction.getFromAddress());
            if (balance < transaction.getAmount()) {
                return "Insufficient balance";
            }
        }
        pendingTransactions.add(transaction);
        return "Transaction added to pending";
    }

    public void minePendingTransactions(String minerAddress) {
        Block block = new Block(chain.size(), getLatestBlock().getHash());
        for (Transaction tx : pendingTransactions) {
            block.addTransaction(tx);
        }
        Transaction rewardTx = new Transaction(
            "reward_" + System.currentTimeMillis(),
            "0", minerAddress, miningReward, Transaction.TYPE_MINING
        );
        block.addTransaction(rewardTx);
        block.mineBlock(difficulty);
        chain.add(block);
        pendingTransactions.clear();
    }

    public long getBalance(String address) {
        long balance = 0;
        for (Block block : chain) {
            for (Transaction tx : block.getTransactions()) {
                if (tx.getToAddress() != null && tx.getToAddress().equals(address)) {
                    balance += tx.getAmount();
                }
                if (tx.getFromAddress() != null && tx.getFromAddress().equals(address)) {
                    balance -= tx.getAmount();
                }
            }
        }
        for (Transaction tx : pendingTransactions) {
            if (tx.getToAddress().equals(address)) {
                balance += tx.getAmount();
            }
            if (tx.getFromAddress().equals(address)) {
                balance -= tx.getAmount();
            }
        }
        return balance;
    }

    public List<Transaction> getTransactionsForAddress(String address) {
        List<Transaction> result = new ArrayList<>();
        for (Block block : chain) {
            for (Transaction tx : block.getTransactions()) {
                if (address.equals(tx.getFromAddress()) || address.equals(tx.getToAddress())) {
                    result.add(tx);
                }
            }
        }
        return result;
    }

    public List<Block> getChain() { return chain; }
    public void setChain(List<Block> chain) { this.chain = chain; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    public List<Transaction> getPendingTransactions() { return pendingTransactions; }

    public long getMiningReward() { return miningReward; }
    public void setMiningReward(long miningReward) { this.miningReward = miningReward; }
}