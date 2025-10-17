package com.example.pocketpetv2;

// FIX1: ADDED REQUIRED IMPORTS
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;import android.hardware.SensorEvent;
import android.hardware.SensorEventListener; // <-- ADD THIS
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;

import com.example.pocketpetv2.databinding.ActivityMainBinding;

import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import androidx.appcompat.app.AppCompatActivity;

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

    // --- NEW: Add a variable to track the last facing direction ---
    private boolean isFacingLeft = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(binding.getRoot());

        hideSystemUI();

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
                // *** FIX: PREVENT FLICKER FROM IDLE TO SMACK ***
                PetManager.State currentState = petManager.getState();
                float smackVelocityThreshold = 100f; // Velocity needed to trigger a smack from idle

                // Only allow a smack if the pet is NOT idle, OR if it IS idle but hits the wall hard enough.
                if (currentState != PetManager.State.IDLE || Math.abs(velocity) > smackVelocityThreshold) {
                    if (value >= maxX) {
                        changePetState(PetManager.State.SMACK_RIGHT);
                        isFacingLeft = false; // It smacked right, so it's facing left now
                    } else if (value <= minX) {
                        changePetState(PetManager.State.SMACK_LEFT);
                        isFacingLeft = true; // It smacked left, so it's facing right now
                    }
                }
            });

            // Vertical End Listener (Collision)
            flingY.addEndListener((animation, canceled, value, velocity) -> {
                // Use the CLASS VARIABLE 'this.maxY' for the check
                // A small buffer to ensure it's properly on the ground
                if (value >= this.maxY - 5f) {
                    changePetState(PetManager.State.IDLE);
                }
            });

            // Horizontal Update Listener (State Change)
            flingX.addUpdateListener((animation, value, velocity) -> {
                float velocityThreshold = 25.0f;
                float idleVelocityThreshold = 15.0f; // Small dead zone for velocity
                PetManager.State currentState = petManager.getState();

                // *** NEW FIX: Check if on the ground and sliding ***
                float currentY = petStateImageView.getY();
                boolean onGround = currentY >= this.maxY - 5f;

                if (onGround && Math.abs(velocity) < idleVelocityThreshold) {
                    changePetState(PetManager.State.IDLE);
                    return; // Exit early, we are idle on the ground
                }


                if (velocity > velocityThreshold && currentState != PetManager.State.SMACK_RIGHT) {
                    changePetState(PetManager.State.FALLING_RIGHT);
                    isFacingLeft = false;
                } else if (velocity < -velocityThreshold && currentState != PetManager.State.SMACK_LEFT) {
                    changePetState(PetManager.State.FALLING_LEFT);
                    isFacingLeft = true;
                } else if (currentState == PetManager.State.IDLE) {
                    // Only change facing direction if idle and velocity is significant
                    if (velocity > idleVelocityThreshold) {
                        isFacingLeft = false;
                    } else if (velocity < -idleVelocityThreshold) {
                        isFacingLeft = true;
                    }
                }
            });


            // Vertical Update Listener (State Change)
            flingY.addUpdateListener((animation, value, velocity) -> {
                float velocityThreshold = 25.0f;
                // *** FIX: ADDED STATE CHECKS TO PREVENT FLICKERING AGAINST WALLS ***
                PetManager.State currentState = petManager.getState();
                boolean isSmacking = (currentState == PetManager.State.SMACK_LEFT || currentState == PetManager.State.SMACK_RIGHT);

                if (velocity < -velocityThreshold && !isSmacking) {
                    changePetState(PetManager.State.FALLING_VERTICAL);
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

    private void changePetState(PetManager.State newState) {
        // FIX: Call the PetManager's setState method, NOT itself.
        petManager.setState(newState);

        // Now call the visual update method
        petManager.updatePetVisuals(this, newState);
    }
    //new
    public void updatePetImage(Drawable drawable, PetManager.State state) {
        if (drawable == null) return;

        // Set the drawable on the ImageView
        petStateImageView.setImageDrawable(drawable);

        // Get the dimensions of the view and the image
        int viewWidth = petStateImageView.getWidth();
        int viewHeight = petStateImageView.getHeight();
        if (viewWidth == 0 || viewHeight == 0) return; // Not laid out yet

        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();

        // Calculate the scale factor to fit the image without distortion
        float scale;
        if (drawableWidth * viewHeight > viewWidth * drawableHeight) {
            // Image is wider than the view
            scale = (float) viewWidth / (float) drawableWidth;
        } else {
            // Image is taller than or same aspect as the view
            scale = (float) viewHeight / (float) drawableHeight;
        }

        // Calculate the scaled image dimensions
        float scaledWidth = drawableWidth * scale;
        float scaledHeight = drawableHeight * scale;

        // Determine the translation (movement) based on the state
        float dx = 0; // Horizontal translation
        float dy = (viewHeight - scaledHeight) * 0.5f; // Center vertically by default

        switch (state) {
            case FALLING_LEFT:
            case SMACK_LEFT:
                // Align to the left edge
                dx = 0;
                break;

            case FALLING_RIGHT:
            case SMACK_RIGHT:
                // Align to the right edge
                dx = viewWidth - scaledWidth;
                break;

            case IDLE:
            case FALLING_VERTICAL:
            default:
                // Center horizontally
                dx = (viewWidth - scaledWidth) * 0.5f;
                break;
        }

        // Create and apply the matrix
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate(Math.round(dx), Math.round(dy));
        petStateImageView.setImageMatrix(matrix);
    }

    private final Runnable updateImageRunnable = new Runnable() {
        @Override
        public void run() {
            if (petManager == null || binding == null) return;

            PetManager.State currentState = petManager.getState();
            int imageResource; // Default will be handled inside the switch

            switch (currentState) {
                case IDLE:
                    // --- MODIFIED: Choose idle image based on last direction ---
                    if (isFacingLeft) {
                        imageResource = R.drawable.gator_idle_left;
                    } else {
                        // Assumes you have a 'gator_idle_right.png' in your drawable folder
                        imageResource = R.drawable.gator_idle_right;
                    }
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
                default:
                    // Default to a known safe state in case of an unexpected enum value
                    imageResource = R.drawable.gator_idle_left;
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

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {            // Hides the status and navigation bars
                insetsController.hide(WindowInsets.Type.systemBars());
                // Makes the app fullscreen and allows gestures to temporarily reveal the bars
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // For older Android versions (legacy approach)
            // *** FIX: REMOVED THE INCORRECT PREFIX ***
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

}
