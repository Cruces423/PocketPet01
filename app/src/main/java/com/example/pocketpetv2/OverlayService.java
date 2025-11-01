package com.example.pocketpetv2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

public class OverlayService extends Service implements SensorEventListener {

    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams params;
    private ImageView petImageView;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float velocityX = 0, velocityY = 0;
    private static final float DAMPING = 0.92f;
    private static final float ACCELERATION_SCALE = 2.5f;
    private int screenWidth, screenHeight;

    public enum State {
        IDLE_LEFT, IDLE_RIGHT, FALLING_VERTICAL, FALLING_LEFT,
        FALLING_RIGHT, SMACK_LEFT, SMACK_RIGHT
    }
    private volatile State currentState = State.IDLE_LEFT;
    private volatile State lastIdleState = State.IDLE_LEFT;
    private boolean isDragging = false;
    private float initialTouchX, initialTouchY;
    private int initialWindowX, initialWindowY;
    private static final float DRAG_DIRECTION_THRESHOLD = 50f;
    private final Handler handler = new Handler();
    private static final int UPDATE_DELAY = 16; // about 60 FPS

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null);
        petImageView = overlayView.findViewById(R.id.overlay_pet_image);
        Button stopButton = overlayView.findViewById(R.id.button_stop_overlay);
        stopButton.setOnClickListener(v -> stopSelf());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = (screenWidth / 2);
        params.y = (screenHeight / 2);

        windowManager.addView(overlayView, params);
        setupDragAndDrop();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        handler.post(updateRunnable);
    }

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isDragging) {
                updateWindowPosition();
                updatePetState();
                stateImageUpdate();
            }
            handler.postDelayed(this, UPDATE_DELAY);
        }
    };

    private void updateWindowPosition() {
        params.x += (int) velocityX;
        params.y += (int) velocityY;

        int viewWidth = overlayView.getWidth();
        int viewHeight = overlayView.getHeight();

        if (params.x < 0) {
            params.x = 0;
            velocityX = 0;
        } else if (params.x > screenWidth - viewWidth) {
            params.x = screenWidth - viewWidth;
            velocityX = 0;
        }

        if (params.y < 0) {
            params.y = 0;
            velocityY = 0;
        } else if (params.y > screenHeight - viewHeight) {
            params.y = screenHeight - viewHeight;
            velocityY = 0;
        }

        windowManager.updateViewLayout(overlayView, params);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isDragging) return;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float accelX = -event.values[0];
            float accelY = event.values[1];

            velocityX *= DAMPING;
            velocityY *= DAMPING;

            velocityX += accelX * ACCELERATION_SCALE;
            velocityY += accelY * ACCELERATION_SCALE;
        }
    }

    private void setupDragAndDrop() {
        petImageView.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDragging = true;
                    velocityX = 0;
                    velocityY = 0;
                    initialWindowX = params.x;
                    initialWindowY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    petImageView.setImageResource(PetData.fallVertical);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (isDragging) {
                        float deltaX = event.getRawX() - initialTouchX;
                        float deltaY = event.getRawY() - initialTouchY;
                        params.x = initialWindowX + (int) deltaX;
                        params.y = initialWindowY + (int) deltaY;
                        windowManager.updateViewLayout(overlayView, params);

                        if (deltaX > DRAG_DIRECTION_THRESHOLD) {
                            petImageView.setImageResource(PetData.fallHorizontalRight);
                        } else if (deltaX < -DRAG_DIRECTION_THRESHOLD) {
                            petImageView.setImageResource(PetData.fallHorizontalLeft);
                        } else {
                            petImageView.setImageResource(PetData.fallVertical);
                        }
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    isDragging = false;
                    return true;
            }
            return false;
        });
    }

    private boolean isOnWall() {
        int tolerance = 5;
        int viewWidth = overlayView.getWidth();
        return params.x <= tolerance || params.x >= screenWidth - viewWidth - tolerance;
    }

    private boolean isOnGround() {
        int tolerance = 5;
        int viewHeight = overlayView.getHeight();
        return params.y >= screenHeight - viewHeight - tolerance;
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
                    currentState = (params.x >= screenWidth - overlayView.getWidth() - 5) ? State.SMACK_RIGHT : State.SMACK_LEFT;
                } else if (!isOnGround()) {
                    if (absVelocityY > absVelocityX) {
                        currentState = State.FALLING_VERTICAL;
                    } else {
                        currentState = (velocityX > 0) ? State.FALLING_RIGHT : State.FALLING_LEFT;
                    }
                }
                break;

            case IDLE_RIGHT:
                if (isOnWall()) {
                    currentState = (params.x <= 5) ? State.SMACK_LEFT : State.SMACK_RIGHT;
                } else if (!isOnGround()) {
                    if (absVelocityY > absVelocityX) {
                        currentState = State.FALLING_VERTICAL;
                    } else {
                        currentState = (velocityX > 0) ? State.FALLING_RIGHT : State.FALLING_LEFT;
                    }
                }
                break;

            case FALLING_VERTICAL:
                if (isOnWall()) {
                    currentState = (params.x <= 5) ? State.SMACK_LEFT : State.SMACK_RIGHT;
                } else if (isOnGround()) {
                    currentState = lastIdleState;
                } else if (absVelocityX > absVelocityY) {
                    currentState = (velocityX > 0) ? State.FALLING_RIGHT : State.FALLING_LEFT;
                }
                break;

            case FALLING_LEFT:
                if (isOnWall()) {
                    currentState = (params.x <= 5) ? State.SMACK_LEFT : State.SMACK_RIGHT;
                } else if (isOnGround()) {
                    currentState = State.IDLE_LEFT;
                } else if (absVelocityY > absVelocityX) {
                    currentState = State.FALLING_VERTICAL;
                } else if (velocityX > 0) {
                    currentState = State.FALLING_RIGHT;
                }
                break;

            case FALLING_RIGHT:
                if (isOnWall()) {
                    currentState = (params.x <= 5) ? State.SMACK_LEFT : State.SMACK_RIGHT;
                } else if (isOnGround()) {
                    currentState = State.IDLE_RIGHT;
                } else if (absVelocityY > absVelocityX) {
                    currentState = State.FALLING_VERTICAL;
                } else if (velocityX < 0) {
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
            case IDLE_LEFT: imageResource = PetData.idleLeft; break;
            case IDLE_RIGHT: imageResource = PetData.idleRight; break;
            case FALLING_VERTICAL: imageResource = PetData.fallVertical; break;
            case FALLING_LEFT: imageResource = PetData.fallHorizontalLeft; break;
            case FALLING_RIGHT: imageResource = PetData.fallHorizontalRight; break;
            case SMACK_LEFT: imageResource = PetData.wallSmackLeft; break;
            case SMACK_RIGHT: imageResource = PetData.wallSmackRight; break;
        }
        if (imageResource != 0) {
            petImageView.setImageResource(imageResource);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
