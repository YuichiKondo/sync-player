package com.example.syncplayer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SwitchCompat;
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
    private long prev_time;
    private final long timeoutMillis = 2000;
    private boolean useManualBPM = false;
    private boolean shouldSyncBPM = false;
    private boolean shuffle = false;
    private boolean repeat = false;
    private final int averageN = 5;
    private final ArrayList<Long> stepDeltas = new ArrayList<>(averageN);
    private Drawable shuffleOnImage;
    private Drawable shuffleOffImage;
    private Drawable repeatOnImage;
    private Drawable repeatOffImage;
    private Drawable pauseImage;
    private Drawable resumeImage;
    private Player player;
    private PlayList playList;
    private Handler updateSeekBarHandler;
    private Handler timeoutHandler;
    private TextView detectedBPMTextView;
    private TextView songInfoTextView;
    private TextView originalBPMTextView;
    private TextView playTimeTextView;
    private EditText manualBpmEditText;
    private SeekBar seekBar;
    private ImageButton shuffleButton;
    private ImageButton repeatButton;
    private ImageButton pauseOrResumeButton;
    private View mainView;

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

//        初期値設定
        prev_time = System.currentTimeMillis();

//        ボタン画像
        shuffleOnImage = AppCompatResources.getDrawable(this, R.drawable.shuffle_on);
        shuffleOffImage = AppCompatResources.getDrawable(this, R.drawable.shuffle_off);
        repeatOnImage = AppCompatResources.getDrawable(this, R.drawable.repeat_on);
        repeatOffImage = AppCompatResources.getDrawable(this, R.drawable.repeat_off);
        pauseImage = AppCompatResources.getDrawable(this, R.drawable.pause);
        resumeImage = AppCompatResources.getDrawable(this, R.drawable.resume);

//        音楽再生機能
        player = new Player(this);
        playList = new PlayList(this, findViewById(R.id.linear_layout_play_list), this::play);
        playList.add(new Song(R.raw.vibe, "Vibe", "Spicyverse", 143));
        playList.add(new Song(R.raw.lets_play, "Let's Play", "MADZI", 124));
        playList.add(new Song(R.raw.paradise, "Paradise", "N3WPORT x Britt Lari", 80));
        playList.add(new Song(R.raw.slash, "SLASH", "Tokyo Machine", 128));
        playList.add(new Song(R.raw.get_up, "GET UP", "TOKYO MACHINE & Guy Arthur", 128));
        playList.add(new Song(R.raw.lost_my_love, "Lost My Love", "Tatsunoshin", 165));
        playList.add(new Song(R.raw.the_time, "The Time", "Tatsunoshin", 150));
        playList.add(new Song(R.raw.summers_end, "Summer's End (feat. Meredith Bull)", "Tatsunoshin & Jade Key", 153));
        playList.add(new Song(R.raw.light_me_up, "Light Me Up (feat. Giin)", "Tatsunoshin", 160));
        playList.add(new Song(R.raw.take_me_away, "Take Me Away", "NATSUMI", 150));
        songInfoTextView = findViewById(R.id.text_view_song_info);
        originalBPMTextView = findViewById(R.id.text_view_original_bpm);
        playTimeTextView = findViewById(R.id.text_view_play_time);
        shuffleButton = findViewById(R.id.button_shuffle);
        shuffleButton.setOnClickListener(v -> {
            if (shuffle) {
                playList.resetOrder();
                shuffle = false;
                shuffleButton.setImageDrawable(shuffleOffImage);
                Log.d("DEBUG", "shuffle off");
            } else {
                playList.shuffleOrder();
                shuffle = true;
                shuffleButton.setImageDrawable(shuffleOnImage);
                Log.d("DEBUG", "shuffle on");
            }
        });
        repeatButton = findViewById(R.id.button_repeat);
        repeatButton.setOnClickListener(v -> {
            if (repeat) {
                playList.setRepeat(false);
                repeat = false;
                repeatButton.setImageDrawable(repeatOffImage);
                Log.d("DEBUG", "repeat off");
            } else {
                playList.setRepeat(true);
                repeat = true;
                repeatButton.setImageDrawable(repeatOnImage);
                Log.d("DEBUG", "repeat on");
            }
        });
        ImageButton nextButton = findViewById(R.id.button_next);
        nextButton.setOnClickListener(v -> next());
        ImageButton previousButton = findViewById(R.id.button_previous);
        previousButton.setOnClickListener(v -> {
            if (player.currentSong != null && player.getCurrentPosition() < 2000) {
                previous();
            } else {
                player.seekTo(0);
            }
        });
        pauseOrResumeButton = findViewById(R.id.button_pause_or_resume);
        pauseOrResumeButton.setOnClickListener(v -> {
            if (player.currentSong == null) {
                first();
            } else if (player.isPlaying()) {
                player.pause();
                pauseOrResumeButton.setImageDrawable(resumeImage);
                Log.d("DEBUG", "pause");
            } else {
                player.resume();
                pauseOrResumeButton.setImageDrawable(pauseImage);
                Log.d("DEBUG", "resume");
            }
        });
        seekBar = findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int duration = player.getDuration();
                playTimeTextView.setText(String.format(Locale.getDefault(), "%s/%s", formatTime(progress), formatTime(duration)));
                if (fromUser) {
                    player.seekTo(progress);
                    Log.d("DEBUG", "onProgressChanged fromUser to: " + progress + " / " + duration);
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

//        BPM手動設定機能
        SwitchCompat manualBPMSwitch = findViewById(R.id.switch_manual_bpm);
        manualBPMSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            useManualBPM = isChecked;
            if (shouldSyncBPM) {
                sync();
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
                    sync();
                }
            }
        });
//        キーボードの完了ボタンでキーボードを閉じて、フォーカスを外す
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mainView = findViewById(R.id.main);
        manualBpmEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mainView.requestFocus();
                imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                return true;
            } else {
                return false;
            }
        });

//        BPM同期スイッチ
        SwitchCompat syncSwitch = findViewById(R.id.switch_sync);
        syncSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shouldSyncBPM = isChecked;
            if (isChecked) {
                sync();
            } else {
                player.setSpeed(1);
            }
            Log.d("DEBUG", "shouldSyncBPM: " + shouldSyncBPM);
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
//        noinspection deprecation
        requestVisibleBehind(true);
    }

    private void play(Song song) {
        player.play(song);
        seekBar.setMax(player.getDuration());
        seekBar.setProgress(0);
        songInfoTextView.setText(String.format(Locale.getDefault(), "%s - %s", song.artist, song.title));
        originalBPMTextView.setText(String.format(Locale.getDefault(), "Original BPM %.2f", song.BPM));
        pauseOrResumeButton.setImageDrawable(pauseImage);
        player.setCompletionListener(mp -> next());
        if (shouldSyncBPM) {
            sync();
        }
        Log.d("DEBUG", "play song: " + song.title);
    }

    private void first() {
        Song song = playList.first();
        if (song != null) {
            Log.d("DEBUG", "first song: " + song.title);
            play(song);
        }
    }

    private void next() {
        Song nextSong = playList.next();
        if (nextSong != null) {
            play(nextSong);
            Log.d("DEBUG", "next song: " + nextSong.title);
        } else {
            resetPlayer();
            Log.d("DEBUG", "no next song");
        }
    }

    private void previous() {
        Song previousSong = playList.previous();
        if (previousSong != null) {
            play(previousSong);
            Log.d("DEBUG", "previous song: " + previousSong.title);
        } else {
            resetPlayer();
            Log.d("DEBUG", "no previous song");
        }
    }

    private void resetPlayer() {
        player.release();
        songInfoTextView.setText("");
        originalBPMTextView.setText("");
        seekBar.setMax(0);
        playTimeTextView.setText("");
        pauseOrResumeButton.setImageDrawable(resumeImage);
    }

    private void sync() {
        float targetBPM;
        if (useManualBPM) {
            try {
                targetBPM = Float.parseFloat(manualBpmEditText.getText().toString());
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
                sync();
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
                    detectedBPMTextView.setText(String.format(Locale.getDefault(), "%.2f", detectedBPM));
                    if (shouldSyncBPM) {
                        sync();
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

    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}