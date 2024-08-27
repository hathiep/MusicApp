package com.example.bottomnavigationapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class SongAdapter extends ArrayAdapter<Song> {
    private final Context context;
    private final List<Song> songs;

    public SongAdapter(Context context, List<Song> songs) {
        super(context, R.layout.item_song, songs);
        this.context = context;
        this.songs = songs;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.item_song, parent, false);
        }

        Song song = songs.get(position);

        TextView songTitle = convertView.findViewById(R.id.song_title);
        TextView songArtist = convertView.findViewById(R.id.song_artist);

        songTitle.setText(song.getTitle());
        songArtist.setText(song.getArtist());

        return convertView;
    }
}
