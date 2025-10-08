package com.example.pocketpetv2;
public class PetManager implements Runnable {

    public enum State {
        IDLE,
        FALLING_LEFT,
        FALLING_RIGHT,
        SMACK_LEFT,
        SMACK_RIGHT
    }

    private volatile State currentState = State.IDLE;
    private volatile boolean running = true;

    public void setState(State newState) {
        this.currentState = newState;
    }

    public State getState() {
        return currentState;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            switch (currentState) {
                case IDLE:
                    System.out.println("Pet is idle...");
                    break;
                case FALLING_LEFT:
                    System.out.println("Pet is falling left!");
                    break;
                case FALLING_RIGHT:
                    System.out.println("Pet is falling right!");
                    break;
                case SMACK_LEFT:
                    System.out.println("Pet is hit the left wall!");
                    break;
                case SMACK_RIGHT:
                    System.out.println("Pet is hit the right wall!");
                    break;
            }

            try {
                Thread.sleep(3000); // simulate time between updates
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
