package com.example.pocketpetv2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pocketpetv2.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private PetManager petManager;
    private Thread petManagerThread;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(binding.getRoot());

        // Initialize State Machine
        petManager = new PetManager();
        petManagerThread = new Thread(petManager);
        petManagerThread.start();

        // Setup button listeners
        setupButtons();
    }

    private void setupButtons() {
        binding.buttonDeploy.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) {
                Intent overlayIntent = new Intent(this, OverlayService.class);
                startService(overlayIntent);
            } else {
                // Ask user for permission
                Intent settingsIntent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())
                );
                startActivity(settingsIntent);
            }
        });

        binding.buttonCustomize.setOnClickListener(v ->
                Toast.makeText(this, "Customize clicked", Toast.LENGTH_SHORT).show()
        );

        binding.buttonSettings.setOnClickListener(v ->
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
        );

        // The exit button from your layout
        binding.buttonExit.setOnClickListener(v -> finish());

        // The pet name button from your layout
        binding.buttonPetName.setOnClickListener(v ->
                Toast.makeText(this, "Pet name clicked", Toast.LENGTH_SHORT).show()
        );
    }

    private final Runnable updateImageRunnable = new Runnable() {
        @Override
        public void run() {
            if (petManager == null || binding == null) return;

            PetManager.State currentState = petManager.getState();
            int imageResource = R.drawable.gator_idle01; // Default image

            switch (currentState) {
                case IDLE:
                    imageResource = R.drawable.gator_idle01;
                    break;
                case FALLING_LEFT:
                    imageResource = R.drawable.gator_fall_horizontal_left;
                    break;
                case FALLING_RIGHT:
                    imageResource = R.drawable.gator_fall_horizontal_right;
                    break;
                case SMACK_LEFT:
                    imageResource = R.drawable.gator_wallsmack_left;
                    break;
                case SMACK_RIGHT:
                    imageResource = R.drawable.gator_wallsmack_right;
                    break;
            }
            binding.petStateImageView.setImageResource(imageResource);

            // Keep the image update loop running
            uiHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // Start the UI update runnable when the app is visible
        uiHandler.post(updateImageRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop the UI update runnable when the app is not visible to save resources
        uiHandler.removeCallbacks(updateImageRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (petManager != null) {
            petManager.stop();
        }
        if (petManagerThread != null) {
            petManagerThread.interrupt();
        }
        binding = null;
    }
}
