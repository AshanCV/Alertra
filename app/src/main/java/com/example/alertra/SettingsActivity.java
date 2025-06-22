package com.example.alertra;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {
    private SeekBar sensitivityBar;
    private SwitchCompat smsSwitch;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sensitivityBar = findViewById(R.id.sensitivityBar);
        smsSwitch = findViewById(R.id.smsSwitch);
        prefs = getSharedPreferences("AlertraPrefs", MODE_PRIVATE);

        // Load saved settings
        sensitivityBar.setProgress(prefs.getInt("sensitivity", 25));
        smsSwitch.setChecked(prefs.getBoolean("smsEnabled", true));

        sensitivityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prefs.edit().putInt("sensitivity", progress).apply();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Optional:  start tracking logic
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Optional:  stop tracking logic
            }
        });

        smsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("smsEnabled", isChecked).apply();
        });
    }
}
