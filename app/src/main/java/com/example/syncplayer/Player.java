package com.example.syncplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.PlaybackParams;

import androidx.core.math.MathUtils;

public class Player {
    private final Context context;
    private MediaPlayer mediaPlayer = null;
    private final PlaybackParams playbackParams;
    public Song currentSong = null;

    public Player(Context context) {
        this.context = context;
        playbackParams = new PlaybackParams().setSpeed(1);
    }

    public void play(Song song) {
        currentSong = song;
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(context, song.resId);
//        初回の再生速度変更では音声の途切れが発生するのであらかじめ一度再生速度を変更して元に戻す
//        setPlaybackParams()で0以外の再生速度を設定すると、start()を呼んでいなくても再生が始まる
        mediaPlayer.setPlaybackParams(new PlaybackParams().setSpeed(1.0001f));
        mediaPlayer.setPlaybackParams(playbackParams);
    }

    public void setCompletionListener(MediaPlayer.OnCompletionListener listener) {
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.setOnCompletionListener(listener);
    }

    public void pause() {
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.pause();
    }

    public void resume() {
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.setPlaybackParams(playbackParams);
    }

    public boolean isPlaying() {
        if (mediaPlayer == null) {
            return false;
        }
        return mediaPlayer.isPlaying();
    }

    public void setSpeed(float speed) {
        if (mediaPlayer == null) {
            return;
        }
//        極端な再生速度を設定するとエラーが発生するので範囲を制限
        speed = MathUtils.clamp(speed, 0.5f, 2.0f);
        playbackParams.setSpeed(speed);
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.setPlaybackParams(playbackParams);
        }
    }

    public void syncBPM(float targetBPM) {
        if (currentSong == null) {
            return;
        }
        float speed = targetBPM / currentSong.BPM;
        setSpeed(speed);
    }

    public int getDuration() {
        if (mediaPlayer == null) {
            return 0;
        }
        return mediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
        if (mediaPlayer == null) {
            return 0;
        }
        return mediaPlayer.getCurrentPosition();
    }

    public void seekTo(int msec) {
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.seekTo(msec);
    }
}
