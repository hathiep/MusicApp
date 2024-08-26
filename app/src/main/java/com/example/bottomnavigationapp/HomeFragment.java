package com.example.bottomnavigationapp;

import static com.example.bottomnavigationapp.BackgroundSoundService.CHANNEL_ID;

import android.annotation.SuppressLint;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.app.NotificationCompat.MediaStyle;

import androidx.core.app.NotificationCompat;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
        registerReceiver();

        return view;
    }

    private void init(View view) {
        btnPlay = view.findViewById(R.id.btn_play);
    }

    private void setOnclick() {
        btnPlay.setOnClickListener(view -> {
            String action = isPlaying ? "ACTION_PAUSE" : "ACTION_PLAY";
            getActivity().startService(new Intent(getActivity(), BackgroundSoundService.class).setAction(action));

            // Cập nhật trạng thái nút
            isPlaying = !isPlaying;
            btnPlay.setText(isPlaying ? "Pause" : "Play");
            btnPlay.setBackgroundColor(ContextCompat.getColor(getContext(), isPlaying ? R.color.red : R.color.blue));
        });
    }

    private void registerReceiver() {
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(playStateReceiver,
                new IntentFilter("UPDATE_PLAY_STATE"));
    }

    private BroadcastReceiver playStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.hasExtra("isPlaying")) {
                isPlaying = intent.getBooleanExtra("isPlaying", false);
                btnPlay.setText(isPlaying ? "Pause" : "Play");
                btnPlay.setBackgroundColor(ContextCompat.getColor(getContext(), isPlaying ? R.color.red : R.color.blue));
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(playStateReceiver);
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
