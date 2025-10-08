package com.example.pocketpetv2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;
    private GestureDetector gestureDetector;
    private WindowManager.LayoutParams params; // Make params a class member

    // Variables for dragging
    private float initialTouchX;
    private float initialTouchY;
    private int initialX;
    private int initialY;

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

        // --- Gesture Detector for handling taps and long press ---
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // This is called when a single tap is detected.
                Toast.makeText(getApplicationContext(), "Pet was tapped!", Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                // Called on long press (tap and hold).
                // We will use the main onTouch listener to handle the drag.
                // We can show a toast to indicate dragging has started.
                Toast.makeText(getApplicationContext(), "Drag mode!", Toast.LENGTH_SHORT).show();
            }
        });

        // --- OnTouchListener for handling taps and dragging ---
        overlayView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // First, pass the event to the GestureDetector.
                // If it handles the event (like a tap), we don't need to do anything else.
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }

                // Handle the dragging logic
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Record the initial position.
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // Calculate the new position of the view.
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        // Update the layout with the new coordinates.
                        windowManager.updateViewLayout(overlayView, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        // You could add logic here for when the drag is finished.
                        return true;
                }
                return false;
            }
        });

        int layout_params_type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layout_params_type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layout_params_type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layout_params_type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        // Set initial position of the overlay
        params.x = 0;
        params.y = 100;

        Button stopButton = overlayView.findViewById(R.id.button_stop_overlay);
        stopButton.setOnClickListener(v1 -> {
            stopSelf();
        });

        windowManager.addView(overlayView, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
    }
}



