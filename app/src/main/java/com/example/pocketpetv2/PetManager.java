package com.example.pocketpetv2;

// import java.util.logging.Handler; // <-- REMOVED THIS LINE
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

    // FIX 2: Added the 'synchronized' keyword
    public synchronized void setState(State newState) {
        // --- START OF NEW LOGIC ---

        // Prevent invalid state transitions that cause flickering
        if (currentState == State.SMACK_RIGHT && newState == State.FALLING_RIGHT) {
            return; // Ignore this change
        }
        if (currentState == State.SMACK_LEFT && newState == State.FALLING_LEFT) {
            return; // Ignore this change
        }

        // If we are currently in a smack state and the new state is IDLE, don't change immediately.
        // Let the timed "recovery" handle it.
        if ((currentState == State.SMACK_LEFT || currentState == State.SMACK_RIGHT) && newState == State.IDLE) {
            return;
        }
        if (newState == State.IDLE) {
            stateHandler.removeCallbacksAndMessages(null);
        }
        // If the new state is a smack, set it and then schedule a recovery back to IDLE
        if (newState == State.SMACK_LEFT || newState == State.SMACK_RIGHT) {
            // Cancel any previously scheduled recovery
            stateHandler.removeCallbacksAndMessages(null);
            this.currentState = newState;
            // Schedule a transition back to IDLE after a short delay
            stateHandler.postDelayed(() -> setState(State.IDLE), 300); // Recover after 300ms
        } else {
            // For any other state change, just apply it directly
            this.currentState = newState;
        }
        // --- END OF NEW LOGIC ---
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
            // This loop is mostly for debugging now or future complex passive behaviors.
            // The System.out.println calls can be removed if not needed.
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
                Thread.sleep(3000); // simulate time between updates
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false; // Important to exit the loop if interrupted
            }
        }
    }
    // This method will now be called from MainActivity when the state changes
    public void updatePetVisuals(MainActivity activity, State state) {
        int resId = R.drawable.gator_idle_left; // Default image
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

