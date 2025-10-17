package com.example.pocketpetv2;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
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

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private ActivityMainBinding binding;
    private PetManager petManager;
    private Thread petManagerThread;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private FrameLayout petContainer;
    private ImageView petStateImageView;

    private FlingAnimation flingX;
    private FlingAnimation flingY;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(binding.getRoot());

        hideSystemUI();

        //start state machine
        petManager = new PetManager();
        petManagerThread = new Thread(petManager);
        petManagerThread.start();

        setupButtons();

        petContainer = findViewById(R.id.pet_container);
        petStateImageView = findViewById(R.id.pet_state_image_view);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        petContainer.post(() -> {
            final float minX = 0;
            final float maxX = petContainer.getWidth() - petStateImageView.getWidth();
            final float minY = 0;
            final float maxY = petContainer.getHeight() - petStateImageView.getHeight();

            flingX = new FlingAnimation(petStateImageView, DynamicAnimation.X);
            flingX.setMinValue(minX);
            flingX.setMaxValue(maxX);

            flingY = new FlingAnimation(petStateImageView, DynamicAnimation.Y);
            flingY.setMinValue(minY);
            flingY.setMaxValue(maxY);

            //update listener here possibly
        });
    }

    private final Runnable updateImageRunnable = new Runnable() {
        @Override
        public void run() {
            if (petManager == null || binding == null) return;

            PetManager.State currentState = petManager.getState();
            int imageResource;

            switch (currentState) {
                case IDLE:
                    imageResource = R.drawable.gator_idle_left;
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
        uiHandler.post(updateImageRunnable);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHandler.removeCallbacks(updateImageRunnable);
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        /*
        if (flingX != null) flingX.cancel();
        if (flingY != null) flingY.cancel();
         */
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

    private void setupButtons() {
        binding.buttonDeploy.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) {
                Intent overlayIntent = new Intent(this, OverlayService.class);
                startService(overlayIntent);
            } else {
                // ask user for permission
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

    public void updatePetImage(Drawable drawable, PetManager.State state) {
        if (drawable == null) return;
        petStateImageView.setImageDrawable(drawable);


        int viewWidth = petStateImageView.getWidth();
        int viewHeight = petStateImageView.getHeight();
        if (viewWidth == 0 || viewHeight == 0) return;

        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();

        float scale;
        if (drawableWidth * viewHeight > viewWidth * drawableHeight) {
            scale = (float) viewWidth / (float) drawableWidth;
        } else {
            scale = (float) viewHeight / (float) drawableHeight;
        }

        float scaledWidth = drawableWidth * scale;
        float scaledHeight = drawableHeight * scale;

        float dx = 0;
        float dy = (viewHeight - scaledHeight) * 0.5f;

        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate(Math.round(dx), Math.round(dy));
        petStateImageView.setImageMatrix(matrix);
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

    //unfortunately required
    @Override
    public void onSensorChanged(android.hardware.SensorEvent event) {}
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}