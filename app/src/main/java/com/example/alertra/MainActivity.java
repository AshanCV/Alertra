package com.example.alertra;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.Locale;
import android.animation.ValueAnimator;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;



public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    SensorManager sensorManager;
    Sensor accSensor, gyroSensor;
    TextView tvAcc, tvGyro;
    EditText editPhone;
    Button btnSave;
    String savedNumber = "";
    float fallThreshold = 25;
    private long lastSmsTime = 0;
    private static final long SMS_DEBOUNCE_TIME = 30000; // 30 seconds
    private int fallCount = 0;
    private static final int FALL_COUNT_THRESHOLD = 3; // Require 3 detections
    private static final long FALL_WINDOW_MS = 2000; // 2-second window
    private long lastFallTime = 0;
    private float[] lastAccel = new float[3];
    private float lastGx, lastGy, lastGz;
    private float gyroThreshold = 5f; // rad/s, adjust based on testing
    private CardView cardSensor;
    private boolean isFallVisualActive = false;
    private LinearLayout alertContainer;
    private Button btnResetAlert;
    private float getFallThreshold() {
        return prefs.getInt("sensitivity", 25);
    }

    private boolean isSmsEnabled() {
        return prefs.getBoolean("smsEnabled", true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("AlertraPrefs", MODE_PRIVATE);

        tvAcc = findViewById(R.id.tv_acc);
        tvGyro = findViewById(R.id.tv_gyro);
        editPhone = findViewById(R.id.editPhone);
        btnSave = findViewById(R.id.btnSave);
        alertContainer = findViewById(R.id.alertContainer);
        btnResetAlert = findViewById(R.id.btnResetAlert);
        btnResetAlert.setOnClickListener(v -> {
            alertContainer.setVisibility(View.GONE);
        });

        Button btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        cardSensor = findViewById(R.id.cardSensor);

        savedNumber = prefs.getString("phone", "");
        editPhone.setText(savedNumber);

        btnSave.setOnClickListener(v -> {
            savedNumber = editPhone.getText().toString();
            prefs.edit().putString("phone", savedNumber).apply();
        });

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE},
                1);

        sensorManager.registerListener(sensorListener, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorListener, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                lastAccel[0] = event.values[0];
                lastAccel[1] = event.values[1];
                lastAccel[2] = event.values[2];
                tvAcc.setText(String.format(Locale.ENGLISH,"Accelerometer:\n" +
                                "X=%.2f\n" +
                                "Y=%.2f\n" +
                                "Z=%.2f",
                        lastAccel[0], lastAccel[1], lastAccel[2]));
            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                lastGx = event.values[0];
                lastGy = event.values[1];
                lastGz = event.values[2];
                tvGyro.setText(String.format(Locale.ENGLISH,"Gyroscope:\n" +
                                "X=%.2f\n" +
                                "Y=%.2f\n" +
                                "Z=%.2f",
                        lastGx, lastGy, lastGz));
            }

            // Fall detection logic (using both sensors)
            double totalAcc = Math.sqrt(lastAccel[0] * lastAccel[0] + lastAccel[1] * lastAccel[1] + lastAccel[2] * lastAccel[2]);
            double totalGyro = Math.sqrt(lastGx * lastGx + lastGy * lastGy + lastGz * lastGz);
            float currentThreshold = getFallThreshold();
            if (totalAcc > currentThreshold && totalGyro > gyroThreshold) {
                long now = System.currentTimeMillis();
                if (now - lastFallTime > FALL_WINDOW_MS) {
                    fallCount = 0; // Reset if too much time has passed
                }
                fallCount++;
                lastFallTime = now;
                if (fallCount >= FALL_COUNT_THRESHOLD) {
                    if (isSmsEnabled()){
                        sendSMS("Alertra Alert: Fall Detected!");
                    }
                    showFallVisualAlert();
                    showFallAlert();
                    fallCount = 0;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };


    private void sendSMS(String msg) {
        long now = System.currentTimeMillis();
        if (!savedNumber.isEmpty() && ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED && now - lastSmsTime > SMS_DEBOUNCE_TIME) {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(savedNumber, null, msg, null, null);
            lastSmsTime = now;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(sensorListener, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorListener, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void showFallVisualAlert() {
        if (isFallVisualActive) return; // Prevent overlapping animations
        isFallVisualActive = true;
        int colorFrom = ContextCompat.getColor(this, android.R.color.white);
        int colorTo = Color.parseColor("#FFF59D"); // Light yellow for alert

        ValueAnimator colorAnim = ValueAnimator.ofArgb(colorFrom, colorTo);
        colorAnim.setDuration(400); // 400ms animation
        colorAnim.addUpdateListener(animator -> cardSensor.setCardBackgroundColor((int) animator.getAnimatedValue()));
        colorAnim.start();

        // Optionally show a warning message
        tvAcc.setText(tvAcc.getText() + "\n⚠️ FALL DETECTED!");

        // Revert after 3 seconds
        cardSensor.postDelayed(() -> {
            ValueAnimator reverseAnim = ValueAnimator.ofArgb(colorTo, colorFrom);
            reverseAnim.setDuration(400);
            reverseAnim.addUpdateListener(animator -> cardSensor.setCardBackgroundColor((int) animator.getAnimatedValue()));
            reverseAnim.start();
            isFallVisualActive = false;
        }, 10000);
    }

    private void showFallAlert() {
        alertContainer.setVisibility(View.VISIBLE);
    }


}
