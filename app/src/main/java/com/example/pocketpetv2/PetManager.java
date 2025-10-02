package com.example.pocketpetv2;
public class PetManager implements Runnable {

    public enum State {
        IDLE,
        ACTIVE,
        SLEEP
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
                    // Do idle logic
                    System.out.println("Pet is idle...");
                    break;
                case ACTIVE:
                    // Do active logic
                    System.out.println("Pet is active!");
                    break;
                case SLEEP:
                    // Do sleep logic
                    System.out.println("Pet is sleeping...");
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
