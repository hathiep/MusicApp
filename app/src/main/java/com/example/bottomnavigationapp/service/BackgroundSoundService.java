package com.example.bottomnavigationapp.service;

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

import com.example.bottomnavigationapp.R;
import com.example.bottomnavigationapp.model.Song;
import com.example.bottomnavigationapp.screen.homeFragment.HomeFragment;

public class BackgroundSoundService extends Service {
    public static final String CHANNEL_ID = "BackgroundMusicService";
    private MediaPlayer mediaPlayer;
    private boolean isPaused = false;
    private Handler handler = new Handler();
    private Song currentSong;
    private String audioPath;
    private int pausedPosition = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Không khởi tạo MediaPlayer tại onCreate, chỉ khởi tạo khi cần
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            Song song = intent.getParcelableExtra("SONG");
            pausedPosition = intent.getIntExtra("MEDIA_POSITION", pausedPosition);

            if (song != null) {
                currentSong = song;
                String newAudioPath = song.getAudioPath();

                if (newAudioPath != null && !newAudioPath.equals(audioPath)) {
                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                    }
                    audioPath = newAudioPath;
                    mediaPlayer = MediaPlayer.create(this, Uri.parse(audioPath));
                    if (mediaPlayer != null) {
                        mediaPlayer.setVolume(100, 100);

                        // Thêm sự kiện khi bài hát kết thúc
                        mediaPlayer.setOnCompletionListener(mp -> {
                            // Gửi broadcast để chuyển bài tiếp theo
                            sendBroadcastNext();
                        });
                    } else {
                        Log.e("BackgroundSoundService", "MediaPlayer could not be created with the provided audio path: " + audioPath);
                        stopSelf();
                    }
                }
            }

            switch (intent.getAction()) {
                case "ACTION_PLAY":
                    playAudio();
                    break;
                case "ACTION_PAUSE":
                    pauseAudio();
                    break;
                case "ACTION_SEEK":
                    int newPosition = intent.getIntExtra("MEDIA_POSITION", 0);
                    if (mediaPlayer != null) {
                        mediaPlayer.seekTo(newPosition);
                        pausedPosition = newPosition;
                        handler.removeCallbacks(updateProgress);
                    }
                    break;
                case "ACTION_PREVIOUS":
                    sendBroadcastPrevious();
                    break;
                case "ACTION_NEXT":
                    sendBroadcastNext();
                    break;
                case "ACTION_TOGGLE_REPEAT": // New action to handle repeat toggle
                    toggleRepeat();
                    break;
            }
        }
        return START_STICKY;
    }

    private void playAudio() {
        if (mediaPlayer == null) {
            Log.e("BackgroundSoundService", "MediaPlayer is not initialized.");
            return;
        }

        if (isPaused) {
            Log.e("A", "PausedPosition: " + pausedPosition);
            mediaPlayer.seekTo(pausedPosition); // Phát từ vị trí đã lưu khi tạm dừng
            mediaPlayer.start();
            isPaused = false;
        } else if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(pausedPosition); // Phát từ vị trí đã lưu khi tạm dừng
            mediaPlayer.start();
        }

        handler.post(updateProgress);
        updateNotification(true);
        sendBroadcastUpdate(true, mediaPlayer.getCurrentPosition());
    }

    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            pausedPosition = mediaPlayer.getCurrentPosition(); // Lưu vị trí hiện tại
            mediaPlayer.pause();
            isPaused = true;
            Log.d("BackgroundSoundService", "Media paused at position: " + pausedPosition);
        } else {
            Log.d("BackgroundSoundService", "MediaPlayer is either null or not playing");
        }
        updateNotification(false);
        sendBroadcastUpdate(false, pausedPosition);
    }

    private void toggleRepeat() {
        if (mediaPlayer != null) {
            boolean isRepeating = mediaPlayer.isLooping();
            mediaPlayer.setLooping(!isRepeating);

            // Optional: Send broadcast or update notification to reflect repeat state
            Intent intent = new Intent("UPDATE_REPEAT_STATE");
            intent.putExtra("IS_REPEATING", !isRepeating);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    private Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();

                Intent intent = new Intent("UPDATE_SEEKBAR");
                intent.putExtra("CURRENT_POSITION", currentPosition);
                intent.putExtra("DURATION", duration);
                LocalBroadcastManager.getInstance(BackgroundSoundService.this).sendBroadcast(intent);

                handler.postDelayed(this, 100);
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
        super.onDestroy();
    }

    private void updateNotification(boolean isPlaying) {
        Intent intent = new Intent(this, HomeFragment.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String action = isPlaying ? "ACTION_PAUSE" : "ACTION_PLAY";
        int icon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        String status = isPlaying ? "Pause" : "Play";

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.image_background);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setSubText("Music Tina")
                .setContentTitle(currentSong != null ? currentSong.getTitle() : "No Song Playing")
                .setContentText(currentSong != null ? currentSong.getArtist() : "")
                .setLargeIcon(bitmap)
                .addAction(R.drawable.ic_previous, "Previous", getPendingIntent("ACTION_PREVIOUS", 0))
                .addAction(icon, status, getPendingIntent(action, 1))  // Nút Play/Pause
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
        intent.putExtra("SONG", currentSong);
        intent.putExtra("CURRENT_POSITION", mediaPlayer.getCurrentPosition());
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void sendBroadcastUpdate(boolean isPlaying, int position) {
        Intent intent = new Intent("UPDATE_PLAY_STATE");
        intent.putExtra("isPlaying", isPlaying);
        intent.putExtra("CURRENT_POSITION", position);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendBroadcastPrevious() {
        Intent intent = new Intent("ACTION_PREVIOUS");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendBroadcastNext() {
        Intent intent = new Intent("ACTION_NEXT");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}

// CustomNotification

//package com.example.bottomnavigationapp.homeFragment;
//
//import android.annotation.SuppressLint;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.media.MediaPlayer;
//import android.net.Uri;
//import android.os.Handler;
//import android.os.IBinder;
//import android.util.Log;
//import android.widget.RemoteViews;
//
//import androidx.annotation.Nullable;
//import androidx.core.app.NotificationCompat;
//import androidx.localbroadcastmanager.content.LocalBroadcastManager;
//
//import com.example.bottomnavigationapp.R;
//import com.example.bottomnavigationapp.model.Song;
//
//public class BackgroundSoundService extends Service {
//    public static final String CHANNEL_ID = "BackgroundMusicService";
//    private MediaPlayer mediaPlayer;
//    private boolean isPaused = false;
//    private Handler handler = new Handler();
//    private Song currentSong;
//    private String audioPath;
//    private int pausedPosition = 0;
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        // Không khởi tạo MediaPlayer tại onCreate, chỉ khởi tạo khi cần
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        if (intent != null && intent.getAction() != null) {
//            Song song = intent.getParcelableExtra("SONG");
//            pausedPosition = intent.getIntExtra("MEDIA_POSITION", pausedPosition); // Lấy vị trí khi bắt đầu phát
//            if (song != null) {
//                currentSong = song;
//                String newAudioPath = song.getAudioPath();
//
//                if (newAudioPath != null && !newAudioPath.equals(audioPath)) {
//                    if (mediaPlayer != null) {
//                        mediaPlayer.release();
//                    }
//                    audioPath = newAudioPath;
//                    mediaPlayer = MediaPlayer.create(this, Uri.parse(audioPath));
//                    if (mediaPlayer != null) {
//                        mediaPlayer.setLooping(true);
//                        mediaPlayer.setVolume(100, 100);
//                    } else {
//                        Log.e("BackgroundSoundService", "MediaPlayer could not be created with the provided audio path: " + audioPath);
//                        stopSelf();
//                    }
//                }
//
//                switch (intent.getAction()) {
//                    case "ACTION_PLAY":
//                        playAudio();
//                        break;
//                    case "ACTION_PAUSE":
//                        pauseAudio();
//                        break;
//                    case "ACTION_SEEK":
//                        int newPosition = intent.getIntExtra("MEDIA_POSITION", 0);
//                        if (mediaPlayer != null) {
//                            mediaPlayer.seekTo(newPosition); // Phát từ vị trí được lưu
//                            pausedPosition = newPosition;
//                            handler.removeCallbacks(updateProgress); // Dừng cập nhật hiện tại
//                        }
//                        break;
//                    case "ACTION_PREVIOUS":
//                        sendBroadcastPrevious();
//                        break;
//                    case "ACTION_NEXT":
//                        sendBroadcastNext();
//                        break;
//                }
//            }
//        }
//        return START_STICKY;
//    }
//
//
//    private void playAudio() {
//        if (mediaPlayer == null) {
//            Log.e("BackgroundSoundService", "MediaPlayer is not initialized.");
//            return;
//        }
//
//        if (isPaused) {
//            Log.e("A", "PausedPosition: " + pausedPosition);
//            mediaPlayer.seekTo(pausedPosition); // Phát từ vị trí đã lưu khi tạm dừng
//            mediaPlayer.start();
//            isPaused = false;
//        } else if (!mediaPlayer.isPlaying()) {
//            mediaPlayer.seekTo(pausedPosition); // Phát từ vị trí đã lưu khi tạm dừng
//            mediaPlayer.start();
//        }
//
//        handler.post(updateProgress);
//        updateNotification(true);
//        sendBroadcastUpdate(true, mediaPlayer.getCurrentPosition());
//    }
//
//    private void pauseAudio() {
//        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
//            pausedPosition = mediaPlayer.getCurrentPosition(); // Lưu vị trí hiện tại
//            mediaPlayer.pause();
//            isPaused = true;
//            Log.d("BackgroundSoundService", "Media paused at position: " + pausedPosition);
//        } else {
//            Log.d("BackgroundSoundService", "MediaPlayer is either null or not playing");
//        }
//        updateNotification(false);
//        sendBroadcastUpdate(false, pausedPosition);
//    }
//
//    private Runnable updateProgress = new Runnable() {
//        @Override
//        public void run() {
//            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
//                int currentPosition = mediaPlayer.getCurrentPosition();
//                int duration = mediaPlayer.getDuration();
//
//                Intent intent = new Intent("UPDATE_SEEKBAR");
//                intent.putExtra("CURRENT_POSITION", currentPosition);
//                intent.putExtra("DURATION", duration);
//                LocalBroadcastManager.getInstance(BackgroundSoundService.this).sendBroadcast(intent);
//
//                handler.postDelayed(this, 100);
//            }
//        }
//    };
//
//    @Override
//    public void onDestroy() {
//        if (mediaPlayer != null) {
//            mediaPlayer.stop();
//            mediaPlayer.release();
//            mediaPlayer = null;
//        }
//        handler.removeCallbacks(updateProgress);
//        super.onDestroy();
//    }
//
//    private void updateNotification(boolean isPlaying) {
//        Intent intent = new Intent(this, HomeFragment.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//
//        String action = isPlaying ? "ACTION_PAUSE" : "ACTION_PLAY";
//        int icon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
//        String status = isPlaying ? "Pause" : "Play";
//
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.image_background);
//
//        // Create RemoteViews for custom notification
//        @SuppressLint("RemoteViewLayout") RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_custom_layout);
//        notificationLayout.setTextViewText(R.id.notification_title, currentSong != null ? currentSong.getTitle() : "No Song Playing");
//        notificationLayout.setTextViewText(R.id.notification_artist, currentSong != null ? currentSong.getArtist() : "");
//        notificationLayout.setImageViewBitmap(R.id.notification_image, bitmap);
//        notificationLayout.setProgressBar(R.id.notification_seekbar, mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition(), false);
//
//        // Set actions for the buttons
//        notificationLayout.setOnClickPendingIntent(R.id.notification_previous, getPendingIntent("ACTION_PREVIOUS", 0));
//        notificationLayout.setOnClickPendingIntent(R.id.notification_play_pause, getPendingIntent(action, 1));  // Nút Play/Pause
//        notificationLayout.setOnClickPendingIntent(R.id.notification_next, getPendingIntent("ACTION_NEXT", 2));
//
//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_logo)
//                .setSubText("Music Tina")
//                .setContent(notificationLayout)
//                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
//                        .setShowActionsInCompactView(1))
//                .setContentIntent(pendingIntent)
//                .setPriority(NotificationCompat.PRIORITY_LOW);
//
//        startForeground(1, notificationBuilder.build());
//    }
//
//    private PendingIntent getPendingIntent(String action, int requestCode) {
//        Intent intent = new Intent(this, BackgroundSoundService.class);
//        intent.setAction(action);
//        intent.putExtra("SONG", currentSong);
//        intent.putExtra("CURRENT_POSITION", mediaPlayer.getCurrentPosition());
//        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//    }
//
//    private void sendBroadcastUpdate(boolean isPlaying, int position) {
//        Intent intent = new Intent("UPDATE_PLAY_STATE");
//        intent.putExtra("isPlaying", isPlaying);
//        intent.putExtra("CURRENT_POSITION", position);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
//    }
//
//    private void sendBroadcastPrevious() {
//        Intent intent = new Intent("ACTION_PREVIOUS");
//        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
//    }
//
//    private void sendBroadcastNext() {
//        Intent intent = new Intent("ACTION_NEXT");
//        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
//    }
//}
//
