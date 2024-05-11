package com.example.syncplayer;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class PlayList {
    private final Context context;
    private final LinearLayout playListLinearLayout;
    private final Consumer<Song> callback;
    private final List<Song> songs = new ArrayList<>();
    private int currentSongIndex = 0;

    public PlayList(Context context, LinearLayout playListLinearLayout, Consumer<Song> callback) {
        this.context = context;
        this.playListLinearLayout = playListLinearLayout;
        this.callback = callback;
    }

    public void add(Song song) {
        songs.add(song);
        int index = songs.size() - 1;
        Button button = new Button(context);
        button.setText(String.format(Locale.getDefault(), "%s - %s", song.artist, song.title));
        button.setAllCaps(false);
        button.setTextSize(16);
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 5);
        button.setLayoutParams(params);
        button.setOnClickListener(v -> {
            currentSongIndex = index;
            callback.accept(song);
        });
        playListLinearLayout.addView(button);
    }

    public Song next() {
        if (songs.isEmpty()) {
            return null;
        }
        currentSongIndex = (currentSongIndex + 1) % songs.size();
        return songs.get(currentSongIndex);
    }
}
