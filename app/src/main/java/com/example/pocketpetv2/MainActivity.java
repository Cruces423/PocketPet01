package com.example.pocketpetv2;

// FIX 1: ADDED REQUIRED IMPORTS
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener; // <-- ADD THIS
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;

import com.example.pocketpetv2.databinding.ActivityMainBinding;

// FIX 2: IMPLEMENT THE SENSOR EVENT LISTENER INTERFACE
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private ActivityMainBinding binding;
    private PetManager petManager;
    private Thread petManagerThread;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float GRAVITY_MULTIPLIER = 1000.0f;
    private FrameLayout petContainer;
    private ImageView petStateImageView;
    private FlingAnimation flingX;
    private FlingAnimation flingY;
    private static final float GRAVITY_THRESHOLD = 0.5f;
    private float maxY = Float.MAX_VALUE;


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

        // --- 1. Initialize Views and Sensors ---
        petContainer = findViewById(R.id.pet_container);
        petStateImageView = findViewById(R.id.pet_state_image_view);

        // FIX 3: USE THE CORRECT 'Context' CLASS
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            // Get the accelerometer sensor
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // --- 2. Set up Physics-based Animations ---
        // We need to wait for the layout to be drawn to get the correct boundaries
        petContainer.post(() -> {
            // --- CORRECTED AND CLEANED UP INITIALIZATION ---
            final float minX = 0;
            final float maxX = petContainer.getWidth() - petStateImageView.getWidth();
            final float minY = 0;
            // Assign the calculated max Y value to our class-level variable
            this.maxY = petContainer.getHeight() - petStateImageView.getHeight();

            // --- Initialize X Animation ---
            flingX = new FlingAnimation(petStateImageView, DynamicAnimation.X);
            flingX.setMinValue(minX);
            flingX.setMaxValue(maxX);

            // --- Initialize Y Animation ---
            flingY = new FlingAnimation(petStateImageView, DynamicAnimation.Y);
            flingY.setMinValue(minY);
            flingY.setMaxValue(this.maxY); // Use the class variable

            // --- SETUP LISTENERS ---

            // Horizontal End Listener (Collision)
            flingX.addEndListener((animation, canceled, value, velocity) -> {
                if (value >= maxX) {
                    petManager.setState(PetManager.State.SMACK_RIGHT);
                } else if (value <= minX) {
                    petManager.setState(PetManager.State.SMACK_LEFT);
                }
            });

            // Vertical End Listener (Collision)
            flingY.addEndListener((animation, canceled, value, velocity) -> {
                // Use the CLASS VARIABLE 'this.maxY' for the check
                if (value >= this.maxY) {
                    petManager.setState(PetManager.State.IDLE);
                }
            });

            // Horizontal Update Listener (State Change)
            flingX.addUpdateListener((animation, value, velocity) -> {
                float velocityThreshold = 25.0f;
                if (velocity > velocityThreshold) {
                    petManager.setState(PetManager.State.FALLING_RIGHT);
                } else if (velocity < -velocityThreshold) {
                    petManager.setState(PetManager.State.FALLING_LEFT);
                }
            });

            // Vertical Update Listener (State Change)
            flingY.addUpdateListener((animation, value, velocity) -> {
                float velocityThreshold = 25.0f;
                if (velocity < -velocityThreshold) {
                    petManager.setState(PetManager.State.FALLING_VERTICAL);
                }
            });
        });
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
                case FALLING_VERTICAL:
                    imageResource = R.drawable.gator_fall_vertical;
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
            uiHandler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // Start the UI update runnable when the app is visible
        uiHandler.post(updateImageRunnable);
        // --- 3. Start Listening for Sensor Data ---
        // Register the listener when the app is in the foreground
        if (accelerometer != null) {
            // The 'this' keyword now correctly refers to a SensorEventListener
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop the UI update runnable when the app is not visible to save resources
        uiHandler.removeCallbacks(updateImageRunnable);
        // --- 4. Stop Listening to Save Battery ---
        // Unregister the listener when the app goes into the background
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        // Cancel any ongoing animation
        if (flingX != null) flingX.cancel();
        if (flingY != null) flingY.cancel();
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        // --- 5. React to Sensor Changes ---
        // This method is called every time the accelerometer detects a change
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Check if animations are initialized (they are created after the layout pass)
            if (flingX == null || flingY == null) {
                return;
            }

            // The accelerometer values are typically reversed for screen coordinates.
            // Get the raw accelerometer values.
            // value[0] is for the X-axis (left/right tilt)
            // value[1] is for the Y-axis (up/down tilt)
            float accelX = event.values[0];
            float accelY = event.values[1];

            // Initialize velocities to zero
            float velocityX = 0;
            float velocityY = 0;

            // Only apply velocity if the tilt is beyond our threshold
            // We check the absolute value to handle both positive and negative tilt
            // --- HORIZONTAL GRAVITY ---
            if (Math.abs(accelX) > GRAVITY_THRESHOLD) {
                velocityX = accelX * -1 * GRAVITY_MULTIPLIER;
            }

            // --- VERTICAL GRAVITY (MODIFIED LOGIC) ---
            if (Math.abs(accelY) > GRAVITY_THRESHOLD) {
                // Get the current vertical position of the pet
                float currentY = petStateImageView.getTranslationY();

                // Check for downward gravity pull.
                // A positive accelY means gravity is pulling the phone "down"
                if (accelY > 0) {
                    // *** THIS IS THE KEY FIX ***
                    // Only apply downward velocity if the pet is NOT on the floor.
                    // We add a small buffer (e.g., 5 pixels) to account for float inaccuracies.
                    if (currentY < maxY - 5) {
                        velocityY = accelY * GRAVITY_MULTIPLIER;
                    }
                } else {
                    // For upward gravity, always apply it (to make the pet "jump")
                    velocityY = accelY * GRAVITY_MULTIPLIER;
                }
            }

            // Update the velocity of the fling animations
            flingX.setStartVelocity(velocityX);
            flingY.setStartVelocity(velocityY);

            // Start the animations if they aren't already running
            if (!flingX.isRunning()) {
                flingX.start();
            }
            if (!flingY.isRunning()) {
                flingY.start();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this implementation, but required by the interface.
    }
}
