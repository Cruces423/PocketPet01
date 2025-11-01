package com.example.pocketpetv2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.example.pocketpetv2.databinding.ActivityCustomizeBinding;

public class CustomizeActivity extends AppCompatActivity {

    private ActivityCustomizeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomizeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonBack.setOnClickListener(v -> {
            finish();
        });

        binding.buttonPetName.setOnClickListener(v -> {
            // TODO: Implement pet name functionality
            showToast("Pet Name button clicked (placeholder)");
        });

        binding.buttonGator.setOnClickListener(v -> {
            // TODO: Implement gator selection
            showToast("Gator button clicked (placeholder)");
        });

        binding.buttonCat.setOnClickListener(v -> {
            // TODO: Implement cat selection
            showToast("Cat button clicked (placeholder)");
        });

        binding.buttonUnknown.setOnClickListener(v -> {
            // TODO: Implement unknown selection
            showToast("Unknown button clicked (placeholder)");
        });

        hideSystemUI();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void hideSystemUI() {
        WindowInsetsControllerCompat windowInsetsController =
                ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            windowInsetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }
}
