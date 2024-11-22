package com.example.meshup;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_CODE = 1001;
    private static final int BROADCAST_PORT = 8888;
    private static final int MAX_HOP_COUNT = 5; // Prevent infinite routing

    private static final long ACKNOWLEDGMENT_TIMEOUT = 5000; // 5 seconds

    private RecyclerView messageRecyclerView;
    private MessageAdapter messageAdapter;
    private TextView deviceCountText;
    private String userName;
    private String deviceMac;

    private EditText messageInput;
//    private TextView messageView;
    private Button sendButton;
    private TextView statusText;

    private ExecutorService executorService;
    private Handler mainHandler;

    private volatile boolean isReceiving = true;

    // Track seen message IDs to prevent duplicates
    private Set<String> seenMessageIds = ConcurrentHashMap.newKeySet();

    // Unique device ID
    private final String DEVICE_ID = UUID.randomUUID().toString();

    // Track connected devices
    private Set<String> connectedDevices = ConcurrentHashMap.newKeySet();

    private String localUsername = "Anonymous";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements BEFORE using them
        initializeUIComponents();

        // Setup execution and UI handling
        executorService = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());

        // Check and request necessary permissions
        checkPermissions();

        // Setup UI interactions
        setupUI();

        // Start background message receiver
        startMessageReceiver();

        // Update connection status
        updateConnectionStatus();

        // Prompt for username after initial setup
        promptForUserName();
    }

    private void initializeUIComponents() {
        // Ensure ALL UI components are initialized
        messageInput = findViewById(R.id.messageInput);
//        messageView = findViewById(R.id.messageView); // Keep this if you still want it
        sendButton = findViewById(R.id.sendButton);
        statusText = findViewById(R.id.statusText);
        deviceCountText = findViewById(R.id.deviceCountText);

        // Initialize RecyclerView
        messageRecyclerView = findViewById(R.id.messageRecyclerView);
        messageAdapter = new MessageAdapter(this);
        messageRecyclerView.setAdapter(messageAdapter);
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupUI() {
        sendButton.setOnClickListener(v -> {
            // Null and empty checks
            if (messageInput == null) {
                Log.e("MainActivity", "Message input is null");
                return;
            }

            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                broadcastMessage(message);
                messageInput.setText(""); // Clear input
            }
        });
    }

    private void promptForUserName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        EditText input = new EditText(this);

        builder.setTitle("Enter Your Name")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    localUsername = TextUtils.isEmpty(name)
                            ? "User-" + DEVICE_ID.substring(0, 8)
                            : name;
                })
                .show();
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CHANGE_WIFI_MULTICAST_STATE
        };

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSIONS_REQUEST_CODE);
        }
    }


    private void updateConnectionStatus() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);

            if (wifiManager == null || statusText == null) return;

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            String status = "Network Status: ";
            try {
                String localIP = getLocalIpAddress();
                status += (localIP != null ? "Connected (IP: " + localIP + ")" : "Not Connected");
            } catch (Exception e) {
                status += "Error detecting network";
            }

            final String finalStatus = status;
            if (mainHandler != null) {
                mainHandler.post(() -> {
                    if (statusText != null) {
                        statusText.setText(finalStatus);
                    }
                });
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to update connection status", e);
        }
    }

    private void broadcastMessage(final String message) {
        executorService.execute(() -> {
            try {
                // Use local username or default
                String senderName = TextUtils.isEmpty(localUsername)
                        ? "User-" + DEVICE_ID.substring(0, 8)
                        : localUsername;

                // Get device MAC safely
                String macAddress = "Unknown";
                try {
                    WifiManager wifiManager = (WifiManager) getApplicationContext()
                            .getSystemService(Context.WIFI_SERVICE);
                    if (wifiManager != null) {
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        macAddress = wifiInfo != null ? wifiInfo.getMacAddress() : "Unknown";
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Failed to get MAC address", e);
                }

                // Create message with safe values
                String messageId = UUID.randomUUID().toString();
                String localIP = getLocalIpAddress();
                Message msgObj = new Message(
                        senderName,
                        macAddress,
                        localIP != null ? localIP : "Unknown",
                        message,
                        messageId
                );

                // Safely update UI on main thread
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        if (messageAdapter != null) {
                            messageAdapter.addMessage(msgObj);
                        }
                    });
                }

                // Construct mesh message
                String meshMessage = constructMeshMessage(messageId, message);
                // Construct mesh message

                // Create UDP broadcast socket
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

                // Prepare message data
                byte[] sendData = meshMessage.getBytes();

                // Get network interface details
                List<InetAddress> broadcastAddresses = getBroadcastAddresses();

                // Send to all potential broadcast addresses
                for (InetAddress broadcastAddress : broadcastAddresses) {
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(
                                sendData,
                                sendData.length,
                                broadcastAddress,
                                BROADCAST_PORT
                        );

                        socket.send(sendPacket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                socket.close();

                // Start acknowledgment timeout
                scheduleAcknowledgmentTimeout(messageId);

            } catch (SocketException e) {
                Log.e("MainActivity", "Broadcast message failed", e);
                // Safely show toast on main thread
                if (mainHandler != null)
                    mainHandler.post(() -> {
                        Toast.makeText(MainActivity.this,
                                "Failed to broadcast message",
                                Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void scheduleAcknowledgmentTimeout(String messageId) {
        mainHandler.postDelayed(() -> {
            messageAdapter.updateMessageDeliveryStatus(messageId, false);
        }, ACKNOWLEDGMENT_TIMEOUT);
    }

    private List<InetAddress> getBroadcastAddresses() {
        List<InetAddress> broadcastAddresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast != null) {
                        broadcastAddresses.add(broadcast);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        // Add fallback broadcast address if no addresses found
        if (broadcastAddresses.isEmpty()) {
            try {
                broadcastAddresses.add(InetAddress.getByName("255.255.255.255"));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        return broadcastAddresses;
    }

    private void startMessageReceiver() {
        executorService.execute(() -> {
            try {
                // Create UDP socket to receive broadcasts
                DatagramSocket socket = new DatagramSocket(BROADCAST_PORT);
                socket.setBroadcast(true);

                byte[] receiveBuffer = new byte[1024];

                while (isReceiving) {
                    // Prepare packet to receive data
                    DatagramPacket receivePacket = new DatagramPacket(
                            receiveBuffer,
                            receiveBuffer.length
                    );

                    // Receive incoming packet
                    socket.receive(receivePacket);

                    // Convert received data to message
                    String receivedMessage = new String(
                            receivePacket.getData(),
                            0,
                            receivePacket.getLength()
                    );

                    // Process the received mesh message
                    processReceivedMessage(receivedMessage,
                            receivePacket.getAddress().getHostAddress());
                }

                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this,
                            "Failed to receive messages",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String constructMeshMessage(String messageId, String message) {
        // Format: messageId|originDeviceId|hopCount|senderUsername|message
        return String.format("%s|%s|0|%s|%s",
                messageId,
                DEVICE_ID,
                localUsername,  // Include actual username
                message
        );
    }

    private void processReceivedMessage(String receivedMessage, String senderIP) {
        String[] parts = receivedMessage.split("\\|");
        if (parts.length != 5) return;

        String messageId = parts[0];
        String originDeviceId = parts[1];
        int hopCount = Integer.parseInt(parts[2]);
        String senderUsername = parts[3];
        String message = parts[4];

        // Track connected device
        connectedDevices.add(originDeviceId);
        updateDeviceCount();

        // Check if we've already seen this message
        if (seenMessageIds.contains(messageId)) {
            return; // Prevent duplicate processing
        }

        // Add message to seen list
        seenMessageIds.add(messageId);

        // Check hop count to prevent infinite routing
        if (hopCount >= MAX_HOP_COUNT) {
            return;
        }

        // If not from this device, process and potentially rebroadcast
        if (!originDeviceId.equals(DEVICE_ID)) {
            sendAcknowledgment(messageId, originDeviceId);

            // Display received message on UI thread
            mainHandler.post(() -> {
                Message msgObj = new Message(
                        senderUsername,  // Use actual sender username
                        "Unknown",
                        senderIP,
                        message,
                        messageId
                );
                msgObj.setDelivered(true);
                messageAdapter.addMessage(msgObj);
            });

            // Enhanced routing: Rebroadcast with incremented hop count
            rebroadcastMessage(messageId, originDeviceId, hopCount, senderUsername, message);
        }
    }

    private void rebroadcastMessage(String messageId, String originDeviceId, int hopCount, String senderUsername, String message) {
        executorService.execute(() -> {
            try {
                // Increment hop count
                int newHopCount = hopCount + 1;
                String forwardMessage = String.format("%s|%s|%d|%s|%s",
                        messageId, originDeviceId, newHopCount, senderUsername, message);

                // Create UDP broadcast socket
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

                // Prepare message data
                byte[] sendData = forwardMessage.getBytes();

                // Get network interface details
                List<InetAddress> broadcastAddresses = getBroadcastAddresses();

                // Send to all potential broadcast addresses
                for (InetAddress broadcastAddress : broadcastAddresses) {
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(
                                sendData,
                                sendData.length,
                                broadcastAddress,
                                BROADCAST_PORT
                        );

                        socket.send(sendPacket);
                        Log.d("Mesh Network", "Rebroadcast sent to: " + broadcastAddress.getHostAddress());
                    } catch (IOException e) {
                        Log.e("Mesh Network", "Rebroadcast failed to " + broadcastAddress.getHostAddress(), e);
                    }
                }

                socket.close();
            } catch (Exception e) {
                Log.e("Mesh Network", "Rebroadcast error", e);
            }
        });
    }

    // Remove disconnected devices periodically
    private void startDeviceCleanup() {
        executorService.execute(() -> {
            while (isReceiving) {
                try {
                    Thread.sleep(30000); // Check every 30 seconds
                    // Remove devices not seen in last 60 seconds
                    // Implementation depends on how you want to track device activity
                    updateDeviceCount();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    private void sendAcknowledgment(String messageId, String targetDeviceId) {
        // Send UDP acknowledgment packet
        executorService.execute(() -> {
            try {
                String ackMessage = "ACK|" + messageId + "|" + DEVICE_ID;
                // ... (similar to broadcast code, but with ack message)
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateDeviceCount() {
        mainHandler.post(() -> {
            deviceCountText.setText("Devices: " + connectedDevices.size());
        });
    }


    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                     enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() &&
                            inetAddress.getHostAddress().contains(".")) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop receiving messages
        isReceiving = false;
        // Shutdown executor service
        executorService.shutdown();
    }
}