package com.dshare.network;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DHTManager {

    private static final String TAG = "DHTManager";
    private static final int MAX_BUCKET_SIZE = 20;
    private static final int K = 8;

    private final String localNodeId;
    private final Map<Integer, List<DHTNode>> routingTable;
    private final Map<String, List<String>> contentStore;

    public static class DHTNode {
        public String nodeId;
        public String address;
        public String host;
        public int port;
        public long lastSeen;

        public DHTNode(String nodeId, String host, int port, String address) {
            this.nodeId = nodeId;
            this.host = host;
            this.port = port;
            this.address = address;
            this.lastSeen = System.currentTimeMillis();
        }
    }

    public DHTManager(String localNodeId) {
        this.localNodeId = localNodeId;
        this.routingTable = new ConcurrentHashMap<>();
        this.contentStore = new ConcurrentHashMap<>();
        for (int i = 0; i < 160; i++) {
            routingTable.put(i, Collections.synchronizedList(new ArrayList<DHTNode>()));
        }
    }

    public void insertNode(DHTNode node) {
        int distance = xorDistance(localNodeId, node.nodeId);
        int bucketIndex = 160 - Integer.numberOfLeadingZeros(distance);
        if (bucketIndex >= 160) bucketIndex = 159;

        List<DHTNode> bucket = routingTable.get(bucketIndex);
        synchronized (bucket) {
            for (DHTNode existing : bucket) {
                if (existing.nodeId.equals(node.nodeId)) {
                    existing.lastSeen = System.currentTimeMillis();
                    return;
                }
            }
            if (bucket.size() < K) {
                bucket.add(node);
                Log.d(TAG, "Node added to bucket " + bucketIndex + ": " + node.nodeId.substring(0, 8));
            } else {
                bucket.sort(new Comparator<DHTNode>() {
                    @Override
                    public int compare(DHTNode a, DHTNode b) {
                        return Long.compare(a.lastSeen, b.lastSeen);
                    }
                });
                bucket.set(bucket.size() - 1, node);
            }
        }
    }

    public void storeContent(String contentHash, String peerAddress) {
        List<String> peers = contentStore.get(contentHash);
        if (peers == null) {
            peers = new ArrayList<>();
            contentStore.put(contentHash, peers);
        }
        if (!peers.contains(peerAddress)) {
            peers.add(peerAddress);
        }
    }

    public List<String> findContent(String contentHash) {
        return contentStore.get(contentHash);
    }

    public List<DHTNode> findNearestNodes(String chash) {
        byte[] targetHash = hexToBytes(localNodeId);
        byte[] contentBytes = hexToBytes(chash);
        int distance = xorDistanceBytes(targetHash, contentBytes);

        List<DHTNode> closestNodes = new ArrayList<>();
        int bucketIndex = 160 - Integer.numberOfLeadingZeros(distance);
        for (int i = Math.max(0, bucketIndex - 3); i <= Math.min(159, bucketIndex + 3); i++) {
            List<DHTNode> bucket = routingTable.get(i);
            synchronized (bucket) {
                closestNodes.addAll(bucket);
            }
        }

        Collections.sort(closestNodes, new Comparator<DHTNode>() {
            @Override
            public int compare(DHTNode a, DHTNode b) {
                int distA = xorDistance(a.nodeId, chash);
                int distB = xorDistance(b.nodeId, chash);
                return Integer.compare(distA, distB);
            }
        });

        return closestNodes.subList(0, Math.min(K, closestNodes.size()));
    }

    private int xorDistance(String a, String b) {
        if (a == null || b == null) return Integer.MAX_VALUE;
        byte[] aBytes = hexToBytes(a);
        byte[] bBytes = hexToBytes(b);
        int minLen = Math.min(aBytes.length, bBytes.length);
        int distance = 0;
        for (int i = 0; i < minLen; i++) {
            distance = (distance << 8) | (aBytes[i] ^ bBytes[i]);
        }
        return Math.abs(distance);
    }

    private int xorDistanceBytes(byte[] a, byte[] b) {
        int minLen = Math.min(a.length, b.length);
        int distance = 0;
        for (int i = 0; i < minLen; i++) {
            distance = (distance << 8) | (a[i] ^ b[i]);
        }
        return Math.abs(distance);
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null) return new byte[0];
        int len = hex.length();
        byte[] result = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            result[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return result;
    }

    public Map<Integer, List<DHTNode>> getRoutingTable() { return routingTable; }
    public Map<String, List<String>> getContentStore() { return contentStore; }
}