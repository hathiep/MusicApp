package com.example.bottomnavigationapp;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
                    updateNotification(true); // Cập nhật thông báo khi đang phát
                    break;
                case "ACTION_PAUSE":
                    pauseAudio();
                    updateNotification(false); // Cập nhật thông báo khi tạm dừng
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
        updateNotification(true);
        sendBroadcastUpdate(true);
    }

    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPaused = true;
            handler.removeCallbacks(updateProgress);
        }
        updateNotification(false);
        sendBroadcastUpdate(false);
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

    private void updateNotification(boolean isPlaying) {
        Intent intent = new Intent(this, HomeFragment.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String action = isPlaying ? "ACTION_PAUSE" : "ACTION_PLAY";
        int icon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        String title = isPlaying ? "Pause" : "Play";

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.image_backround);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setSubText("Music Tina")
                .setContentTitle("Title of song")
                .setContentText("Single of song")
                .setLargeIcon(bitmap)
                .addAction(R.drawable.ic_previous, "Previous", getPendingIntent("ACTION_PREVIOUS", 0))
                .addAction(icon, title, getPendingIntent(action, 1))  // Nút Play/Pause
                .addAction(R.drawable.ic_next, "Next", getPendingIntent("ACTION_NEXT", 2))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(1))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        startForeground(1, notificationBuilder.build());
    }

    private PendingIntent getPendingIntent(String action, int requestCode) {
        Intent intent = new Intent(this, BackgroundSoundService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void sendBroadcastUpdate(boolean isPlaying) {
        Intent intent = new Intent("UPDATE_PLAY_STATE");
        intent.putExtra("isPlaying", isPlaying);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
