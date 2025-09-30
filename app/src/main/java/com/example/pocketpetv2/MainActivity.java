package com.example.pocketpetv2;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pocketpetv2.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(binding.getRoot());

        // Button logic
        binding.buttonDeploy.setOnClickListener(v ->
                Toast.makeText(this, "Deploy clicked", Toast.LENGTH_SHORT).show()
        );

        binding.buttonCustomize.setOnClickListener(v ->
                Toast.makeText(this, "Customize clicked", Toast.LENGTH_SHORT).show()
        );

        binding.buttonSettings.setOnClickListener(v ->
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
        );
    }
}
