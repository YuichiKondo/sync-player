package com.example.syncplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private float detectedBPM = 0;
    private long prev_time = System.currentTimeMillis();
    private final long timeoutMillis = 2000;
    private boolean useManualBPM = false;
    private boolean shouldSyncBPM = false;
    private final int averageN = 5;
    private final ArrayList<Long> stepDeltas = new ArrayList<>(averageN);
    private Player player;
    private Handler updateSeekBarHandler;
    private Handler timeoutHandler;
    private TextView detectedBPMTextView;
    private TextView playTimeTextView;
    private EditText manualBpmEditText;
    private SeekBar seekBar;
    private Button pauseOrResumeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Song songVibe = new Song(R.raw.vibe, "Vibe", "Spicyverse", 143);
        player = new Player(this);
        playTimeTextView = findViewById(R.id.text_view_play_time);
        seekBar = findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int duration = player.getDuration();
                playTimeTextView.setText(String.format(Locale.getDefault(), "%s/%s", formatTime(progress), formatTime(duration)));
                if (fromUser) {
                    player.seekTo(progress);
                    Log.d("DEBUG", "onProgressChanged to: " + progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        updateSeekBarHandler = new Handler();
        updateSeekBarHandler.post(new Runnable() {
            @Override
            public void run() {
                seekBar.setProgress(player.getCurrentPosition());
                updateSeekBarHandler.postDelayed(this, 100);
            }
        });
        Button playButton = findViewById(R.id.button_play);
        pauseOrResumeButton = findViewById(R.id.button_pause_or_resume);
        playButton.setOnClickListener(v -> {
            player.play(songVibe);
            seekBar.setMax(player.getDuration());
            seekBar.setProgress(0);
            pauseOrResumeButton.setText(R.string.pause);
            player.setCompletionListener(mp -> pauseOrResumeButton.setText(R.string.resume));
            if (shouldSyncBPM) {
                syncBPM();
            }
        });
        pauseOrResumeButton.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                pauseOrResumeButton.setText(R.string.resume);
            } else {
                player.resume();
                pauseOrResumeButton.setText(R.string.pause);
            }
        });

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch manualBPMSwitch = findViewById(R.id.switch_manual_bpm);
        manualBPMSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            useManualBPM = isChecked;
            if (shouldSyncBPM) {
                syncBPM();
            }
        });
        manualBpmEditText = findViewById(R.id.edit_text_number_manual_bpm);
        manualBpmEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (shouldSyncBPM) {
                    syncBPM();
                }
            }
        });

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch syncBPMSwitch = findViewById(R.id.switch_sync_bpm);
        syncBPMSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shouldSyncBPM = isChecked;
            if (isChecked) {
                syncBPM();
            } else {
                player.setSpeed(1);
            }
        });

//        BPM検出
        detectedBPMTextView = findViewById(R.id.text_view_detected_bpm);
        timeoutHandler = new Handler();
        Runnable timeout = () -> {
            stepDeltas.clear();
            detectedBPM = 0;
            detectedBPMTextView.setText("0");
            if (shouldSyncBPM) {
                syncBPM();
            }
            Log.d("DEBUG", "timeout");
        };
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                timeoutHandler.removeCallbacks(timeout);
                long curr_time = System.currentTimeMillis();
                long delta = curr_time - prev_time;
                if (delta < timeoutMillis) {
                    stepDeltas.add(delta);
                }
                if (stepDeltas.size() > averageN) {
                    stepDeltas.remove(0);
                }
                stepDeltas.stream().mapToLong(Long::longValue).average().ifPresent(averageDelta -> {
                    detectedBPM = 60000 / (float) averageDelta;
                    detectedBPMTextView.setText(String.valueOf(detectedBPM));
                    if (shouldSyncBPM) {
                        syncBPM();
                    }
                    Log.d("DEBUG", "detectedBPM: " + detectedBPM + ", averageDelta: " + averageDelta + ", stepDeltas: " + stepDeltas);
                });
                prev_time = curr_time;
                timeoutHandler.postDelayed(timeout, timeoutMillis);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        }, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        擬似バックグラウンド実行
        requestVisibleBehind(true);
    }

    private void syncBPM() {
        float targetBPM;
        if (useManualBPM) {
            try {
                targetBPM = Integer.parseInt(manualBpmEditText.getText().toString());
            } catch (NumberFormatException e) {
                targetBPM = detectedBPM;
            }
        } else {
            targetBPM = detectedBPM;
        }
        player.syncBPM(targetBPM);
        Log.d("DEBUG", "syncBPM targetBPM: " + targetBPM);
    }

    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}