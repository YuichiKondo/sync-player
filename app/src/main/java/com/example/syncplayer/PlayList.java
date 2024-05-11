package com.example.syncplayer;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.Locale;
import java.util.function.Consumer;

public class PlayList {
    private final Context context;
    private final LinearLayout playListLinearLayout;
    private final Consumer<Song> callback;

    public PlayList(Context context, LinearLayout playListLinearLayout, Consumer<Song> callback) {
        this.context = context;
        this.playListLinearLayout = playListLinearLayout;
        this.callback = callback;
    }

    public void Add(Song song){
        Button button = new Button(context);
        button.setText(String.format(Locale.getDefault(), "%s - %s", song.artist, song.title));
        button.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        button.setOnClickListener(v -> callback.accept(song));
        playListLinearLayout.addView(button);
    }
}
