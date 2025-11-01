package com.example.pocketpetv2;

import android.content.Context;
import android.content.SharedPreferences;

public class PetData {

    public static String name;
    public static int idleLeft;
    public static int idleRight;
    public static int fallVertical;
    public static int fallHorizontalLeft;
    public static int fallHorizontalRight;
    public static int wallSmackLeft;
    public static int wallSmackRight;
    public static int currentHunger;
    public static final int maxHunger = 720;

    private static final String PREFS_NAME = "PetPrefs";
    private static final String PET_NAME_KEY = "petName";
    private static final String PET_TYPE_KEY = "petType";
    private static final String CURRENT_HUNGER_KEY = "currentHunger";
    private static final String LAST_UPDATED_KEY = "lastUpdatedTime";
    private static SharedPreferences sharedPreferences;

    public static void init(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            name = sharedPreferences.getString(PET_NAME_KEY, "Pet");
            String petType = sharedPreferences.getString(PET_TYPE_KEY, "gator");
            setPetType(petType);

            calculateOfflineHungerLoss();
        }
    }

    private static void calculateOfflineHungerLoss() {
        long lastUpdatedTime = sharedPreferences.getLong(LAST_UPDATED_KEY, System.currentTimeMillis());
        long currentTime = System.currentTimeMillis();
        long timeElapsedMillis = currentTime - lastUpdatedTime;
        int minutesElapsed = (int) (timeElapsedMillis / (1000 * 60));

        int savedHunger = sharedPreferences.getInt(CURRENT_HUNGER_KEY, maxHunger);

        savedHunger = maxHunger / 2;

        if (minutesElapsed > 0) {
            currentHunger = Math.max(0, savedHunger - minutesElapsed);
        } else {
            currentHunger = savedHunger;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(CURRENT_HUNGER_KEY, currentHunger);
        editor.putLong(LAST_UPDATED_KEY, currentTime);
        editor.apply();
    }

    public static void feedPet(int amount) {
        currentHunger = Math.min(maxHunger, currentHunger + amount);
        saveHungerState();
    }

    public static int getHungerPercentage() {
        if (maxHunger <= 0) return 0;
        return (int) (((float) currentHunger / maxHunger) * 100);
    }

    private static void saveHungerState() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(CURRENT_HUNGER_KEY, currentHunger);
        editor.putLong(LAST_UPDATED_KEY, System.currentTimeMillis());
        editor.apply();
    }

    public static void setPetName(String newName) {
        name = newName;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PET_NAME_KEY, newName);
        editor.apply();
    }

    public static String getPetName() {
        return name;
    }

    public static String getPetType() {
        return sharedPreferences.getString(PET_TYPE_KEY, "gator");
    }

    public static void setPetType(String petType) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PET_TYPE_KEY, petType);
        editor.apply();

        switch (petType) {
            case "gator":
                idleLeft = R.drawable.gator_idle_left;
                idleRight = R.drawable.gator_idle_right;
                fallVertical = R.drawable.gator_fall_vertical;
                fallHorizontalLeft = R.drawable.gator_fall_horizontal_left;
                fallHorizontalRight = R.drawable.gator_fall_horizontal_right;
                wallSmackLeft = R.drawable.gator_wallsmack_left;
                wallSmackRight = R.drawable.gator_wallsmack_right;
                break;
            case "cat":
                idleLeft = R.drawable.cat_idle_left;
                idleRight = R.drawable.cat_idle_right;
                fallVertical = R.drawable.cat_falling_vertical;
                fallHorizontalLeft = R.drawable.cat_falling_left;
                fallHorizontalRight = R.drawable.cat_falling_right;
                wallSmackLeft = R.drawable.cat_smack_left;
                wallSmackRight = R.drawable.cat_smack_right;
                break;
            case "penguin":
                idleLeft = R.drawable.peng_idle_left;
                idleRight = R.drawable.peng_idle_right;
                fallVertical = R.drawable.peng_falling_vertical;
                fallHorizontalLeft = R.drawable.peng_falling_left;
                fallHorizontalRight = R.drawable.peng_falling_right;
                wallSmackLeft = R.drawable.peng_smack_left;
                wallSmackRight = R.drawable.peng_smack_right;
                break;
            default:
                idleLeft = R.drawable.gator_idle_left;
                idleRight = R.drawable.gator_idle_right;
                fallVertical = R.drawable.gator_fall_vertical;
                fallHorizontalLeft = R.drawable.gator_fall_horizontal_left;
                fallHorizontalRight = R.drawable.gator_fall_horizontal_right;
                wallSmackLeft = R.drawable.gator_wallsmack_left;
                wallSmackRight = R.drawable.gator_wallsmack_right;
                break;
        }
    }
}
