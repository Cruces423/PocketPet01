package com.example.pocketpetv2;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pocketpetv2.databinding.ActivityMainBinding;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private PetManager petManager;
    private Thread petManagerThread;
    private Thread uiUpdaterThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(binding.getRoot());

        // Initialize State Machine
        petManager = new PetManager();
        petManagerThread = new Thread(petManager);
        petManagerThread.start();

        // Update UI periodically to show current state
        uiUpdaterThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); // update once per second
                } catch (InterruptedException e) {
                    return; // exit thread if interrupted
                }

                runOnUiThread(() -> {
                    if (petManager != null) {
                        binding.mainText.setText("Current State: " + petManager.getState().name());
                    }
                });
            }
        });
        uiUpdaterThread.start();

        //buttons

        binding.buttonDeploy.setOnClickListener(v -> {
            if (android.provider.Settings.canDrawOverlays(this)) {
                Intent overlayIntent = new Intent(this, OverlayService.class);
                startService(overlayIntent);
            } else {
                // Ask user for permission
                Intent settingsIntent = new Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:" + getPackageName())
                );
                startActivity(settingsIntent);
            }
        });

        binding.buttonCustomize.setOnClickListener(v ->
                Toast.makeText(this, "Customize clicked", Toast.LENGTH_SHORT).show()
        );

        binding.buttonSettings.setOnClickListener(v ->
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (petManager != null) {
            petManager.stop();
        }
        if (uiUpdaterThread != null) {
            uiUpdaterThread.interrupt();
        }
    }
}
