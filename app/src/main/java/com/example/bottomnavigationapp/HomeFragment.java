package com.example.bottomnavigationapp;

import static com.example.bottomnavigationapp.BackgroundSoundService.CHANNEL_ID;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private ImageView imvPlay;
    private boolean isPlaying = false;
    private ListView listView;
    private SongAdapter adapter;
    private List<Song> songList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        init(view);
        createNotificationChannel();
        setOnclick();
        registerReceiver();
        loadSongsFromFirestore();  // Load songs from Firestore

        return view;
    }

    private void init(View view) {
        imvPlay = view.findViewById(R.id.imv_play);
        listView = view.findViewById(R.id.listView);

        adapter = new SongAdapter(requireContext(), songList);
        listView.setAdapter(adapter);
    }

    private void setOnclick() {
        imvPlay.setOnClickListener(view -> {
            if (!songList.isEmpty()) {
                String action = isPlaying ? "ACTION_PAUSE" : "ACTION_PLAY";
                Song firstSong = songList.get(0);  // Play the first song in the list

                Intent serviceIntent = new Intent(getActivity(), BackgroundSoundService.class);
                serviceIntent.setAction(action);
                serviceIntent.putExtra("AUDIO_PATH", firstSong.getAudioPath());

                getActivity().startService(serviceIntent);

                isPlaying = !isPlaying;
                imvPlay.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            }
        });
    }

    private void loadSongsFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference songsRef = db.collection("songs");

        songsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                songList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String id = document.getId(); // Lấy ID của tài liệu
                    Song song = document.toObject(Song.class);
                    song.setId(id);  // Cập nhật ID cho đối tượng Song
                    songList.add(song);
                }
                fetchAudioPathsAndUpdateSongs(); // Fetch audio paths from Firebase Storage
            } else {
                // Handle error
            }
        });
    }

    private void fetchAudioPathsAndUpdateSongs() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("songs");

        for (Song song : songList) {
            StorageReference songRef = storageRef.child(song.getId() + ".mp3");
            songRef.getDownloadUrl().addOnSuccessListener(uri -> {
                song.setAudioPath(uri.toString()); // Update audioPath with download URL
                adapter.notifyDataSetChanged(); // Notify adapter about data change
            }).addOnFailureListener(exception -> {
                // Handle error
            });
        }
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
                imvPlay.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
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
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = requireContext().getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
