package com.example.meshup;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

    private EditText messageInput;
    private TextView messageView;
    private Button sendButton;
    private TextView statusText;

    private ExecutorService executorService;
    private Handler mainHandler;

    private volatile boolean isReceiving = true;

    // Track seen message IDs to prevent duplicates
    private Set<String> seenMessageIds = ConcurrentHashMap.newKeySet();

    // Unique device ID
    private final String DEVICE_ID = UUID.randomUUID().toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        messageInput = findViewById(R.id.messageInput);
        messageView = findViewById(R.id.messageView);
        sendButton = findViewById(R.id.sendButton);
        statusText = findViewById(R.id.statusText);

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

    private void setupUI() {
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString();
            if (!message.isEmpty()) {
                broadcastMessage(message);
                messageInput.setText("");
            }
        });
    }

    private void updateConnectionStatus() {
        WifiManager wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        String status = "Network Status: ";

        try {
            // Get local IP address
            String localIP = getLocalIpAddress();
            status += (localIP != null ? "Connected (IP: " + localIP + ")" : "Not Connected");
        } catch (Exception e) {
            status += "Error detecting network";
        }

        final String finalStatus = status;
        mainHandler.post(() -> {
            statusText.setText(finalStatus);
        });
    }

    private void broadcastMessage(final String message) {
        executorService.execute(() -> {
            try {
                // Create a unique message ID to prevent routing loops
                String messageId = UUID.randomUUID().toString();

                // Construct mesh network message
                String meshMessage = constructMeshMessage(messageId, message);

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

                // Update UI with sent message
                mainHandler.post(() -> {
                    String currentText = messageView.getText().toString();
                    messageView.setText(currentText + "\nMe: " + message);
                });

            } catch (SocketException e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this,
                            "Failed to broadcast message",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
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
        // Format: messageId|originDeviceId|hopCount|message
        return String.format("%s|%s|0|%s",
                messageId,
                DEVICE_ID,
                message
        );
    }

    private void processReceivedMessage(String receivedMessage, String senderIP) {
        // Split the mesh message
        String[] parts = receivedMessage.split("\\|");
        if (parts.length != 4) {
            return; // Malformed message
        }

        String messageId = parts[0];
        String originDeviceId = parts[1];
        int hopCount = Integer.parseInt(parts[2]);
        String message = parts[3];

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

        // Update hop count and rebroadcast if not from this device
        if (!originDeviceId.equals(DEVICE_ID)) {
            // Rebroadcast the message with incremented hop count
            String forwardMessage = String.format("%s|%s|%d|%s",
                    messageId,
                    originDeviceId,
                    hopCount + 1,
                    message
            );

            // Broadcast to all interfaces
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

                List<InetAddress> broadcastAddresses = getBroadcastAddresses();

                for (InetAddress broadcastAddress : broadcastAddresses) {
                    byte[] sendData = forwardMessage.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(
                            sendData,
                            sendData.length,
                            broadcastAddress,
                            BROADCAST_PORT
                    );

                    socket.send(sendPacket);
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Display the received message
            mainHandler.post(() -> {
                String currentText = messageView.getText().toString();
                messageView.setText(currentText +
                        "\n" + senderIP + " (Forwarded): " + message);
            });
        }
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