package com.dshare.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import com.dshare.crypto.CryptoManager;
import com.dshare.crypto.Wallet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class P2PNode {

    private static final String TAG = "P2PNode";
    private static final int TCP_PORT = 8333;
    private static final int UDP_PORT = 8334;
    private static final int BUFFER_SIZE = 32768;
    
    private static final int DISCOVERY_INTERVAL_NORMAL = 60000;
    private static final int DISCOVERY_INTERVAL_LOW_BATTERY = 180000;
    private static final int DISCOVERY_INTERVAL_CRITICAL = 300000;
    
    private static final int THREAD_POOL_SIZE = 4;
    private static final int CONNECTION_TIMEOUT = 3000;
    private static final int SOCKET_TIMEOUT = 10000;

    private Wallet wallet;
    private PeerManager peerManager;
    private ServerSocket serverSocket;
    private DatagramSocket udpSocket;
    private volatile boolean isRunning;
    private ExecutorService threadPool;
    private List<P2PConnectionListener> listeners;
    private List<String> bootstrapNodes;
    
    private Context context;
    private volatile boolean isLowBatteryMode;
    private volatile boolean isCriticalBatteryMode;
    private volatile boolean isWifiConnected;
    private long lastDiscoveryTime;
    private BroadcastReceiver batteryReceiver;
    private BroadcastReceiver networkReceiver;

    public interface P2PConnectionListener {
        void onMessageReceived(MessageProtocol message);
        void onPeerConnected(PeerManager.PeerInfo peer);
        void onPeerDisconnected(PeerManager.PeerInfo peer);
        void onError(String error);
    }

    public P2PNode(Wallet wallet, PeerManager peerManager, Context context) {
        this.wallet = wallet;
        this.peerManager = peerManager;
        this.context = context.getApplicationContext();
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.listeners = new CopyOnWriteArrayList<>();
        this.bootstrapNodes = new ArrayList<>();
        this.lastDiscoveryTime = 0;
        this.isLowBatteryMode = false;
        this.isCriticalBatteryMode = false;
        this.isWifiConnected = true;
    }

    public void addListener(P2PConnectionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(P2PConnectionListener listener) {
        listeners.remove(listener);
    }

    public void start() {
        try {
            isRunning = true;
            registerBatteryReceiver();
            registerNetworkReceiver();
            threadPool.execute(this::startTcpServer);
            threadPool.execute(this::startUdpDiscovery);
            Log.d(TAG, "P2P node started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start P2P node", e);
            notifyError("Failed to start: " + e.getMessage());
        }
    }

    public void stop() {
        isRunning = false;
        unregisterReceivers();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
            }
            shutdownThreadPool();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping P2P node", e);
        }
    }
    
    private void shutdownThreadPool() {
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(3, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void startTcpServer() {
        try {
            serverSocket = new ServerSocket(TCP_PORT);
            serverSocket.setSoTimeout(5000);
            Log.d(TAG, "TCP server listening on port " + TCP_PORT);
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    if (isRunning) {
                        clientSocket.setSoTimeout(SOCKET_TIMEOUT);
                        threadPool.execute(() -> handleClientConnection(clientSocket));
                    } else {
                        clientSocket.close();
                    }
                } catch (java.net.SocketTimeoutException e) {
                } catch (Exception e) {
                    if (isRunning) Log.e(TAG, "Error accepting connection", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start TCP server", e);
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(clientSocket.getOutputStream()), true);

            String line;
            StringBuilder messageBuilder = new StringBuilder();
            while (isRunning && (line = reader.readLine()) != null) {
                messageBuilder.append(line);
                if (line.endsWith("}")) break;
            }

            String jsonData = messageBuilder.toString();
            if (!jsonData.isEmpty()) {
                MessageProtocol message = parseMessage(jsonData);
                if (message != null) {
                    processMessage(message);
                }
            }
            clientSocket.close();
        } catch (Exception e) {
            Log.e(TAG, "Error handling client", e);
        }
    }

    private void startUdpDiscovery() {
        try {
            udpSocket = new DatagramSocket(UDP_PORT);
            udpSocket.setBroadcast(true);
            udpSocket.setSoTimeout(5000);
            Log.d(TAG, "UDP discovery listening on port " + UDP_PORT);

            byte[] buffer = new byte[BUFFER_SIZE];
            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    threadPool.execute(() -> handleDiscoveryMessage(message, packet.getAddress()));
                } catch (java.net.SocketTimeoutException e) {
                } catch (Exception e) {
                    if (isRunning) Log.e(TAG, "UDP receive error", e);
                }
                
                if (shouldDiscover()) {
                    performDiscovery();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start UDP discovery", e);
        }
    }
    
    private boolean shouldDiscover() {
        if (!isWifiConnected || isCriticalBatteryMode) {
            return false;
        }
        long now = System.currentTimeMillis();
        long interval = isLowBatteryMode ? DISCOVERY_INTERVAL_LOW_BATTERY : DISCOVERY_INTERVAL_NORMAL;
        return (now - lastDiscoveryTime) > interval;
    }
    
    private void performDiscovery() {
        lastDiscoveryTime = System.currentTimeMillis();
        broadcastDiscovery();
    }

    public void broadcastDiscovery() {
        if (!isWifiConnected) return;
        
        try {
            MessageProtocol discoveryMsg = new MessageProtocol(
                MessageProtocol.TYPE_DISCOVERY,
                wallet.getAddress(),
                ""
            );
            String json = discoveryMsg.toJsonString();
            byte[] sendData = json.getBytes();

            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(2000);

            List<InetAddress> broadcastAddresses = getBroadcastAddresses();
            for (InetAddress addr : broadcastAddresses) {
                DatagramPacket packet = new DatagramPacket(
                    sendData, sendData.length, addr, UDP_PORT);
                socket.send(packet);
            }
            socket.close();
        } catch (Exception e) {
            Log.e(TAG, "Broadcast discovery error", e);
        }
    }

    private List<InetAddress> getBroadcastAddresses() {
        List<InetAddress> broadcastList = new ArrayList<>();
        try {
            List<NetworkInterface> interfaces = Collections.list(
                    NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                if (networkInterface.isLoopback() || !networkInterface.isUp()) continue;
                for (java.net.InterfaceAddress interfaceAddress :
                        networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast != null) {
                        broadcastList.add(broadcast);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting broadcast addresses", e);
        }
        return broadcastList;
    }

    private void handleDiscoveryMessage(String message, InetAddress address) {
        try {
            MessageProtocol msg = parseMessage(message);
            if (msg != null && MessageProtocol.TYPE_DISCOVERY.equals(msg.getType())) {
                MessageProtocol response = new MessageProtocol(
                    MessageProtocol.TYPE_DISCOVERY_RESPONSE,
                    wallet.getAddress(),
                    "{\"address\":\"" + wallet.getAddress() + "\",\"nickname\":\"\"}"
                );
                String responseJson = response.toJsonString();
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(2000);
                DatagramPacket packet = new DatagramPacket(
                    responseJson.getBytes(), responseJson.getBytes().length,
                    address, UDP_PORT);
                socket.send(packet);
                socket.close();
            } else if (msg != null && MessageProtocol.TYPE_DISCOVERY_RESPONSE.equals(msg.getType())) {
                PeerManager.PeerInfo peer = new PeerManager.PeerInfo(
                    msg.getSenderAddress(), address.getHostAddress(), TCP_PORT, msg.getSenderAddress()
                );
                peer.nickname = msg.getSenderNickname();
                peerManager.addPeer(peer);
                notifyPeerConnected(peer);
            }
        } catch (Exception e) {
            Log.e(TAG, "Handle discovery error", e);
        }
    }

    public void sendMessage(MessageProtocol message, String host, int port) {
        Socket socket = null;
        try {
            message.setSenderAddress(wallet.getAddress());
            String dataToSign = message.getType() + message.getPayload() + message.getTimestamp();
            message.setSignature(wallet.sign(dataToSign));

            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT);
            socket.setSoTimeout(SOCKET_TIMEOUT);
            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true);
            writer.println(message.toJsonString());
            writer.flush();
        } catch (Exception e) {
            Log.e(TAG, "Send message error", e);
            notifyError("Failed to send message: " + e.getMessage());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public void broadcastMessage(MessageProtocol message) {
        List<PeerManager.PeerInfo> peers = peerManager.getActivePeers();
        int maxPeers = isLowBatteryMode ? Math.min(peers.size(), 3) : peers.size();
        for (int i = 0; i < maxPeers; i++) {
            sendMessage(message, peers.get(i).host, peers.get(i).port);
        }
    }

    private MessageProtocol parseMessage(String json) {
        try {
            MessageProtocol msg = new MessageProtocol();
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                String content = json.substring(1, json.length() - 1);
                String[] pairs = splitJsonPairs(content);
                for (String pair : pairs) {
                    int colonIndex = pair.indexOf(":");
                    if (colonIndex > 0) {
                        String key = pair.substring(0, colonIndex).trim().replace("\"", "");
                        String value = pair.substring(colonIndex + 1).trim();
                        value = value.replaceAll("^\"|\"$", "");
                        switch (key) {
                            case "messageId": msg.setMessageId(value); break;
                            case "type": msg.setType(value); break;
                            case "senderAddress": msg.setSenderAddress(value); break;
                            case "senderNickname": msg.setSenderNickname(value); break;
                            case "payload": msg.setPayload(value); break;
                            case "timestamp": msg.setTimestamp(Long.parseLong(value)); break;
                            case "signature": msg.setSignature(value); break;
                        }
                    }
                }
            }
            return msg;
        } catch (Exception e) {
            Log.e(TAG, "Parse message error", e);
            return null;
        }
    }

    private String[] splitJsonPairs(String content) {
        List<String> pairs = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (char c : content.toCharArray()) {
            if (c == '"' && (current.length() == 0 || current.charAt(current.length() - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{' || c == '[') depth++;
                if (c == '}' || c == ']') depth--;
                if (c == ',' && depth == 0) {
                    pairs.add(current.toString());
                    current = new StringBuilder();
                    continue;
                }
            }
            current.append(c);
        }
        if (current.length() > 0) pairs.add(current.toString());
        return pairs.toArray(new String[0]);
    }

    private void processMessage(MessageProtocol message) {
        if (message.getType() == null) return;
        if (MessageProtocol.TYPE_POST.equals(message.getType()) ||
            MessageProtocol.TYPE_COMMENT.equals(message.getType()) ||
            MessageProtocol.TYPE_LIKE.equals(message.getType()) ||
            MessageProtocol.TYPE_DISLIKE.equals(message.getType()) ||
            MessageProtocol.TYPE_TRANSACTION.equals(message.getType()) ||
            MessageProtocol.TYPE_BLOCK.equals(message.getType()) ||
            MessageProtocol.TYPE_USER_INFO.equals(message.getType())) {
            notifyMessageReceived(message);
        }
    }

    private void notifyMessageReceived(MessageProtocol message) {
        for (P2PConnectionListener listener : listeners) {
            listener.onMessageReceived(message);
        }
    }

    private void notifyPeerConnected(PeerManager.PeerInfo peer) {
        for (P2PConnectionListener listener : listeners) {
            listener.onPeerConnected(peer);
        }
    }

    private void notifyError(String error) {
        for (P2PConnectionListener listener : listeners) {
            listener.onError(error);
        }
    }

    public String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(
                    NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                if (networkInterface.isLoopback() || !networkInterface.isUp()) continue;
                for (java.net.InetAddress addr : Collections.list(
                        networkInterface.getInetAddresses())) {
                    if (addr instanceof java.net.Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting local IP", e);
        }
        return "127.0.0.1";
    }
    
    private void registerBatteryReceiver() {
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                int batteryPct = (level * 100) / scale;
                
                isLowBatteryMode = batteryPct < 30;
                isCriticalBatteryMode = batteryPct < 15;
                
                Log.d(TAG, "Battery level: " + batteryPct + "%, Low mode: " + isLowBatteryMode);
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(batteryReceiver, filter);
    }
    
    private void registerNetworkReceiver() {
        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                isWifiConnected = isWifiConnected(context);
                Log.d(TAG, "Wifi connected: " + isWifiConnected);
            }
        };
        IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        context.registerReceiver(networkReceiver, filter);
    }
    
    private boolean isWifiConnected(Context context) {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected() && 
                   info.getType() == android.net.ConnectivityManager.TYPE_WIFI;
        } catch (Exception e) {
            return true;
        }
    }
    
    private void unregisterReceivers() {
        try {
            if (batteryReceiver != null) {
                context.unregisterReceiver(batteryReceiver);
                batteryReceiver = null;
            }
            if (networkReceiver != null) {
                context.unregisterReceiver(networkReceiver);
                networkReceiver = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receivers", e);
        }
    }

    public int getPort() { return TCP_PORT; }
    public boolean isRunning() { return isRunning; }
}
