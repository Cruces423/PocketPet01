package com.example.pocketpetv2;import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class CustomizeActivity extends AppCompatActivity {

    private ImageView petPreviewImageView;
    private Button backButton;
    private Button petNameButton;
    private Spinner petSelectionSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize);

        hideSystemUI();

        petPreviewImageView = findViewById(R.id.petPreviewImageView);
        backButton = findViewById(R.id.button_back);
        petNameButton = findViewById(R.id.petNameButton);
        petSelectionSpinner = findViewById(R.id.pet_selection_spinner);

        setupPetSelectionSpinner();

        updatePreviewImage();
        updatePetNameButton();

        backButton.setOnClickListener(v -> finish());
        petNameButton.setOnClickListener(v -> showPetNameDialog());
    }

    private void setupPetSelectionSpinner() {
        List<String> petTypes = new ArrayList<>();
        petTypes.add("gator");
        petTypes.add("cat");
        petTypes.add("penguin");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, petTypes);

        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);

        petSelectionSpinner.setAdapter(adapter);

        String currentPet = PetData.getPetType();
        int currentSelection = petTypes.indexOf(currentPet);
        if (currentSelection >= 0) {
            petSelectionSpinner.setSelection(currentSelection);
        }

        petSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedPet = parent.getItemAtPosition(position).toString();
                PetData.setPetType(selectedPet);
                updatePreviewImage();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //useless
            }
        });
    }

    private void showPetNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Pet Name");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter new name");
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString();
            if (!newName.isEmpty()) {
                PetData.setPetName(newName);
                updatePetNameButton();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.systemBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    private void updatePetNameButton() {
        petNameButton.setText(PetData.getPetName());
    }

    private void updatePreviewImage() {
        String currentPetType = PetData.getPetType();

        if ("gator".equals(currentPetType)) {
            petPreviewImageView.setImageResource(R.drawable.gator_idle_left);
        } else if ("cat".equals(currentPetType)) {
            petPreviewImageView.setImageResource(R.drawable.cat_idle_left);
        } else if ("penguin".equals(currentPetType)) {
            petPreviewImageView.setImageResource(R.drawable.peng_idle_left);
        } else {
            petPreviewImageView.setImageResource(R.drawable.gator_idle_left);
        }
    }
}
