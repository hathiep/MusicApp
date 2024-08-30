package com.example.bottomnavigationapp;

import static android.content.ContentValues.TAG;
import static com.example.bottomnavigationapp.BackgroundSoundService.CHANNEL_ID;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
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
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import android.util.TypedValue;
import android.widget.ViewSwitcher;


public class HomeFragment extends Fragment {
//    private LinearLayout layoutPlaying;
    private TextView tvTitle, tvArtist, tvPosition, tvDuration;
    private ImageView imvPullDown, imvImagePlaying, imvPlay, imvPrevious, imvNext;
    private boolean isPlaying = false;
    private ListView listView;
    private SongAdapter adapter;
    private List<Song> songList = new ArrayList<>();
    private Song currentSong;  // Thêm biến để lưu song hiện tại
    private ObjectAnimator rotateAnimator;
    private SeekBar seekBar;
    private Handler seekBarHandler;
    private int currentMediaPosition = 0; // Biến lưu vị trí hiện tại của media
    private boolean isUserSeeking;
    private ViewSwitcher viewSwitcher;
    private View collapsedView, expandedView;
    private float currentRotation = 0f;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        init(view);
        createNotificationChannel();
        setOnclick();

        return view;
    }

    private void init(View view) {
        progressBar = view.findViewById(R.id.progressBar);
        viewSwitcher = view.findViewById(R.id.view_switcher);
        collapsedView = viewSwitcher.getChildAt(0);
        expandedView = viewSwitcher.getChildAt(1);
        imvPullDown = view.findViewById(R.id.imv_pull_down);
        imvImagePlaying = collapsedView.findViewById(R.id.imv_image_playing);
        tvTitle = collapsedView.findViewById(R.id.tv_title);
        tvArtist = collapsedView.findViewById(R.id.tv_artist);
        tvPosition = collapsedView.findViewById(R.id.tv_position);
        tvDuration = collapsedView.findViewById(R.id.tv_duration);
        imvPlay = collapsedView.findViewById(R.id.imv_play);
        imvPrevious = collapsedView.findViewById(R.id.imv_previous);
        imvNext = collapsedView.findViewById(R.id.imv_next);
        seekBar = collapsedView.findViewById(R.id.seekBar);
        listView = view.findViewById(R.id.listView);

        loadSongsFromFirestore();  // Load songs from Firestore
        adapter = new SongAdapter(requireContext(), songList);
        listView.setAdapter(adapter);

        rotateAnimator = ObjectAnimator.ofFloat(imvImagePlaying, "rotation", 0f, 360f);
        rotateAnimator.setDuration(10000); // thời gian quay là 10 giây
        rotateAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        rotateAnimator.setRepeatMode(ObjectAnimator.RESTART);
        rotateAnimator.setInterpolator(new LinearInterpolator()); // Sử dụng LinearInterpolator để quay đều

        seekBarHandler = new Handler();
    }

    private void setOnclick() {
        // Thiết lập sự kiện click cho item trong ListView
        listView.setOnItemClickListener((parent, view, position, id) -> {
            SongAdapter adapter = (SongAdapter) parent.getAdapter();
            adapter.setSelectedPosition(position); // Cập nhật vị trí của item được chọn

            currentSong = songList.get(position);  // Lưu song hiện tại
//            layoutPlaying.setVisibility(View.VISIBLE); // Hiển thị trình đang phát
            viewSwitcher.setVisibility(View.VISIBLE);
            updatePlayButton();  // Cập nhật nút play/tạm dừng

            registerReceiver();
            // Phát bài hát đã chọn
            startPlayingCurrentSong();
        });

        imvPlay.setOnClickListener(view -> {
            if (currentSong != null) {
                String action = isPlaying ? "ACTION_PAUSE" : "ACTION_PLAY";

                sendActionToService(action);
                registerReceiver();

                isPlaying = !isPlaying;
                updatePlayButton();  // Cập nhật nút play/tạm dừng
            }
        });

        imvPrevious.setOnClickListener(view -> startPlayingPreviousSong());

        imvNext.setOnClickListener(view -> startPlayingNextSong());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Khi người dùng kéo SeekBar, cập nhật vị trí hiển thị nhưng không thay đổi tiến trình phát nhạc
                    isUserSeeking = true;
                    currentMediaPosition = seekBar.getProgress();
                    tvPosition.setText(formatTime(currentMediaPosition));
                    seekBarHandler.removeCallbacks(updateProgress);
                }
                else {
                    isUserSeeking = false;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                currentMediaPosition = seekBar.getProgress();
                sendActionToService("ACTION_PAUSE");
                sendActionToService("ACTION_SEEK", currentMediaPosition);
                sendActionToService("ACTION_PLAY");
            }
        });

        // Bắt sự kiện click vào layoutPlaying để mở rộng
        if (viewSwitcher.getCurrentView().getId() == R.id.layout_playing_collapsed)
            viewSwitcher.getChildAt(0).setOnClickListener(view -> updatePlayingLayout(1, expandedView));

        // Sự kiện thu nhỏ layout khi click vào imv_pull_down
        imvPullDown.setOnClickListener(view -> updatePlayingLayout(0, collapsedView));
    }

    private void sendActionToService(String action) {
        Intent intent = new Intent(getContext(), BackgroundSoundService.class);
        intent.setAction(action);
        intent.putExtra("SONG", currentSong);  // Truyền đối tượng Song
        getContext().startService(intent);
    }

    private void sendActionToService(String action, int position) {
        Intent intent = new Intent(getContext(), BackgroundSoundService.class);
        intent.setAction(action);
        intent.putExtra("SONG", currentSong);  // Truyền đối tượng Song
        intent.putExtra("MEDIA_POSITION", position);
        getContext().startService(intent);
    }

    private void updateInforPlaying(Song song){
        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtist());
    }

    private void updateImagePlaying(Boolean isPlaying){
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
    }

    private void updatePlayButton() {
        imvPlay.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        updateImagePlaying(!isPlaying);
    }

    private void startPlayingCurrentSong() {
        if (currentSong != null) {
            currentMediaPosition = 0;
            isUserSeeking = false;
            Intent serviceIntent = new Intent(getActivity(), BackgroundSoundService.class);
            serviceIntent.setAction("ACTION_PLAY");
            serviceIntent.putExtra("SONG", currentSong);  // Truyền đối tượng Song
            serviceIntent.putExtra("MEDIA_POSITION", currentMediaPosition); // Truyền vị trí hiện tại
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

        progressBar.setVisibility(View.VISIBLE);

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
                progressBar.setVisibility(View.GONE);
                song.setAudioPath(uri.toString()); // Update audioPath with download URL
                adapter.notifyDataSetChanged(); // Notify adapter about data change
            }).addOnFailureListener(exception -> {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error fetching audio path: ", exception);
            });
        }
    }

    private Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (currentSong != null && isPlaying && !isUserSeeking) {
                // Cập nhật SeekBar dựa trên tiến trình hiện tại
                seekBar.setProgress(currentMediaPosition);
                tvPosition.setText(formatTime(currentMediaPosition));
                seekBarHandler.postDelayed(this, 1000); // Tiếp tục cập nhật mỗi giây
            }
        }
    };


    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("UPDATE_PLAY_STATE");
        filter.addAction("ACTION_PREVIOUS");
        filter.addAction("ACTION_NEXT");
        filter.addAction("UPDATE_SEEKBAR");

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
            } else if ("UPDATE_SEEKBAR".equals(action)) {
                int duration = intent.getIntExtra("DURATION", 0);
                int currentPosition = intent.getIntExtra("CURRENT_POSITION", 0);
                seekBar.setMax(duration);
                if(!isUserSeeking) {
                    seekBar.setProgress(currentPosition);
                    tvPosition.setText(formatTime(currentPosition));
                }
                tvDuration.setText(formatTime(duration));
            }
        }
    };

    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(playStateReceiver);
    }

    private void updatePlayingLayout(int i, View view) {
        listView.setVisibility(i==1 ? View.GONE : View.VISIBLE);
        saveCurrentRotation();
        viewSwitcher.setDisplayedChild(i);
        imvImagePlaying = view.findViewById(R.id.imv_image_playing);
        tvTitle = view.findViewById(R.id.tv_title);
        tvArtist = view.findViewById(R.id.tv_artist);
        tvPosition = view.findViewById(R.id.tv_position);
        tvDuration = view.findViewById(R.id.tv_duration);
        imvPlay = view.findViewById(R.id.imv_play);
        imvPrevious = view.findViewById(R.id.imv_previous);
        imvNext = view.findViewById(R.id.imv_next);
        seekBar = view.findViewById(R.id.seekBar);
        rotateAnimator.pause();
        rotateAnimator = ObjectAnimator.ofFloat(imvImagePlaying, "rotation", 0f, 360f);
        rotateAnimator.setDuration(10000); // thời gian quay là 10 giây
        rotateAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        rotateAnimator.setInterpolator(new LinearInterpolator()); // Sử dụng LinearInterpolator để quay đều
        restoreRotation();
        updateInforPlaying(currentSong);
        updatePlayButton();
        updateNavigationButtons();
        setOnclick();
    }

    private void saveCurrentRotation() {
        if (rotateAnimator != null) {
            currentRotation = (float) rotateAnimator.getAnimatedValue();
            rotateAnimator.cancel(); // Dừng animation hiện tại
        }
    }

    private void restoreRotation() {
        if (rotateAnimator != null) {
            rotateAnimator.setFloatValues(currentRotation, 360f + currentRotation);
            rotateAnimator.start();
        }
    }


    public int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
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
