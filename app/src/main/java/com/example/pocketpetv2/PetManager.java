package com.example.pocketpetv2;

import android.os.Handler;
import android.os.Looper;

public class PetManager implements Runnable {

    // --- State and Physics ---
    public enum State {
        IDLE,
        FALLING_VERTICAL,
        FALLING_LEFT,
        FALLING_RIGHT,
        SMACK_LEFT,
        SMACK_RIGHT,
        DRAGGING_VERTICAL,
        DRAGGING_LEFT,
        DRAGGING_RIGHT,
    }
    private volatile State currentState = State.IDLE;
    private float positionX, positionY;
    private float velocityX, velocityY;
    private float minX, maxX, minY, maxY;
    private boolean isFacingLeft = false;
    private static final float GRAVITY = 9.8f * 10; // A stronger gravity for faster effect
    private static final float ACCELEROMETER_SENSITIVITY = 2.75f; // Controls how much the accelerometer affects movement
    private static final float FRICTION = .5f; // Slows the pet down on the ground
    private static final float FALL_THRESHOLD =5.0f; // Minimum distance from ground to be considered falling
    private float lastTouchX, lastTouchY;

    // --- Communication ---
    public interface PetStateListener {
        void onStateChanged(State newState, boolean isFacingLeft);
        void onPositionChanged(float newX, float newY);
    }
    private PetStateListener listener;
    private final Handler stateHandler = new Handler(Looper.getMainLooper());

    // --- Timing and Simulation ---
    private static final long UPDATE_INTERVAL_MS = 16; // Approx. 60 FPS
    private volatile boolean isRunning = false;

    // --- Public Methods ---

    public void setPetStateListener(PetStateListener listener) {
        this.listener = listener;
    }

    public void setBoundaries(float minX, float maxX, float minY, float maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        // Set initial position to be on the ground in the center
        this.positionX = (minX + maxX) / 2;
        this.positionY = maxY;
    }

    public void updateSensorInput(float accelX, float accelY) {
        // Invert the accelerometer's X-axis input and apply sensitivity
        // This makes the pet move in the direction the phone is tilted
        this.velocityX = -accelX * ACCELEROMETER_SENSITIVITY;
        this.velocityY += accelY * ACCELEROMETER_SENSITIVITY;
    }

    @Override
    public void run() {
        isRunning = true;
        while (isRunning) {
            long startTime = System.currentTimeMillis();

            // Only run physics and state updates if we are NOT dragging.
            if (!isDraggingState()) {
                updatePhysics();
                updateState();
            }

            // Notify listener on the UI thread
            stateHandler.post(() -> {
                if (listener != null) {
                    listener.onPositionChanged(positionX, positionY);
                }
            });

            // Control the loop speed
            long sleepTime = UPDATE_INTERVAL_MS - (System.currentTimeMillis() - startTime);
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    isRunning = false;
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void stop() {
        isRunning = false;
    }

    public State getState() {
        return currentState;
    }

    private void updatePhysics() {
        if (isDraggingState()) {
            return; // <-- Exit early, skip all physics
        }

        // Apply gravity if the pet is not on the ground
        if (!isOnGround()) {
            velocityY += GRAVITY * (UPDATE_INTERVAL_MS / 1000f);
        }

        // Update position based on velocity
        float timeDelta = UPDATE_INTERVAL_MS / 1000f;
        positionX += velocityX * timeDelta * 50; // Added a multiplier to make horizontal movement more noticeable
        positionY += velocityY * timeDelta * 50; // Added a multiplier to make vertical movement more noticeable

        // Check for boundary collisions
        if (positionX < minX) {
            positionX = minX;
            //velocityX *= -0.4; // Bounce off the wall
        } else if (positionX > maxX) {
            positionX = maxX;
            //velocityX *= -0.4; // Bounce
        }

        if (positionY < minY) {
            positionY = minY;
            velocityY = 0; // Hit the "ceiling"
        } else if (positionY > maxY) {
            positionY = maxY;
            velocityY = 0; // Land on the "floor"
            velocityX *= FRICTION; // Apply friction when on the ground
        }
    }

    private void updateState() {
        if (isDraggingState()) {
            return; // <-- Exit early, do not allow state changes
        }
        State oldState = currentState; // Store the current state to check for changes

        switch (currentState) {
            case IDLE:
                boolean isHighEnoughToFall = positionY < (maxY - FALL_THRESHOLD);
                if (!isOnGround() && isHighEnoughToFall) {
                    if (Math.abs(velocityX) > Math.abs(velocityY)) {
                        if (velocityX > 0) { isFacingLeft = false; currentState = State.FALLING_RIGHT; }
                        else { isFacingLeft = true; currentState = State.FALLING_LEFT; }
                    } else {
                        currentState = State.FALLING_VERTICAL;
                    }
                }
                break;
            case FALLING_VERTICAL:
                //if on ground -> state = idle
                if(isOnGround()) { currentState = State.IDLE; }
                //if horizontal velocity > vertical velocity -> state = falling horizontal (direction)
                if((Math.abs(velocityX) > Math.abs(velocityY)) && !isOnGround()) {
                    if(velocityX > 0) { isFacingLeft = true; currentState = State.FALLING_RIGHT; }
                    else { isFacingLeft = false; currentState = State.FALLING_LEFT; }
                }
                //if on wall -> state = wall smack
                if(isOnWallLeft()) { currentState = State.SMACK_LEFT; }
                if(isOnWallRight()) { currentState = State.SMACK_RIGHT; }
                break;
            case FALLING_LEFT:
                //if on ground -> state = idle
                if(isOnGround()) { isFacingLeft = false; currentState = State.IDLE; }
                //if on wall -> state = wall smack
                if(isOnWallLeft()) { currentState = State.SMACK_LEFT; }
                //if hor velocity > vert velocity && opposite direction -> state = falling right
                if((Math.abs(velocityX) > Math.abs(velocityY)) && velocityX > 0) { isFacingLeft = false; currentState = State.FALLING_RIGHT; }
                if(Math.abs(velocityY) > Math.abs(velocityX)) { currentState = State.FALLING_VERTICAL; }
                break;
            case FALLING_RIGHT:
                //if on ground -> state = idle
                if(isOnGround()) { isFacingLeft = true; currentState = State.IDLE; }
                //if on wall -> state = wall smack
                if(isOnWallRight()) { currentState = State.SMACK_RIGHT; }
                //if vert velocity > hor velocity && opposite direction -> state = falling left
                if((Math.abs(velocityX) > Math.abs(velocityY)) && velocityX < 0) { isFacingLeft = true; currentState = State.FALLING_LEFT; }
                if(Math.abs(velocityY) > Math.abs(velocityX)) { currentState = State.FALLING_VERTICAL; }
                break;
            case SMACK_LEFT:
                //if on ground -> state = idle
                if(isOnGround()) { isFacingLeft = false; currentState = State.IDLE; }
                //if hor velocity > vert velocity && opposite direction -> state = falling right
                if((Math.abs(velocityX) > Math.abs(velocityY)) && velocityX > 0) { isFacingLeft = false; currentState = State.FALLING_RIGHT; }
                //slide up and down wall while smacked, no need to change to falling vertical
                break;
            case SMACK_RIGHT:
                //if on ground -> state = idle
                if(isOnGround()) { isFacingLeft = true; currentState = State.IDLE; }
                //if vert velocity > hor velocity && opposite direction -> state = falling left
                if((Math.abs(velocityX) > Math.abs(velocityY)) && velocityX < 0) { isFacingLeft = true; currentState = State.FALLING_LEFT; }
                //slide up and down wall while smacked, no need to change to falling vertical
                break;
            default:
                break;
        }

        // If the state has changed, notify the listener on the UI thread
        if (oldState != currentState && listener != null) {
            final State finalState = currentState;
            final boolean finalIsFacingLeft = isFacingLeft;
            stateHandler.post(() -> listener.onStateChanged(finalState, finalIsFacingLeft));
        }
    }

    private boolean isOnGround() {
        // Use a small buffer to account for floating point inaccuracies
        return positionY >= maxY - 1.0f;
    }

    private boolean isOnWallLeft() {
        // True if the pet is at or past the left boundary
        return positionX <= minX;
    }

    private boolean isOnWallRight() {
        // True if the pet is at or past the right boundary
        return positionX >= maxX;
    }

    public void startDragging(float touchX, float touchY) {
        // Stop all existing momentum and turn off gravity's effect during drag
        this.velocityX = 0;
        this.velocityY = 0;
        this.currentState = State.DRAGGING_VERTICAL;

        // Center the pet on the touch point. We'll need the pet's dimensions for this.
        // For now, let's just set the position. We can refine this later.
        this.positionX = touchX;
        this.positionY = touchY;

        // Store the initial touch point for velocity calculation
        this.lastTouchX = touchX;
        this.lastTouchY = touchY;

        // Notify of the state change immediately
        if (listener != null) {
            stateHandler.post(() -> listener.onStateChanged(currentState, isFacingLeft));
        }
    }

    public void updateDrag(float touchX, float touchY) {
        // Only proceed if we are in any of the dragging states
        if (!isDraggingState()) return; // Helper method needed, see below

        State oldState = this.currentState;

        // Calculate velocity for the "throw" when released
        this.velocityX = (touchX - lastTouchX) * 10;
        this.velocityY = (touchY - lastTouchY) * 10;

        // --- NEW LOGIC TO DETERMINE DRAG STATE ---
        // Check if horizontal movement is significantly greater than vertical
        if (Math.abs(this.velocityX) > Math.abs(this.velocityY) * 1.5) { // 1.5 is a tweakable ratio
            this.currentState = this.velocityX < 0 ? State.DRAGGING_LEFT : State.DRAGGING_RIGHT;
            this.isFacingLeft = this.velocityX < 0;
        } else {
            // Otherwise, consider it a vertical drag
            this.currentState = State.DRAGGING_VERTICAL;
        }

        // Update position to follow finger
        this.positionX = touchX;
        this.positionY = touchY;

        // Update the last touch point
        this.lastTouchX = touchX;
        this.lastTouchY = touchY;

        // If the state changed during the drag, notify the listener
        if (oldState != this.currentState && listener != null) {
            stateHandler.post(() -> listener.onStateChanged(currentState, isFacingLeft));
        }
    }

    public void stopDragging() {
        if (isDraggingState()) { // Change this condition
            // Transition to a falling state, physics will take over
            currentState = State.FALLING_VERTICAL;
        }
    }

    private boolean isDraggingState() {
        return currentState == State.DRAGGING_VERTICAL ||
                currentState == State.DRAGGING_LEFT ||
                currentState == State.DRAGGING_RIGHT;
    }

    public float getPositionX() {
        return positionX;
    }

    public float getPositionY() {
        return positionY;
    }
}
