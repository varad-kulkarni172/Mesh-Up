package com.example.meshup;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_CODE = 1001;
    private static final int UDP_PORT = 8888;
    private static final int TCP_PORT = 8889;

    private EditText messageInput;
    private TextView messageView;
    private Button sendButton;
    private TextView statusText;

    private ExecutorService executorService;
    private Handler mainHandler;
    private boolean isReceiving = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageInput = findViewById(R.id.messageInput);
        messageView = findViewById(R.id.messageView);
        sendButton = findViewById(R.id.sendButton);
        statusText = findViewById(R.id.statusText);

        executorService = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());

        checkPermissions();
        setupUI();
        startMessageReceiver();

        // Display connection status
        updateConnectionStatus();
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION
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

        String ssid = wifiInfo.getSSID();
        String status = "Not connected to meshup network";

        if (ssid != null && ssid.contains("meshup")) {
            status = "Connected to: " + ssid;
        }

        final String finalStatus = status;
        mainHandler.post(() -> {
            statusText.setText(finalStatus);
        });
    }

    private void broadcastMessage(final String message) {
        executorService.execute(() -> {
            try {
                // Create UDP broadcast packet
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

                byte[] sendData = message.getBytes();

                // Send to broadcast address
                InetAddress broadcastAddr = InetAddress.getByName("255.255.255.255");
                DatagramPacket sendPacket = new DatagramPacket(
                        sendData,
                        sendData.length,
                        broadcastAddr,
                        UDP_PORT
                );

                socket.send(sendPacket);
                socket.close();

                // Update UI with sent message
                mainHandler.post(() -> {
                    String currentText = messageView.getText().toString();
                    messageView.setText(currentText + "\nMe: " + message);
                });

            } catch (IOException e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this,
                            "Failed to send message",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void startMessageReceiver() {
        executorService.execute(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(UDP_PORT);
                byte[] receiveData = new byte[1024];

                while (isReceiving) {
                    DatagramPacket receivePacket = new DatagramPacket(
                            receiveData,
                            receiveData.length
                    );

                    socket.receive(receivePacket);

                    String message = new String(
                            receivePacket.getData(),
                            0,
                            receivePacket.getLength()
                    );

                    String sender = receivePacket.getAddress().getHostAddress();

                    mainHandler.post(() -> {
                        String currentText = messageView.getText().toString();
                        messageView.setText(currentText + "\n" + sender + ": " + message);
                    });
                }

                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this,
                            "Failed to start receiver",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isReceiving = false;
        executorService.shutdown();
    }
}
