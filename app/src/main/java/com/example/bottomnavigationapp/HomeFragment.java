package com.example.bottomnavigationapp;

import static com.example.bottomnavigationapp.BackgroundSoundService.CHANNEL_ID;

import android.annotation.SuppressLint;

import androidx.core.content.ContextCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import androidx.core.app.NotificationCompat;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.OptIn;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.legacy.MediaSessionCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class HomeFragment extends Fragment {

    private Button btnPlay;
    private boolean isPlaying = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        init(view);
        createNotificationChannel();
        setOnclick();

        return view;
    }

    private void init(View view) {
        btnPlay = view.findViewById(R.id.btn_play);
    }

    private void setOnclick() {
        btnPlay.setOnClickListener(view -> {
            if (isPlaying) {
                getActivity().startService(new Intent(getActivity(), BackgroundSoundService.class).setAction("ACTION_PAUSE"));
            } else {
                getActivity().startService(new Intent(getActivity(), BackgroundSoundService.class).setAction("ACTION_PLAY"));
            }

            // Gửi thông báo sau khi cập nhật trạng thái isPlaying
            isPlaying = !isPlaying;
            sendNotificationMedia();

            // Cập nhật giao diện nút
            btnPlay.setText(isPlaying ? "Pause" : "Play");
            btnPlay.setBackgroundColor(ContextCompat.getColor(getContext(), isPlaying ? R.color.red : R.color.blue));
        });
    }


    private PendingIntent getPendingIntent(String action) {
        Intent intent = new Intent(getActivity(), BackgroundSoundService.class);
        intent.setAction(action);
        return PendingIntent.getService(getActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @OptIn(markerClass = UnstableApi.class)
    @SuppressLint("MissingPermission")
    private void sendNotificationMedia() {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.image_backround);

        // Khởi tạo MediaSessionCompat
        @SuppressLint("RestrictedApi") MediaSessionCompat mediaSessionCompat = new MediaSessionCompat(requireContext(), "tag");

        // Xác định hành động nút play/pause dựa trên trạng thái hiện tại
        String action = isPlaying ? "ACTION_PAUSE" : "ACTION_PLAY";
        int icon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        String title = isPlaying ? "Pause" : "Play";

        // Xây dựng thông báo
        Notification notification = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setSubText("Music Tina")
                .setContentTitle("Title of song")
                .setContentText("Single of song")
                .setLargeIcon(bitmap)
                .addAction(R.drawable.ic_previous, "Previous", getPendingIntent("ACTION_PREVIOUS")) // #0
                .addAction(icon, title, getPendingIntent(action))  // #1: Play/Pause button
                .addAction(R.drawable.ic_next, "Next", getPendingIntent("ACTION_NEXT"))     // #2
                .setStyle(new MediaStyle()
                        .setShowActionsInCompactView(1 /* #1: play/pause button */))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        // Gửi thông báo
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(requireContext());
        managerCompat.notify(1, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Background Sound Channel",
                    NotificationManager.IMPORTANCE_LOW // Điều chỉnh mức độ ưu tiên
            );
            NotificationManager manager = requireContext().getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }


}
