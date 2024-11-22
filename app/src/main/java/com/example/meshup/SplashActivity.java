package com.example.meshup;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import android.widget.ImageView;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 2500; // 2000 milliseconds = 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Load GIF using Glide
        ImageView logoImageView = findViewById(R.id.logoImageView);
        Glide.with(this)
                .asGif()
                .load(R.drawable.sos) // Replace with your actual GIF file
                .into(logoImageView);

        // Delay for 2000ms before transitioning to the next screen
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Start the next activity (e.g., MainActivity)
                Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(mainIntent);
                finish(); // Close the SplashActivity so it's not accessible after pressing back
            }
        }, SPLASH_DISPLAY_LENGTH); // 2000ms delay
    }
}
