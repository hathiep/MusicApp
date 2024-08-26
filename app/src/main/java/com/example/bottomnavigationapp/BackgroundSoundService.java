package com.example.bottomnavigationapp;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class BackgroundSoundService extends Service {
    public static final String CHANNEL_ID = "BackgroundMusicService";
    private MediaPlayer mediaPlayer;
    private boolean isPaused = false;
    private Handler handler = new Handler();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = MediaPlayer.create(this, R.raw.audio1);
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(100, 100);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "ACTION_PLAY":
                    playAudio();
                    break;
                case "ACTION_PAUSE":
                    pauseAudio();
                    break;
            }
        }
        return START_STICKY;
    }

    private void playAudio() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.audio1);
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(100, 100);
        }

        if (isPaused) {
            mediaPlayer.start();
            isPaused = false;
        } else if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }

        handler.post(updateProgress);
    }

    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPaused = true;
            handler.removeCallbacks(updateProgress);
        }
    }

    private Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateProgress);
    }
}
