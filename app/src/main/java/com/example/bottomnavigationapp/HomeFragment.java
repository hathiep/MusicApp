package com.example.bottomnavigationapp;

import static com.example.bottomnavigationapp.BackgroundSoundService.CHANNEL_ID;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.animation.ObjectAnimator;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private LinearLayout layoutPlaying;
    private TextView tvTitle, tvArtist;
    private ImageView imvImagePlaying, imvPlay, imvPrevious, imvNext;
    private boolean isPlaying = false;
    private ListView listView;
    private SongAdapter adapter;
    private List<Song> songList = new ArrayList<>();
    private Song currentSong;  // Thêm biến để lưu song hiện tại
    private ObjectAnimator rotateAnimator;

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
        layoutPlaying = view.findViewById(R.id.layout_playing);
        imvImagePlaying = view.findViewById(R.id.imv_image_playing);
        tvTitle = view.findViewById(R.id.tv_title);
        tvArtist = view.findViewById(R.id.tv_artist);
        imvPlay = view.findViewById(R.id.imv_play);
        imvPrevious = view.findViewById(R.id.imv_previous);
        imvNext = view.findViewById(R.id.imv_next);
        listView = view.findViewById(R.id.listView);

        adapter = new SongAdapter(requireContext(), songList);
        listView.setAdapter(adapter);

        rotateAnimator = ObjectAnimator.ofFloat(imvImagePlaying, "rotation", 0f, 360f);
        rotateAnimator.setDuration(10000); // thời gian quay là 10 giây
        rotateAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        rotateAnimator.setRepeatMode(ObjectAnimator.RESTART);
    }

    private void setOnclick() {
        // Thiết lập sự kiện click cho item trong ListView
        listView.setOnItemClickListener((parent, view, position, id) -> {
            SongAdapter adapter = (SongAdapter) parent.getAdapter();
            adapter.setSelectedPosition(position); // Cập nhật vị trí của item được chọn

            currentSong = songList.get(position);  // Lưu song hiện tại
            layoutPlaying.setVisibility(View.VISIBLE); // Hiển thị trình đang phát
            updatePlayButton();  // Cập nhật nút play/tạm dừng

            // Phát bài hát đã chọn
            startPlayingCurrentSong();
        });

        imvPlay.setOnClickListener(view -> {
            if (currentSong != null) {
                String action = isPlaying ? "ACTION_PAUSE" : "ACTION_PLAY";

                Intent serviceIntent = new Intent(getActivity(), BackgroundSoundService.class);
                serviceIntent.setAction(action);
                serviceIntent.putExtra("SONG", currentSong);  // Truyền đối tượng Song

                getActivity().startService(serviceIntent);

                if (isPlaying) {
                    // Tạm dừng hiệu ứng quay
                    rotateAnimator.pause();
                } else {
                    // Tiếp tục hoặc bắt đầu lại hiệu ứng quay
                    if (!rotateAnimator.isStarted()) {
                        rotateAnimator.start();
                    } else {
                        rotateAnimator.resume();
                    }
                }

                isPlaying = !isPlaying;
                updatePlayButton();  // Cập nhật nút play/tạm dừng
            }
        });

        imvPrevious.setOnClickListener(view -> startPlayingPreviousSong());

        imvNext.setOnClickListener(view -> startPlayingNextSong());
    }

    private void updateInforPlaying(Song song){
        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtist());
    }

    private void updatePlayButton() {
        imvPlay.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void startPlayingCurrentSong() {
        if (currentSong != null) {
            Intent serviceIntent = new Intent(getActivity(), BackgroundSoundService.class);
            serviceIntent.setAction("ACTION_PLAY");
            serviceIntent.putExtra("SONG", currentSong);  // Truyền đối tượng Song
            getActivity().startService(serviceIntent);

            isPlaying = true;
            updateInforPlaying(currentSong);
            updatePlayButton();  // Cập nhật nút play/tạm dừng
            updateNavigationButtons();  // Cập nhật trạng thái của các nút điều hướng

            // Dừng animation hiện tại nếu nó đang chạy
            rotateAnimator.end();

            // Reset lại animation để bắt đầu từ đầu
            rotateAnimator.setFloatValues(0f, 360f);
            rotateAnimator.start();

        }
    }

    private void startPlayingPreviousSong() {
        int currentIndex = songList.indexOf(currentSong);
        if (currentIndex > 0) {
            currentSong = songList.get(currentIndex - 1);
            adapter.setSelectedPosition(currentIndex - 1); // Cập nhật vị trí item hiện tại
            startPlayingCurrentSong();
        } else {
            // Xử lý trường hợp không có bài hát trước (e.g., thông báo hoặc vô hiệu hóa nút)
        }
    }

    private void startPlayingNextSong() {
        int currentIndex = songList.indexOf(currentSong);
        if (currentIndex < songList.size() - 1) {
            currentSong = songList.get(currentIndex + 1);
            adapter.setSelectedPosition(currentIndex + 1); // Cập nhật vị trí item hiện tại
            startPlayingCurrentSong();
        } else {
            // Xử lý trường hợp không có bài hát tiếp theo (e.g., thông báo hoặc vô hiệu hóa nút)
        }
    }

    private void updateNavigationButtons() {
        int currentIndex = songList.indexOf(currentSong);

        // Vô hiệu hóa nút Previous nếu là bài hát đầu tiên
        imvPrevious.setEnabled(currentIndex > 0);
        imvPrevious.setBackgroundTintList(getResources().getColorStateList(currentIndex > 0 ? R.color.black : R.color.gray));

        // Vô hiệu hóa nút Next nếu là bài hát cuối cùng
        imvNext.setEnabled(currentIndex < songList.size() - 1);
        imvNext.setBackgroundTintList(getResources().getColorStateList(currentIndex < songList.size() - 1 ? R.color.black : R.color.gray));

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
        IntentFilter filter = new IntentFilter();
        filter.addAction("UPDATE_PLAY_STATE");
        filter.addAction("ACTION_PREVIOUS");
        filter.addAction("ACTION_NEXT");

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(playStateReceiver, filter);
    }

    private BroadcastReceiver playStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("UPDATE_PLAY_STATE".equals(action)) {
                if (intent.hasExtra("isPlaying")) {
                    isPlaying = intent.getBooleanExtra("isPlaying", false);
                    updatePlayButton();
                }
            } else if ("ACTION_PREVIOUS".equals(action)) {
                startPlayingPreviousSong();
            } else if ("ACTION_NEXT".equals(action)) {
                startPlayingNextSong();
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
