package com.example.bottomnavigationapp.screen.homeFragment.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.bottomnavigationapp.R;
import com.example.bottomnavigationapp.model.Song;

import java.util.List;

public class SongAdapter extends ArrayAdapter<Song> {
    private final Context context;
    private final List<Song> songs;
    private int selectedPosition = -1; // Vị trí của item được chọn

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

        // Thay đổi nền của CardView dựa trên vị trí đã chọn
        if (position == selectedPosition) {
            convertView.setBackgroundResource(R.drawable.item_bg_seleted);
            songTitle.setTextColor(ContextCompat.getColor(context, R.color.white)); // Màu text khi chọn
            songArtist.setTextColor(ContextCompat.getColor(context, R.color.white)); // Màu text khi chọn
        } else {
            convertView.setBackgroundResource(R.drawable.item_bg_normal);
            songTitle.setTextColor(ContextCompat.getColor(context, R.color.black)); // Màu text khi chọn
            songArtist.setTextColor(ContextCompat.getColor(context, R.color.black)); // Màu text khi chọn
        }

        return convertView;
    }


    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged(); // Cập nhật lại danh sách để áp dụng thay đổi
    }
}
