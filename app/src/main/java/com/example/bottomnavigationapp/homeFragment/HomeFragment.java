package com.example.bottomnavigationapp.homeFragment;

import static android.content.ContentValues.TAG;
import static com.example.bottomnavigationapp.homeFragment.BackgroundSoundService.CHANNEL_ID;

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

import com.example.bottomnavigationapp.R;
import com.example.bottomnavigationapp.Song;
import com.example.bottomnavigationapp.SongAdapter;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import android.util.TypedValue;
import android.widget.ViewSwitcher;


public class HomeFragment extends Fragment implements HomeContract.View {
    //    private LinearLayout layoutPlaying;
    private TextView tvTitle, tvArtist, tvPosition, tvDuration;
    private ImageView imvPullDown, imvImagePlaying, imvPlay, imvPrevious, imvNext;
    private boolean isPlaying = false;
    private ListView listView;
    private SongAdapter adapter;
    private List<Song> songList = new ArrayList<>();
    private Song currentSong;
    private ObjectAnimator rotateAnimator;
    private SeekBar seekBar;
    private Handler seekBarHandler;
    private int currentMediaPosition = 0;
    private boolean isUserSeeking, seekbarStarted;
    private ViewSwitcher viewSwitcher;
    private View collapsedView, expandedView;
    private float currentRotation = 0f;
    private ProgressBar progressBar;
    private HomeContract.Presenter presenter;

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

        presenter = new HomePresenter(this);
        presenter.loadSongs();
        adapter = new SongAdapter(requireContext(), songList);
        listView.setAdapter(adapter);

        rotateAnimator = ObjectAnimator.ofFloat(imvImagePlaying, "rotation", 0f, 360f);
        rotateAnimator.setDuration(10000); // thời gian quay là 10 giây
        rotateAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        rotateAnimator.setRepeatMode(ObjectAnimator.RESTART);
        rotateAnimator.setInterpolator(new LinearInterpolator()); // Sử dụng LinearInterpolator để quay đều

        seekbarStarted = false;
        seekBarHandler = new Handler();
    }

    private void setOnclick() {
        // Thiết lập sự kiện click cho item trong ListView
        listView.setOnItemClickListener((parent, view, position, id) -> {
            SongAdapter adapter = (SongAdapter) parent.getAdapter();
            adapter.setSelectedPosition(position); // Cập nhật vị trí của item được chọn
            currentSong = songList.get(position);  // Lưu song hiện tại
            viewSwitcher.setVisibility(View.VISIBLE);
            updatePlayButton(isPlaying);  // Cập nhật nút play/tạm dừng
            updatePreNextButton();
            registerReceiver();
            presenter.onSongSelected(songList.get(position));
        });

        imvPlay.setOnClickListener(view -> {
            registerReceiver();
            presenter.onPlayPauseClicked();
        });
        imvPrevious.setOnClickListener(view -> presenter.onPreviousClicked());
        imvNext.setOnClickListener(view -> presenter.onNextClicked());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onSeekBarProgressChanged(progress, fromUser);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                currentMediaPosition = seekBar.getProgress();
                presenter.onSeekBarStopTrackingTouch(currentMediaPosition);
            }
        });

        // Bắt sự kiện click vào layoutPlaying để mở rộng
        if (viewSwitcher.getCurrentView().getId() == R.id.layout_playing_collapsed)
            viewSwitcher.getChildAt(0).setOnClickListener(view -> updatePlayingLayout(1, expandedView));

        // Sự kiện thu nhỏ layout khi click vào imv_pull_down
        imvPullDown.setOnClickListener(view -> updatePlayingLayout(0, collapsedView));
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
                    updatePlayButton(isPlaying);
                }
            } else if ("ACTION_PREVIOUS".equals(action)) {
                presenter.onPreviousClicked();
            } else if ("ACTION_NEXT".equals(action)) {
                presenter.onNextClicked();
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
        saveCurrentRotation();
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
        updatePlayingSongInfo(currentSong);
        updatePlayButton(isPlaying);
        updatePreNextButton();
        setOnclick();
        viewSwitcher.setDisplayedChild(i);
        listView.setVisibility(i==1 ? View.GONE : View.VISIBLE);
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

    @Override
    public void showSongs(List<Song> songs) {
        songList.clear();
        songList.addAll(songs);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void showLoadingIndicator(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void updatePlayingSongInfo(Song song) {
        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtist());
    }

    @Override
    public void updatePlayButton(boolean isPlaying) {
        imvPlay.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        if (!isPlaying) {
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

    @Override
    public void updatePreNextButton() {
        currentSong = presenter.getCurrentSong();
        int currentIndex = songList.indexOf(currentSong);

        // Vô hiệu hóa nút Previous nếu là bài hát đầu tiên
        imvPrevious.setEnabled(currentIndex > 0);
        imvPrevious.setBackgroundTintList(getResources().getColorStateList(currentIndex > 0 ? R.color.black : R.color.gray));

        // Vô hiệu hóa nút Next nếu là bài hát cuối cùng
        imvNext.setEnabled(currentIndex < songList.size() - 1);
        imvNext.setBackgroundTintList(getResources().getColorStateList(currentIndex < songList.size() - 1 ? R.color.black : R.color.gray));
    }

    @Override
    public void updateSeekBar() {
        currentMediaPosition = 0;
        isUserSeeking = false;
        seekbarStarted = false;
    }

    @Override
    public void onSeekBarProgressChanged(int progress, boolean fromUser) {
        if (!seekbarStarted) {
            // Dừng animation hiện tại nếu nó đang chạy
            rotateAnimator.end();
            // Reset lại animation để bắt đầu từ đầu
            rotateAnimator.setFloatValues(0f, 360f);
            rotateAnimator.start();
            // Đánh dấu rằng SeekBar đã thay đổi
            seekbarStarted = true;
        }
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
    public void updateAdapter(int position){
        adapter.setSelectedPosition(position); // Cập nhật vị trí item hiện tại
    }
}