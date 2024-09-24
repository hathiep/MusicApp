package com.example.bottomnavigationapp.service;

import static android.support.v4.media.MediaMetadataCompat.*;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.bottomnavigationapp.R;
import com.example.bottomnavigationapp.mainActivity.MainActivity;
import com.example.bottomnavigationapp.model.Song;

public class PlayServiceMedia extends Service {
    public static final String CHANNEL_ID = "BackgroundMusicService";
    private MediaPlayer mediaPlayer;
    private boolean isPaused = false, isRepeat = false;
    private Handler handler = new Handler();
    private Song currentSong;
    private String audioPath;
    private int pausedPosition = 0;
    private NotificationCompat.Builder notificationBuilder;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private MediaSessionCompat mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(this, "PlayService");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                playAudio();
            }

            @Override
            public void onPause() {
                pauseAudio();
            }

            @Override
            public void onSeekTo(long pos) {
                seekAudio((int) pos);
                playAudio();
            }

            @Override
            public void onSkipToNext() {
                sendBroadcastNext();
            }

            @Override
            public void onSkipToPrevious() {
                sendBroadcastPrevious();
            }
        });
        mediaSession.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            Song song = intent.getParcelableExtra("SONG");
            pausedPosition = intent.getIntExtra("MEDIA_POSITION", pausedPosition);
            isRepeat = intent.getBooleanExtra("IS_REPEATING", false);

            if (song != null) {
                currentSong = song;
                String newAudioPath = song.getAudioPath();

                if (newAudioPath != null && !newAudioPath.equals(audioPath)) {
                    if (mediaPlayer != null) {
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                        }
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                    audioPath = newAudioPath;
                    mediaPlayer = MediaPlayer.create(this, Uri.parse(audioPath));
                    if (mediaPlayer != null) {
                        mediaPlayer.setVolume(100, 100);
                        mediaPlayer.setLooping(isRepeat);
                        mediaPlayer.setOnCompletionListener(mp -> {
                            // Gửi broadcast để chuyển bài tiếp theo
                            sendBroadcastNext();
                        });
                    } else {
                        Log.e("PlayService", "MediaPlayer could not be created with the provided audio path: " + audioPath);
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
                    seekAudio(intent.getIntExtra("MEDIA_POSITION", 0));
                    break;
                case "ACTION_PREVIOUS":
                    sendBroadcastPrevious();
                    break;
                case "ACTION_NEXT":
                    sendBroadcastNext();
                    break;
                case "ACTION_TOGGLE_REPEAT": // New action to handle repeat toggle
                    isRepeat = intent.getBooleanExtra("IS_REPEATING", false);
                    mediaPlayer.setLooping(isRepeat);
                    break;
            }
        }
        return START_STICKY;
    }

    private void playAudio() {
        if (mediaPlayer == null) {
            Log.e("PlayService", "MediaPlayer is not initialized.");
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
            Log.d("PlayService", "Media paused at position: " + pausedPosition);
        } else {
            Log.d("PlayService", "MediaPlayer is either null or not playing");
        }
        updateNotification(false);
        sendBroadcastUpdate(false, pausedPosition);
    }

    private void seekAudio(int newPosition){
        if (mediaPlayer != null && newPosition >= 0 && newPosition <= mediaPlayer.getDuration()) {
            mediaPlayer.seekTo(newPosition);
            pausedPosition = newPosition;
            sendBroadcastUpdate(mediaPlayer.isPlaying(), mediaPlayer.getCurrentPosition());
            updateNotification(mediaPlayer.isPlaying());
        }
    }

    private Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();
                Log.e("CurrentPosition", "CurrentPosition" + mediaPlayer.getCurrentPosition());
                // Cập nhật progress của notification
                updateNotification(mediaPlayer.isPlaying());

                // Gửi broadcast để cập nhật SeekBar trên UI
                Intent intent = new Intent("UPDATE_SEEKBAR");
                intent.putExtra("CURRENT_POSITION", currentPosition);
                intent.putExtra("DURATION", duration);
                LocalBroadcastManager.getInstance(PlayServiceMedia.this).sendBroadcast(intent);

                handler.postDelayed(this, 1000); // Cập nhật mỗi giây
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
        Intent intent = new Intent(this, PlayServiceMedia.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        String action = isPlaying ? "ACTION_PAUSE" : "ACTION_PLAY";
        int icon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;

        int currentPosition = mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
        int duration = mediaPlayer != null ? mediaPlayer.getDuration() : 0;

        // Tạo intent để xử lý click vào notification
        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openAppIntent.putExtra("NAVIGATE_TO_FRAGMENT", "FragmentHome");
        PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Tạo intent để xử lý seek
        Intent seekIntent = new Intent(this, PlayServiceMedia.class);
        seekIntent.setAction("ACTION_SEEK"); // Hành động seek
        seekIntent.putExtra("MEDIA_POSITION", currentPosition); // Gửi vị trí hiện tại

        PendingIntent seekPendingIntent = PendingIntent.getService(this, 0, seekIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        mediaSession.setPlaybackState(
                new PlaybackStateCompat.Builder()
                        .setState(
                                isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                                currentPosition,
                                1
                        )
                        .setActions(PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                        .build()
        );
        mediaSession.setMetadata(
                new Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, currentSong.getTitle())
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, currentSong.getArtist())
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, duration) // 4

                        .build()
        );
        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(currentSong != null ? currentSong.getTitle() : "No Song Playing")
                .setContentText(currentSong != null ? currentSong.getArtist() : "")
//                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.image_background)) //Ảnh nền mặc định
                .addAction(R.drawable.ic_previous, "Previous", getPendingIntent("ACTION_PREVIOUS", 0))
                .addAction(icon, isPlaying ? "Pause" : "Play", getPendingIntent(action, 1))  // Nút Play/Pause
                .addAction(R.drawable.ic_next, "Next", getPendingIntent("ACTION_NEXT", 2))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowCancelButton(true))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(seekPendingIntent)
                .setContentIntent(contentPendingIntent);

        if (currentSong != null && currentSong.getImageUrl() != null) {
            Glide.with(this)
                    .asBitmap()
                    .load(currentSong.getImageUrl()) // URL của ảnh bài hát
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            notificationBuilder.setLargeIcon(resource);
                            startForeground(1, notificationBuilder.build());
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
        } else {
            // Hiển thị notification không có ảnh nếu không có URL
            startForeground(1, notificationBuilder.build());
        }
    }

    private PendingIntent getPendingIntent(String action, int requestCode) {
        Intent intent = new Intent(this, PlayServiceMedia.class);
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