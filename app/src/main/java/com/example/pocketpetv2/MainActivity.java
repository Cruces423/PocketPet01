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
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pocketpetv2.databinding.ActivityMainBinding;

import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends AppCompatActivity implements SensorEventListener, PetManager.PetStateListener {

    private ActivityMainBinding binding;
    private PetManager petManager;
    private Thread petManagerThread;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private FrameLayout petContainer;
    private ImageView petStateImageView;

    private final View.OnTouchListener petContainerTouchListener = new View.OnTouchListener() {
        private boolean isDragging = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // ... (The entire onTouch method code goes here, exactly as it was)
            float touchX = event.getX();
            float touchY = event.getY();

            if (petManager == null) return false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    float petX = petManager.getPositionX();
                    float petY = petManager.getPositionY();
                    int petWidth = petStateImageView.getWidth();
                    int petHeight = petStateImageView.getHeight();

                    if (touchX >= petX && touchX <= petX + petWidth &&
                            touchY >= petY && touchY <= petY + petHeight) {
                        isDragging = true;
                        petManager.startDragging(touchX - (petWidth / 2f), touchY - (petHeight / 2f));
                        return true;
                    }
                    return false;

                case MotionEvent.ACTION_MOVE:
                    if (isDragging) {
                        int width = petStateImageView.getWidth();
                        int height = petStateImageView.getHeight();
                        petManager.updateDrag(touchX - (width / 2f), touchY - (height / 2f));
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isDragging) {
                        isDragging = false;
                        petManager.stopDragging();
                        return true;
                    }
                    break;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(binding.getRoot());

        hideSystemUI();
        setupButtons();

        petContainer = findViewById(R.id.pet_container);
        petStateImageView = findViewById(R.id.pet_state_image_view);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        petContainer.post(() -> {
            petManager = new PetManager();
            petManager.setPetStateListener(this);

            float minX = 0;
            float maxX = petContainer.getWidth() - petStateImageView.getWidth();
            float minY = 0;
            float maxY = petContainer.getHeight() - petStateImageView.getHeight();
            petManager.setBoundaries(minX, maxX, minY, maxY);

            float initialX = (minX + maxX) / 2;
            petStateImageView.setX(initialX);
            petStateImageView.setY(maxY);

            petStateImageView.setImageResource(R.drawable.gator_idle_left);

            petManagerThread = new Thread(petManager);
            petManagerThread.start();
        });

        petContainer.setOnTouchListener(petContainerTouchListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && petManager != null) {
            petManager.updateSensorInput(event.values[0], event.values[1]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onStateChanged(PetManager.State newState, boolean isFacingLeft) {
        int imageResource;
        switch (newState) {
            case DRAGGING_VERTICAL:
                imageResource = R.drawable.gator_fall_vertical;
                break;
            case DRAGGING_LEFT:
                imageResource = R.drawable.gator_fall_horizontal_left;
                break;
            case DRAGGING_RIGHT:
                imageResource = R.drawable.gator_fall_horizontal_right;
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
            case IDLE:
            default:
                imageResource = isFacingLeft ? R.drawable.gator_idle_left : R.drawable.gator_idle_right;
                break;
        }
        binding.petStateImageView.setImageResource(imageResource);
    }

    @Override
    public void onPositionChanged(float newX, float newY) {
        petStateImageView.setX(newX);
        petStateImageView.setY(newY);
    }

    private void setupButtons() {
        binding.buttonDeploy.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) {
                Intent overlayIntent = new Intent(this, OverlayService.class);
                startService(overlayIntent);
            } else {
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

        binding.buttonExit.setOnClickListener(v -> finish());

        binding.buttonPetName.setOnClickListener(v ->
                Toast.makeText(this, "Pet name clicked", Toast.LENGTH_SHORT).show()
        );
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.systemBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
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

