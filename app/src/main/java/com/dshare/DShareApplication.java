package com.dshare;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.Log;

import com.dshare.blockchain.Blockchain;
import com.dshare.crypto.Wallet;
import com.dshare.network.P2PNode;
import com.dshare.network.PeerManager;
import com.dshare.storage.ContentManager;
import com.dshare.storage.LocalDatabase;

import java.util.List;

public class DShareApplication extends Application {

    private static final String TAG = "DShareApp";
    private static DShareApplication instance;

    private Wallet wallet;
    private LocalDatabase database;
    private ContentManager contentManager;
    private PeerManager peerManager;
    private P2PNode p2pNode;
    private Blockchain blockchain;

    private boolean isLoggedIn = false;
    private String currentUserAddress;
    
    private PowerManager.WakeLock wakeLock;
    private boolean isLowBatteryMode = false;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        database = new LocalDatabase(this);
        contentManager = new ContentManager(this);
        peerManager = new PeerManager(this);
        blockchain = new Blockchain();

        List<com.dshare.blockchain.Block> savedBlocks = database.getAllBlocks();
        if (savedBlocks != null && !savedBlocks.isEmpty()) {
            blockchain.setChain(savedBlocks);
        }
        
        registerBatteryReceiver();
    }

    public static DShareApplication getInstance() {
        return instance;
    }

    public void initWallet(Wallet wallet) {
        this.wallet = wallet;
        this.p2pNode = new P2PNode(wallet, peerManager, this);
        this.isLoggedIn = true;
        this.currentUserAddress = wallet.getAddress();
    }

    public void startP2P() {
        if (p2pNode != null && !p2pNode.isRunning() && !isLowBatteryMode) {
            p2pNode.start();
        }
    }

    public void stopP2P() {
        if (p2pNode != null) {
            p2pNode.stop();
        }
    }

    public void logout() {
        stopP2P();
        this.wallet = null;
        this.p2pNode = null;
        this.isLoggedIn = false;
        this.currentUserAddress = null;
    }
    
    private void registerBatteryReceiver() {
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                    int batteryPct = (level * 100) / scale;
                    
                    boolean wasLow = isLowBatteryMode;
                    isLowBatteryMode = batteryPct < 20;
                    
                    if (!wasLow && isLowBatteryMode) {
                        Log.w(TAG, "Entering low battery mode, stopping P2P");
                        stopP2P();
                    } else if (wasLow && !isLowBatteryMode && isLoggedIn) {
                        Log.i(TAG, "Battery recovered, starting P2P");
                        startP2P();
                    }
                }
            }, filter);
        } catch (Exception e) {
            Log.e(TAG, "Error registering battery receiver", e);
        }
    }

    public Wallet getWallet() { return wallet; }
    public LocalDatabase getDatabase() { return database; }
    public ContentManager getContentManager() { return contentManager; }
    public PeerManager getPeerManager() { return peerManager; }
    public P2PNode getP2PNode() { return p2pNode; }
    public Blockchain getBlockchain() { return blockchain; }
    public boolean isLoggedIn() { return isLoggedIn; }
    public String getCurrentUserAddress() { return currentUserAddress; }

    public long getGoldBalance() {
        if (wallet != null) {
            return blockchain.getBalance(wallet.getAddress());
        }
        return 0;
    }
}
