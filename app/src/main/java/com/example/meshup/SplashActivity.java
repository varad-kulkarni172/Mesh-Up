package com.example.meshup;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private ImageView carouselImageView;
    private int currentImageIndex = 0;
    private static final int DELAY_BETWEEN_IMAGES = 3000; // 3 seconds
    private static final int TOTAL_DURATION = 9000; // 9 seconds total (3 images × 3 seconds)

    // Array of safety instruction images
    private final int[] safetyImages = {
              R.drawable.earthquake_safety,  // Image showing person under table
            R.drawable.flood_safety,       // Image showing electrical safety
            R.drawable.cyclone_safety      // Image showing emergency preparation
    };

    private final Handler handler = new Handler();
    private final Runnable imageRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentImageIndex < safetyImages.length) {
                carouselImageView.setImageResource(safetyImages[currentImageIndex]);
                currentImageIndex++;

                // Schedule next image if not at the end
                if (currentImageIndex < safetyImages.length) {
                    handler.postDelayed(this, DELAY_BETWEEN_IMAGES);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        carouselImageView = findViewById(R.id.carouselImageView);

        // Start the image carousel
        handler.post(imageRunnable);

        // Schedule transition to MainActivity after all images are shown
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(mainIntent);
                finish();
            }
        }, TOTAL_DURATION);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove callbacks to prevent memory leaks
        handler.removeCallbacks(imageRunnable);
    }
}