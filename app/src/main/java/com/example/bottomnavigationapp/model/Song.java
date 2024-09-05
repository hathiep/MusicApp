package com.example.bottomnavigationapp.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {
    private String id;
    private String title;
    private String artist;
    private String audioPath;
    private String imageUrl;  // Thêm thuộc tính image

    public Song() {
    }

    public Song(String id, String title, String artist, String audioPath, String imageUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.audioPath = audioPath;
        this.imageUrl = imageUrl;  // Khởi tạo image
    }

    protected Song(Parcel in) {
        id = in.readString();
        title = in.readString();
        artist = in.readString();
        audioPath = in.readString();
        imageUrl = in.readString();  // Đọc image từ Parcel
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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
        dest.writeString(imageUrl);  // Ghi image vào Parcel
    }
}
