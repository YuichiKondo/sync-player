package com.example.syncplayer;

public class Song {
    public int resId;
    public String title;
    public String artist;
    public float BPM;

    public Song(int resId, String title, String artist, float BPM) {
        this.resId = resId;
        this.title = title;
        this.artist = artist;
        this.BPM = BPM;
    }
}
