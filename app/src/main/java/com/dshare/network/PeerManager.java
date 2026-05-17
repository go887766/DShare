package com.dshare.network;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class PeerManager {

    public static class PeerInfo {
        public String peerId;
        public String host;
        public int port;
        public String address;
        public String publicKeyBase64;
        public String nickname;
        public long lastSeen;
        public boolean isConnected;

        public PeerInfo() {}

        public PeerInfo(String peerId, String host, int port, String address) {
            this.peerId = peerId;
            this.host = host;
            this.port = port;
            this.address = address;
            this.lastSeen = System.currentTimeMillis();
        }
    }

    private PeerDatabase dbHelper;
    private List<PeerInfo> activePeers;

    public PeerManager(Context context) {
        this.dbHelper = new PeerDatabase(context);
        this.activePeers = new ArrayList<>();
    }

    public void addPeer(PeerInfo peer) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("peer_id", peer.peerId);
        values.put("host", peer.host);
        values.put("port", peer.port);
        values.put("address", peer.address);
        values.put("public_key", peer.publicKeyBase64);
        values.put("last_seen", peer.lastSeen);
        db.replace("known_peers", null, values);
    }

    public List<PeerInfo> getKnownPeers() {
        List<PeerInfo> peers = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM known_peers ORDER BY last_seen DESC LIMIT 100", null);
        while (cursor.moveToNext()) {
            PeerInfo peer = new PeerInfo();
            peer.peerId = cursor.getString(0);
            peer.host = cursor.getString(1);
            peer.port = cursor.getInt(2);
            peer.address = cursor.getString(3);
            peer.publicKeyBase64 = cursor.getString(4);
            peer.nickname = cursor.getString(5);
            peer.lastSeen = cursor.getLong(6);
            peers.add(peer);
        }
        cursor.close();
        return peers;
    }

    public List<PeerInfo> getActivePeers() { return activePeers; }

    public void setActivePeers(List<PeerInfo> activePeers) { this.activePeers = activePeers; }

    public void addActivePeer(PeerInfo peer) {
        if (!activePeers.contains(peer)) {
            activePeers.add(peer);
        }
    }

    public void removeActivePeer(PeerInfo peer) {
        activePeers.remove(peer);
    }

    public void updatePeerLastSeen(String peerId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("last_seen", System.currentTimeMillis());
        db.update("known_peers", values, "peer_id = ?", new String[]{peerId});
    }

    private static class PeerDatabase extends SQLiteOpenHelper {
        PeerDatabase(Context context) {
            super(context, "peers.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS known_peers (" +
                    "peer_id TEXT PRIMARY KEY," +
                    "host TEXT," +
                    "port INTEGER," +
                    "address TEXT," +
                    "public_key TEXT," +
                    "nickname TEXT," +
                    "last_seen INTEGER)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS known_peers");
            onCreate(db);
        }
    }
}