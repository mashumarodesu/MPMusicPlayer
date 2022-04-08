package com.mashedpotato.musicplayer;

import java.io.Serializable;

public class Song implements Serializable {

    private String data;
    private String title;
    private String album;
    private String artist;
    private String genre;
    private String trackNum;

    public Song(String data, String title, String album, String artist, String genre, String trackNum) {
        this.data = data;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.genre = genre;
        this.trackNum = trackNum;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getTrackNum() {
        return trackNum;
    }

    public void setTrackNum(String trackNum) {
        this.trackNum = trackNum;
    }
}