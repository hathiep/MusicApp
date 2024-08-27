package com.example.bottomnavigationapp;

import static android.content.ContentValues.TAG;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class BackgroundSoundService extends Service {
    public static final String CHANNEL_ID = "BackgroundMusicService";
    private MediaPlayer mediaPlayer;
    private boolean isPaused = false;
    private Handler handler = new Handler();
    private String audioPath;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Khởi tạo MediaPlayer khi onCreate Service
        if (audioPath != null && !audioPath.isEmpty()) {
            mediaPlayer = MediaPlayer.create(this, Uri.parse(audioPath));
        }

        // Kiểm tra xem MediaPlayer đã khởi tạo thành công chưa
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true); // Loop the sound
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            // Nhận đường dẫn audio từ Intent
            String newAudioPath = intent.getStringExtra("AUDIO_PATH");

            if (newAudioPath != null && !newAudioPath.equals(audioPath)) { // Chỉ khởi tạo lại MediaPlayer nếu đường dẫn mới khác
                if (mediaPlayer != null) {
                    mediaPlayer.release(); // Giải phóng MediaPlayer cũ nếu tồn tại
                }
                audioPath = newAudioPath;
                mediaPlayer = MediaPlayer.create(this, Uri.parse(audioPath));
                if (mediaPlayer != null) {
                    mediaPlayer.setLooping(true);
                    mediaPlayer.setVolume(100, 100);
                } else {
                    Log.e("BackgroundSoundService", "MediaPlayer could not be created with the provided audio path: " + audioPath);
                    stopSelf();
                }
            }

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

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.image_background);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setSubText("Music Tina")
                .setContentTitle("Chúng ta của tương lai")
                .setContentText("Sơn Tùng MTP")
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
