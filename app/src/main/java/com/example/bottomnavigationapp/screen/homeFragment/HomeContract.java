package com.example.bottomnavigationapp.screen.homeFragment;

import com.example.bottomnavigationapp.model.Song;
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
        void onCancelClicked();
        void updateAdapter(int position);
        Context getContext();
    }

    interface Presenter {
        void loadSongs(String artist);
        void onPlayPauseClicked();
        void onPreviousClicked();
        void onNextClicked();
        void onCancelClicked();
        void onRepeatClicked(boolean isRepeat);
        void onSeekBarStopTrackingTouch(int position);
        void onSongSelected(Song song);
        Song getCurrentSong();
    }
}

