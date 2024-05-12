package com.example.syncplayer;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class PlayList {
    private final Context context;
    private final LinearLayout playListLinearLayout;
    private final Consumer<Song> callback;
    private final HashMap<Song, Button> buttons = new HashMap<>();
    private final List<Song> playbackOrder = new LinkedList<>();
    private final List<Song> defaultOrder = new ArrayList<>();
    private Song currentSong;
    private boolean repeat = false;

    public PlayList(Context context, LinearLayout playListLinearLayout, Consumer<Song> callback) {
        this.context = context;
        this.playListLinearLayout = playListLinearLayout;
        this.callback = callback;
    }

    public void add(Song song) {
        playbackOrder.add(song);
        defaultOrder.add(song);
        Button button = new Button(context);
        button.setText(String.format(Locale.getDefault(), "%s - %s", song.artist, song.title));
        button.setAllCaps(false);
        button.setTextSize(16);
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 5);
        button.setLayoutParams(params);
        button.setOnClickListener(v -> {
            currentSong = song;
            callback.accept(song);
        });
        buttons.put(song, button);
        playListLinearLayout.addView(button);
    }

    public @Nullable Song previous() {
        if (playbackOrder.isEmpty() || currentSong == null) {
            return null;
        }
        int currentSongIndex = playbackOrder.indexOf(currentSong);
        int previousSongIndex;
        if (currentSongIndex == 0) {
            if (!repeat) {
                currentSong = null;
                return null;
            }
            previousSongIndex = playbackOrder.size() - 1;
        } else {
            previousSongIndex = currentSongIndex - 1;
        }
        Song previousSong = playbackOrder.get(previousSongIndex);
        currentSong = previousSong;
        return previousSong;
    }

    public @Nullable Song current() {
        return currentSong;
    }

    public @Nullable Song next() {
        if (playbackOrder.isEmpty() || currentSong == null) {
            return null;
        }
        int currentSongIndex = playbackOrder.indexOf(currentSong);
        int nextSongIndex;
        if (currentSongIndex == playbackOrder.size() - 1) {
            if (!repeat) {
                currentSong = null;
                return null;
            } else {
                nextSongIndex = 0;
            }
        } else {
            nextSongIndex = currentSongIndex + 1;
        }
        Song nextSong = playbackOrder.get(nextSongIndex);
        currentSong = nextSong;
        return nextSong;
    }

    public void shuffleOrder() {
        playbackOrder.remove(currentSong);
        Collections.shuffle(playbackOrder);
        playbackOrder.add(0, currentSong);
        refresh();
    }

    public void resetOrder() {
        playbackOrder.clear();
        playbackOrder.addAll(defaultOrder);
        refresh();
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    private void refresh() {
        playListLinearLayout.removeAllViews();
        for (Song song : playbackOrder) {
            playListLinearLayout.addView(buttons.get(song));
        }
    }
}
