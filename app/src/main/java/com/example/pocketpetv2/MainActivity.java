package com.example.pocketpetv2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
// 1. CORRECTED IMPORT: Use the Android OS Handler
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.pocketpetv2.databinding.ActivityMainBinding;

// REMOVED: java.util.logging.Handler is not needed

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private PetManager petManager;
    private Thread petManagerThread;
    // This variable is no longer needed if using view binding correctly
    // private ImageView petStateImageView;
    private Handler uiHandler = new Handler(Looper.getMainLooper());

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

        //buttons
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
    }

    private final Runnable updateImageRunnable = new Runnable() {
        @Override
        public void run() {
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
            // 2. CORRECTED: Use the binding object to access the ImageView
            if (binding != null) {
                // Assuming the ID of your ImageView in XML is 'pet_state_image_view'
                binding.petStateImageView.setImageResource(imageResource);
            }

            // Schedule the next update.
            uiHandler.postDelayed(this, 500);
        }
    }; // The Runnable definition ends here

    // 3. MOVED: onResume is a method of MainActivity, not the Runnable
    @Override
    protected void onResume() {
        super.onResume();
        // Start updating the image when the activity is visible
        uiHandler.post(updateImageRunnable);
    }

    // 4. MOVED: onPause is a method of MainActivity, not the Runnable
    @Override
    protected void onPause() {
        super.onPause();
        // Stop updating when the activity is not visible to save resources
        uiHandler.removeCallbacks(updateImageRunnable);
    }

    // 5. MOVED: onDestroy is a method of MainActivity, not the Runnable
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanly stop the PetManager thread
        if (petManager != null) {
            petManager.stop();
        }
        if (petManagerThread != null) {
            // 6. CORRECTED: Use the correct variable name
            petManagerThread.interrupt();
        }
        // Set binding to null to avoid memory leaks
        binding = null;
    }
}
