package com.example.pocketpetv2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class OverlayService extends Service implements SensorEventListener {

    private WindowManager windowManager;
    private View overlayView;
    private GestureDetector gestureDetector;
    private WindowManager.LayoutParams params;

    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private boolean isDragging = false;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Handler gravityHandler = new Handler();
    private float sensorX, sensorY;
    private int screenWidth, screenHeight;

    public OverlayService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null);

        // --- Gesture Detector for handling taps ---
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // Prevent tap while dragging
                if (isDragging) return false;
                Toast.makeText(getApplicationContext(), "Pet was tapped!", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        int layout_parms;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layout_parms = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layout_parms = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layout_parms,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // MODIFIED: Removed FLAG_NOT_TOUCH_MODAL to allow ACTION_UP
                PixelFormat.TRANSLUCENT);

        // --- Set OnTouchListener for both tapping and dragging ---
        overlayView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Let the gesture detector handle taps first.
                // It will only detect a tap on ACTION_UP if it's not a drag.
                gestureDetector.onTouchEvent(event);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // --- MODIFIED: Stop gravity on drag start ---
                        gravityHandler.removeCallbacks(gravityRunnable); // Stop the gravity loop
                        isDragging = true; // Mark as dragging

                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // Now, only this code will be responsible for moving the overlay
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        isDragging = false; // Stop dragging

                        // --- MODIFIED: Restart gravity on drag end ---
                        gravityHandler.post(gravityRunnable); // Restart the gravity loop
                        return true;
                }
                return false;
            }
        });

        Button stopButton = overlayView.findViewById(R.id.button_stop_overlay);
        stopButton.setOnClickListener(v -> {
            stopSelf();
        });

        windowManager.addView(overlayView, params);

        // --- NEW: Initialize Gravity ---
        initializeGravity();
    }

    // --- NEW: Gravity and Sensor Methods ---

    private void initializeGravity() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

        // Get screen dimensions to keep the overlay within bounds
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        // Start the animation loop
        gravityHandler.post(gravityRunnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensorX = -event.values[0];
            sensorY = event.values[1];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this implementation
    }

    private final Runnable gravityRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isDragging) {
                params.x += (int) (sensorX * 1.5f);
                params.y += (int) (sensorY * 1.5f);

                int viewWidth = overlayView.getWidth();
                int viewHeight = overlayView.getHeight();

                if (params.x < 0) params.x = 0;
                if (params.y < 0) params.y = 0;
                if (params.x > screenWidth - viewWidth) params.x = screenWidth - viewWidth;
                if (params.y > screenHeight - viewHeight) params.y = screenHeight - viewHeight;

                windowManager.updateViewLayout(overlayView, params);
            }

            // Schedule the next update. 16ms is approximately 60 frames per second.
            gravityHandler.postDelayed(this, 16);
        }
    };

    // --- End of new methods ---

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
        // --- NEW: Clean up gravity resources ---
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        gravityHandler.removeCallbacks(gravityRunnable);
    }
}
