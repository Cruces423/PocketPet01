package com.example.pocketpetv2;

import java.util.HashMap;
import java.util.Map;

public class PetRepository {
    // 1. The single, private, static instance of the class
    private static final PetRepository instance = new PetRepository();

    private final Map<String, PetData> availablePets = new HashMap<>();
    private PetData currentPet;

    // 2. A private constructor to prevent anyone else from creating an instance
    private PetRepository() {
        // Load all available pets here
        loadPets();
        // Set the default pet
        currentPet = availablePets.get("Gator");
    }

    // 3. A public, static method to get the single instance
    public static PetRepository getInstance() {
        return instance;
    }

    private void loadPets() {
        // Create Gator data
        PetData gator = new PetData(
                "Gator",
                R.drawable.gator_idle_left,
                R.drawable.gator_idle_right,
                R.drawable.gator_fall_vertical,
                R.drawable.gator_fall_horizontal_left,
                R.drawable.gator_fall_horizontal_right,
                R.drawable.gator_wallsmack_left,
                R.drawable.gator_wallsmack_right
        );
        availablePets.put(gator.name, gator);

        // Create Cat data
        PetData cat = new PetData(
                "Cat",
                R.drawable.cat_idle_left,
                R.drawable.cat_idle_right,
                R.drawable.cat_falling_vertical,
                R.drawable.cat_falling_left,
                R.drawable.cat_falling_right,
                R.drawable.cat_smack_left,
                R.drawable.cat_smack_right
        );
        availablePets.put(cat.name, cat);

        // Add more pets here in the future
    }

    // Public methods to interact with the data
    public PetData getCurrentPet() {
        return currentPet;
    }

    public void setCurrentPet(String petName) {
        if (availablePets.containsKey(petName)) {
            this.currentPet = availablePets.get(petName);
        }
    }
}
