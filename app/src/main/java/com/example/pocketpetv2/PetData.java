package com.example.pocketpetv2;

// This is a simple data-holding class.
public class PetData {
    public final String name;
    public final int idleLeft;
    public final int idleRight;
    public final int fallVertical;
    public final int fallHorizontalLeft;
    public final int fallHorizontalRight;
    public final int wallSmackLeft;
    public final int wallSmackRight;

    public PetData(String name, int idleLeft, int idleRight, int fallVertical, int fallHorizontalLeft, int fallHorizontalRight, int wallSmackLeft, int wallSmackRight) {
        this.name = name;
        this.idleLeft = idleLeft;
        this.idleRight = idleRight;
        this.fallVertical = fallVertical;
        this.fallHorizontalLeft = fallHorizontalLeft;
        this.fallHorizontalRight = fallHorizontalRight;
        this.wallSmackLeft = wallSmackLeft;
        this.wallSmackRight = wallSmackRight;
    }
}