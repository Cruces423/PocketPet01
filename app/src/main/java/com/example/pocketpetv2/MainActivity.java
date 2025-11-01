package com.example.pocketpetv2;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.pocketpetv2.databinding.ActivityMainBinding;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private ActivityMainBinding binding;
    private FrameLayout petContainer;
    private ImageView petStateImageView;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ProgressBar hungerBar;

    private float currentX, currentY;
    private float velocityX = 0, velocityY = 0;
    private static final float DAMPING = 0.92f;
    private static final float ACCELERATION_SCALE = 2.5f;
    private boolean isDragging = false;
    private float initialTouchX, initialTouchY;
    private float initialPetX, initialPetY;
    private static final float DRAG_DIRECTION_THRESHOLD = 50f;

    public enum State {
        IDLE_LEFT, IDLE_RIGHT, FALLING_VERTICAL, FALLING_LEFT,
        FALLING_RIGHT, SMACK_LEFT, SMACK_RIGHT
    }
    private volatile State currentState = State.IDLE_LEFT;
    private volatile State lastIdleState = State.IDLE_LEFT;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PetData.init(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        hungerBar = findViewById(R.id.hunger_bar); // Link the hunger bar
        petContainer = findViewById(R.id.pet_container);
        petStateImageView = findViewById(R.id.pet_state_image_view);

        hideSystemUI();
        setupButtons();
        updatePetName();
        updateHungerBar();
        setupDragAndDrop();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        scheduleHungerWorker();

        petContainer.post(() -> {
            float minX = 0;
            float maxX = petContainer.getWidth() - petStateImageView.getWidth();
            float minY = 0;
            float maxY = petContainer.getHeight() - petStateImageView.getHeight();

            currentX = (minX + maxX) / 2;
            currentY = maxY;
            petStateImageView.setX(currentX);
            petStateImageView.setY(currentY);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePetName();
        updateHungerBar();

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    private void updateHungerBar() {
        if (hungerBar != null) {
            hungerBar.setProgress(PetData.getHungerPercentage());
        }
    }

    private void scheduleHungerWorker() {
        PeriodicWorkRequest hungerWorkRequest =
                new PeriodicWorkRequest.Builder(HungerWorker.class, 15, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "HungerWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                hungerWorkRequest
        );
    }

    private void setupButtons() {
        binding.buttonExit.setOnClickListener(v -> finish());
        binding.petNameContainer.setOnClickListener(v -> {
            PetData.feedPet(60);
            updateHungerBar();
            Toast.makeText(this, "You fed your pet!", Toast.LENGTH_SHORT).show();
        });
        binding.buttonDeploy.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                startService(new Intent(this, OverlayService.class));
                finish();
            } else {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            }
        });
        binding.buttonCustomize.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CustomizeActivity.class);
            startActivity(intent);
        });
        binding.buttonFeed.setOnClickListener(v -> {
            PetData.feedPet(120);

            updateHungerBar();

            Toast.makeText(this, "You fed your pet!", Toast.LENGTH_SHORT).show();
        });
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.systemBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    private void setupDragAndDrop() {
        petStateImageView.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDragging = true;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    initialPetX = petStateImageView.getX();
                    initialPetY = petStateImageView.getY();
                    petStateImageView.setImageResource(PetData.fallVertical);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (isDragging) {
                        float deltaX = event.getRawX() - initialTouchX;
                        float deltaY = event.getRawY() - initialTouchY;
                        currentX = initialPetX + deltaX;
                        currentY = initialPetY + deltaY;

                        float minX = 0;
                        float maxX = petContainer.getWidth() - petStateImageView.getWidth();
                        float minY = 0;
                        float maxY = petContainer.getHeight() - petStateImageView.getHeight();
                        currentX = Math.max(minX, Math.min(currentX, maxX));
                        currentY = Math.max(minY, Math.min(currentY, maxY));

                        petStateImageView.setX(currentX);
                        petStateImageView.setY(currentY);

                        if (deltaX > DRAG_DIRECTION_THRESHOLD) {
                            petStateImageView.setImageResource(PetData.fallHorizontalRight);
                        } else if (deltaX < -DRAG_DIRECTION_THRESHOLD) {
                            petStateImageView.setImageResource(PetData.fallHorizontalLeft);
                        } else {
                            petStateImageView.setImageResource(PetData.fallVertical);
                        }
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    isDragging = false;
                    velocityX = 0;
                    velocityY = 0;
                    return true;
            }
            return false;
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isDragging) {
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float accelX = -event.values[0];
            float accelY = event.values[1];

            velocityX *= DAMPING;
            velocityY *= DAMPING;

            velocityX += accelX * ACCELERATION_SCALE;
            velocityY += accelY * ACCELERATION_SCALE;

            currentX += velocityX;
            currentY += velocityY;

            float minX = 0;
            float maxX = petContainer.getWidth() - petStateImageView.getWidth();
            float minY = 0;
            float maxY = petContainer.getHeight() - petStateImageView.getHeight();

            if (currentX < minX) {
                currentX = minX;
                velocityX = 0;
            } else if (currentX > maxX) {
                currentX = maxX;
                velocityX = 0;
            }

            if (currentY < minY) {
                currentY = minY;
                velocityY = 0;
            } else if (currentY > maxY) {
                currentY = maxY;
                velocityY = 0;
            }

            petStateImageView.setX(currentX);
            petStateImageView.setY(currentY);

            updatePetState();
            stateImageUpdate();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    private void updatePetName() {
        binding.buttonPetName.setText(PetData.getPetName());
    }

    private boolean isOnWall() {
        float minX = 0;
        float maxX = petContainer.getWidth() - petStateImageView.getWidth();
        float tolerance = 1.0f;
        return currentX <= minX + tolerance || currentX >= maxX - tolerance;
    }

    private boolean isOnGround() {
        float maxY = petContainer.getHeight() - petStateImageView.getHeight();
        float tolerance = 1.0f;
        return currentY >= maxY - tolerance;
    }

    private void updatePetState() {
        float absVelocityX = Math.abs(velocityX);
        float absVelocityY = Math.abs(velocityY);

        if (currentState == State.IDLE_LEFT || currentState == State.IDLE_RIGHT) {
            lastIdleState = currentState;
        }

        switch (currentState) {
            case IDLE_LEFT:
                if (isOnWall()) {
                    currentState = (currentX >= petContainer.getWidth() - petStateImageView.getWidth() - 1.0f) ? State.SMACK_RIGHT : State.SMACK_LEFT;
                }
                else if (!isOnGround()) {
                    if (absVelocityY > absVelocityX) {
                        currentState = State.FALLING_VERTICAL;
                    } else {
                        currentState = (velocityX > 0) ? State.FALLING_RIGHT : State.FALLING_LEFT;
                    }
                }
                break;

            case IDLE_RIGHT:
                if (isOnWall()) {
                    currentState = (currentX <= 1.0f) ? State.SMACK_LEFT : State.SMACK_RIGHT;
                }
                else if (!isOnGround()) {
                    if (absVelocityY > absVelocityX) {
                        currentState = State.FALLING_VERTICAL;
                    } else {
                        currentState = (velocityX > 0) ? State.FALLING_RIGHT : State.FALLING_LEFT;
                    }
                }
                break;

            case FALLING_VERTICAL:
                if (isOnWall()) {
                    currentState = (currentX <= 1.0f) ? State.SMACK_LEFT : State.SMACK_RIGHT;
                }
                else if (isOnGround()) {
                    currentState = lastIdleState;
                }
                else if (absVelocityX > absVelocityY) {
                    currentState = (velocityX > 0) ? State.FALLING_RIGHT : State.FALLING_LEFT;
                }
                break;

            case FALLING_LEFT:
                if (isOnWall()) {
                    currentState = (currentX <= 1.0f) ? State.SMACK_LEFT : State.SMACK_RIGHT;
                }
                else if (isOnGround()) {
                    currentState = State.IDLE_LEFT;
                }
                else if (absVelocityY > absVelocityX) {
                    currentState = State.FALLING_VERTICAL;
                }
                else if (velocityX > 0) {
                    currentState = State.FALLING_RIGHT;
                }
                break;

            case FALLING_RIGHT:
                if (isOnWall()) {
                    currentState = (currentX <= 1.0f) ? State.SMACK_LEFT : State.SMACK_RIGHT;
                }
                else if (isOnGround()) {
                    currentState = State.IDLE_RIGHT;
                }
                else if (absVelocityY > absVelocityX) {
                    currentState = State.FALLING_VERTICAL;
                }
                else if (velocityX < 0) {
                    currentState = State.FALLING_LEFT;
                }
                break;

            case SMACK_LEFT:
                if (!isOnWall()) {
                    if (isOnGround()) {
                        currentState = State.IDLE_LEFT;
                    } else {
                        currentState = State.FALLING_LEFT;
                    }
                }
                break;

            case SMACK_RIGHT:
                if (!isOnWall()) {
                    if (isOnGround()) {
                        currentState = State.IDLE_RIGHT;
                    } else {
                        currentState = State.FALLING_RIGHT;
                    }
                }
                break;

            default:
                currentState = State.IDLE_LEFT;
                break;
        }
    }



    private void stateImageUpdate() {
        int imageResource = 0;
        switch(currentState) {
            case IDLE_LEFT:
                imageResource = PetData.idleLeft;
                break;
            case IDLE_RIGHT:
                imageResource = PetData.idleRight;
                break;
            case FALLING_VERTICAL:
                imageResource = PetData.fallVertical;
                break;
            case FALLING_LEFT:
                imageResource = PetData.fallHorizontalLeft;
                break;
            case FALLING_RIGHT:
                imageResource = PetData.fallHorizontalRight;
                break;
            case SMACK_LEFT:
                imageResource = PetData.wallSmackLeft;
                break;
            case SMACK_RIGHT:
                imageResource = PetData.wallSmackRight;
                break;
        }
        if (imageResource != 0) {
            binding.petStateImageView.setImageResource(imageResource);
        }
    }
}
