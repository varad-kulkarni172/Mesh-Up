package com.example.meshup;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class NetworkControlActivity extends AppCompatActivity {
    private Button startNetworkButton;
    private Button stopNetworkButton;
    private TextView networkStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_control);

        // Initialize UI components
        startNetworkButton = findViewById(R.id.startNetworkButton);
        stopNetworkButton = findViewById(R.id.stopNetworkButton);
        networkStatusText = findViewById(R.id.networkStatusText);

        // Setup button click listeners
        setupButtonListeners();

        // Initial network status
        updateNetworkStatus(false);
    }

    private void setupButtonListeners() {
        startNetworkButton.setOnClickListener(v -> {
            // Start Messaging Service
            Intent serviceIntent = new Intent(this, MainActivity.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            }

            // Launch Main Activity
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);

            updateNetworkStatus(true);
        });

        stopNetworkButton.setOnClickListener(v -> {
            // Stop Messaging Service
            Intent serviceIntent = new Intent(this, MainActivity.class);
            stopService(serviceIntent);

            updateNetworkStatus(false);
        });
    }

    private void updateNetworkStatus(boolean isRunning) {
        if (isRunning) {
            networkStatusText.setText("Network Status: Running");
            networkStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            startNetworkButton.setEnabled(false);
            stopNetworkButton.setEnabled(true);
        } else {
            networkStatusText.setText("Network Status: Stopped");
            networkStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            startNetworkButton.setEnabled(true);
            stopNetworkButton.setEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // You might want to check if service is running and update UI accordingly
    }
}