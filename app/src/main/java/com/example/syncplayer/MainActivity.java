package com.example.syncplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

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

    @RequiresApi(api = Build.VERSION_CODES.Q)
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

//        権限チェック
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("DEBUG", "ACTIVITY_RECOGNITION granted");
            setupDetectingBPM();
        } else {
            Log.d("DEBUG", "ACTIVITY_RECOGNITION not granted");
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACTIVITY_RECOGNITION}, 1);
        }

//        音楽再生機能
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

//        BPM手動設定機能
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

//        BPM同期スイッチ
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch syncBPMSwitch = findViewById(R.id.switch_sync_bpm);
        syncBPMSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shouldSyncBPM = isChecked;
            if (isChecked) {
                syncBPM();
            } else {
                player.setSpeed(1);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && Objects.equals(permissions[0], Manifest.permission.ACTIVITY_RECOGNITION) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("DEBUG", "Request permission succeeded");
            setupDetectingBPM();
        } else {
            Log.d("DEBUG", "Request permission failed");
//            権限が許可されなかった場合は設定画面を開いて許可を求める
            new AlertDialog.Builder(this).setMessage(R.string.request_permission_activity_recognition).setNegativeButton(R.string.to_settings, (dialog, which) -> {
                Intent settings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                startActivity(settings);
                finish();
            }).show();
        }
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

    private void setupDetectingBPM() {
        detectedBPMTextView = findViewById(R.id.text_view_detected_bpm);
        timeoutHandler = new Handler();
        Runnable timeout = () -> {
            stepDeltas.clear();
            detectedBPM = 0;
            detectedBPMTextView.setText(R.string.default_detected_bpm);
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
                if (!stepDeltas.isEmpty()) {
                    long sum = 0;
                    for (long stepDelta : stepDeltas) {
                        sum += stepDelta;
                    }
                    float averageDelta = (float) sum / stepDeltas.size();
                    detectedBPM = 60000 / averageDelta;
                    detectedBPMTextView.setText(String.format(Locale.getDefault(), "%.2f", detectedBPM));
                    if (shouldSyncBPM) {
                        syncBPM();
                    }
                    Log.d("DEBUG", "detectedBPM: " + detectedBPM + ", averageDelta: " + averageDelta + ", stepDeltas: " + stepDeltas);
                }
                prev_time = curr_time;
                timeoutHandler.postDelayed(timeout, timeoutMillis);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        }, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}