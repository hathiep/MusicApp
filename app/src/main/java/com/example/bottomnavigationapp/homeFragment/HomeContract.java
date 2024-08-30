package com.example.bottomnavigationapp.homeFragment;

import com.example.bottomnavigationapp.Song;
import android.content.Context;

import java.util.List;

public interface HomeContract {
    interface View {
        void showSongs(List<Song> songs);
        void showLoadingIndicator(boolean show);
        void updatePlayingSongInfo(Song song);
        void updatePlayButton(boolean isPlaying);
        void updatePreNextButton();
        void updateSeekBar();
        void onSeekBarProgressChanged(int progress, boolean fromUser);
        void updateAdapter(int position);
        Context getContext();
    }

    interface Presenter {
        void loadSongs();
        void onPlayPauseClicked();
        void onPreviousClicked();
        void onNextClicked();
        void onSeekBarStopTrackingTouch(int position);
        void onSongSelected(Song song);
        Song getCurrentSong();
    }
}
