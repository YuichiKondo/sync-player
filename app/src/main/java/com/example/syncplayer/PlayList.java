package com.example.syncplayer;

import android.content.Context;
import android.graphics.Color;
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

public class PlayList {
    @FunctionalInterface
    public interface IOnClickListener {
        void onClick(Song song);
    }

    private final int normalFontSize = 16;
    private final Context context;
    private final LinearLayout playListLinearLayout;
    private final IOnClickListener onClickListener;
    private final HashMap<Song, Button> buttons = new HashMap<>();
    private final List<Song> playbackOrder = new LinkedList<>();
    private final List<Song> defaultOrder = new ArrayList<>();
    private Song currentSong;
    private boolean shuffle = false;
    private boolean repeat = false;

    public PlayList(Context context, LinearLayout playListLinearLayout, IOnClickListener onClickListener) {
        this.context = context;
        this.playListLinearLayout = playListLinearLayout;
        this.onClickListener = onClickListener;
    }

    public void add(Song song) {
        playbackOrder.add(song);
        defaultOrder.add(song);
        Button button = new Button(context);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setText(String.format(Locale.getDefault(), "%s - %s", song.artist, song.title));
        button.setAllCaps(false);
        button.setTextSize(normalFontSize);
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 5);
        button.setLayoutParams(params);
        button.setOnClickListener(v -> {
            setCurrentSong(song);
            onClickListener.onClick(song);
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
                setCurrentSong(null);
                return null;
            }
            previousSongIndex = playbackOrder.size() - 1;
        } else {
            previousSongIndex = currentSongIndex - 1;
        }
        Song previousSong = playbackOrder.get(previousSongIndex);
        setCurrentSong(previousSong);
        return previousSong;
    }

    @SuppressWarnings("unused")
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
                setCurrentSong(null);
                return null;
            } else {
                if (shuffle) {
                    setCurrentSong(null);
                    shuffleOrder();
                }
                nextSongIndex = 0;
            }
        } else {
            nextSongIndex = currentSongIndex + 1;
        }
        Song nextSong = playbackOrder.get(nextSongIndex);
        setCurrentSong(nextSong);
        return nextSong;
    }

    public Song first() {
        if (playbackOrder.isEmpty()) {
            return null;
        }
        Song firstSong = playbackOrder.get(0);
        setCurrentSong(firstSong);
        return firstSong;
    }

    public void shuffleOrder() {
        shuffle = true;
        if (currentSong == null) {
            Collections.shuffle(playbackOrder);
        } else {
            playbackOrder.remove(currentSong);
            Collections.shuffle(playbackOrder);
            playbackOrder.add(0, currentSong);
        }
        refresh();
    }

    public void resetOrder() {
        shuffle = false;
        playbackOrder.clear();
        playbackOrder.addAll(defaultOrder);
        refresh();
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    private void setCurrentSong(Song song) {
        final int highlightedFontSize = 24;
        if (currentSong != null) {
            Button button = buttons.get(currentSong);
            assert button != null;
            button.setTextSize(normalFontSize);
        }
        if (song != null) {
            Button button = buttons.get(song);
            assert button != null;
            button.setTextSize(highlightedFontSize);
        }
        currentSong = song;
    }

    private void refresh() {
        playListLinearLayout.removeAllViews();
        for (Song song : playbackOrder) {
            playListLinearLayout.addView(buttons.get(song));
        }
    }
}
