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
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;

public class PlayService extends Service {
    public static final String CHANNEL_ID = "BackgroundMusicService";
    private ExoPlayer exoPlayer;
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
                    if (exoPlayer != null) {
                        if (exoPlayer.isPlaying()) {
                            exoPlayer.stop();
                        }
                        exoPlayer.release();
                        exoPlayer = null;
                    }

                    exoPlayer = new ExoPlayer.Builder(this).build();
                    exoPlayer.setAudioAttributes(
                            new AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                            true
                    );
                    exoPlayer.addListener(new Player.Listener() {
                        @Override
                        public void onPlaybackStateChanged(int state) {
                            switch (state) {
                                case Player.STATE_READY:
                                    Log.e("ExoPlayer", "Player is ready");
                                    handler.post(updateProgress); // Bắt đầu cập nhật khi player sẵn sàng
                                    break;
                                case Player.STATE_ENDED:
                                    Log.e("ExoPlayer", "Playback ended");
                                    sendBroadcastNext();
                                    break;
                            }
                        }
                    });

                    audioPath = newAudioPath;
                    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(audioPath));
                    exoPlayer.setMediaItem(mediaItem);
                    exoPlayer.setRepeatMode(isRepeat ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
                    exoPlayer.prepare();
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
                    exoPlayer.setRepeatMode(isRepeat ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
                    break;
            }
        }
        return START_STICKY;
    }

    private void playAudio() {
        if (exoPlayer == null) {
            Log.e("PlayService", "ExoPlayer is not initialized.");
            return;
        }

        if (isPaused) {
            exoPlayer.seekTo(pausedPosition); // Phát từ vị trí đã lưu khi tạm dừng
            exoPlayer.play();
            isPaused = false;
        } else if (!exoPlayer.isPlaying()) {
            exoPlayer.seekTo(pausedPosition); // Phát từ vị trí đã lưu khi tạm dừng
            exoPlayer.play();
        }

        handler.post(updateProgress);
        updateNotification(true);
        sendBroadcastUpdate(true, (int) exoPlayer.getCurrentPosition());
    }

    private void pauseAudio() {
        if (exoPlayer != null && exoPlayer.isPlaying()) {
            pausedPosition = (int) exoPlayer.getCurrentPosition(); // Lưu vị trí hiện tại
            exoPlayer.pause();
            isPaused = true;
        }
        updateNotification(false);
        sendBroadcastUpdate(false, pausedPosition);
    }

    private void seekAudio(int newPosition){
        if (exoPlayer != null && newPosition >= 0 && newPosition <= (int) exoPlayer.getDuration()) {
            exoPlayer.seekTo(newPosition);
            pausedPosition = newPosition;
            sendBroadcastUpdate(exoPlayer.isPlaying(), (int) exoPlayer.getCurrentPosition());
            updateNotification(exoPlayer.isPlaying());
        }
    }

    private Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (exoPlayer != null && exoPlayer.isPlaying()) {
                int currentPosition = (int) exoPlayer.getCurrentPosition();
                int duration = (int) exoPlayer.getDuration();
                Log.e("CurrentPosition", "CurrentPosition" + exoPlayer.getCurrentPosition());

                // Cập nhật progress của notification
                updateNotification(exoPlayer.isPlaying());

                // Gửi broadcast để cập nhật SeekBar trên UI
                Intent intent = new Intent("UPDATE_SEEKBAR");
                intent.putExtra("CURRENT_POSITION", currentPosition);
                intent.putExtra("DURATION", duration);
                LocalBroadcastManager.getInstance(PlayService.this).sendBroadcast(intent);

                handler.postDelayed(this, 100);
            }
        }
    };

    @Override
    public void onDestroy() {
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
        handler.removeCallbacks(updateProgress);
        super.onDestroy();
    }

    private void updateNotification(boolean isPlaying) {
        Intent intent = new Intent(this, PlayService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        String action = isPlaying ? "ACTION_PAUSE" : "ACTION_PLAY";
        int icon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;

        int currentPosition = exoPlayer != null ? (int) exoPlayer.getCurrentPosition() : 0;
        int duration = exoPlayer != null ? (int) exoPlayer.getDuration() : 0;

        // Tạo intent để xử lý click vào notification
        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openAppIntent.putExtra("NAVIGATE_TO_FRAGMENT", "FragmentHome");
        PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Tạo intent để xử lý seek
        Intent seekIntent = new Intent(this, PlayService.class);
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
        Intent intent = new Intent(this, PlayService.class);
        intent.setAction(action);
        intent.putExtra("SONG", currentSong);
        intent.putExtra("CURRENT_POSITION",  (int) exoPlayer.getCurrentPosition());
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
