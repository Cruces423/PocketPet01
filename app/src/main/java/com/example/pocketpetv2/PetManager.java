package com.example.pocketpetv2;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

public class PetManager implements Runnable {

    public enum State {
        IDLE,
        FALLING_VERTICAL,
        FALLING_LEFT,
        FALLING_RIGHT,
        SMACK_LEFT,
        SMACK_RIGHT
    }

    private volatile State currentState = State.IDLE;
    private volatile boolean running = true;
    private final Handler stateHandler = new Handler(Looper.getMainLooper());

    public synchronized void setState(State newState) {

        if (currentState == State.SMACK_RIGHT && newState == State.FALLING_RIGHT) {
            return;
        }
        if (currentState == State.SMACK_LEFT && newState == State.FALLING_LEFT) {
            return;
        }
        if ((currentState == State.SMACK_LEFT || currentState == State.SMACK_RIGHT) && newState == State.IDLE) {
            return;
        }
        if (newState == State.IDLE) {
            stateHandler.removeCallbacksAndMessages(null);
        }
        if (newState == State.SMACK_LEFT || newState == State.SMACK_RIGHT) {
            stateHandler.removeCallbacksAndMessages(null);
            this.currentState = newState;
            stateHandler.postDelayed(() -> setState(State.IDLE), 300); // Recover after 300ms
        } else {
            this.currentState = newState;
        }
    }

    public State getState() {
        return currentState;
    }

    public void stop() {
        running = false;
        // FIX 3: Added cleanup for the handler
        stateHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void run() {
        while (running) {
            switch (currentState) {
                case IDLE:
                    System.out.println("Pet is idle...");
                    break;
                case FALLING_VERTICAL:
                    System.out.println("Pet is falling!");
                    break;
                case FALLING_LEFT:
                    System.out.println("Pet is falling left!");
                    break;
                case FALLING_RIGHT:
                    System.out.println("Pet is falling right!");
                    break;
                case SMACK_LEFT:
                    System.out.println("Pet hit the left wall!");
                    break;
                case SMACK_RIGHT:
                    System.out.println("Pet hit the right wall!");
                    break;
            }

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }
    public void updatePetVisuals(MainActivity activity, State state) {
        int resId = R.drawable.gator_idle_left;
        switch (state) {
            case IDLE:
                resId = R.drawable.gator_idle_left;
                break;
            case FALLING_VERTICAL:
                resId = R.drawable.gator_fall_vertical;
                break;
            case FALLING_LEFT:
                resId = R.drawable.gator_fall_horizontal_left;
                break;
            case FALLING_RIGHT:
                resId = R.drawable.gator_fall_horizontal_right;
                break;
            case SMACK_LEFT:
                resId = R.drawable.gator_wallsmack_left;
                break;
            case SMACK_RIGHT:
                resId = R.drawable.gator_wallsmack_right;
                break;
        }

        // Get the drawable and call the new method in MainActivity
        Drawable drawable = activity.getDrawable(resId);
        activity.runOnUiThread(() -> activity.updatePetImage(drawable, state));
    }
}