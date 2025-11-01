package com.example.pocketpetv2;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

// The import for the correct Result class might be needed if not using the full path.
import androidx.work.ListenableWorker.Result;

public class HungerWorker extends Worker {

    private static final String PREFS_NAME = "PetPrefs";
    private static final String CURRENT_HUNGER_KEY = "currentHunger";
    private static final String LAST_UPDATED_KEY = "lastUpdatedTime";

    public HungerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        int maxHunger = PetData.maxHunger;
        int currentHunger = sharedPreferences.getInt(CURRENT_HUNGER_KEY, maxHunger);
        long lastUpdatedTime = sharedPreferences.getLong(LAST_UPDATED_KEY, System.currentTimeMillis());
        long currentTime = System.currentTimeMillis();

        long timeElapsedMillis = currentTime - lastUpdatedTime;
        int minutesElapsed = (int) (timeElapsedMillis / (1000 * 60));

        if (minutesElapsed > 0) {
            int newHunger = Math.max(0, currentHunger - minutesElapsed);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(CURRENT_HUNGER_KEY, newHunger);
            editor.putLong(LAST_UPDATED_KEY, currentTime);
            editor.apply();
        }

        return Result.success();
    }
}

