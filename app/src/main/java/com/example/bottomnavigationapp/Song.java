package com.example.bottomnavigationapp;

import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {
    private String id;
    private String title;
    private String artist;
    private String audioPath;

    public Song() {
        // Constructor mặc định cần thiết cho Firestore
    }

    public Song(String id, String title, String artist, String audioPath) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.audioPath = audioPath;
    }

    protected Song(Parcel in) {
        id = in.readString();
        title = in.readString();
        artist = in.readString();
        audioPath = in.readString();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(audioPath);
    }
}
