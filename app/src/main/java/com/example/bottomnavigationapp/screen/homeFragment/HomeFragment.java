package com.example.bottomnavigationapp.screen.homeFragment;

import static android.content.ContentValues.TAG;
import static com.example.bottomnavigationapp.service.BackgroundSoundService.CHANNEL_ID;

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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.bottomnavigationapp.R;
import com.example.bottomnavigationapp.model.Song;
import com.example.bottomnavigationapp.screen.homeFragment.adapter.SongAdapter;
import com.example.bottomnavigationapp.service.BackgroundSoundService;

import java.util.ArrayList;
import java.util.List;

import android.widget.Toast;
import android.widget.ViewSwitcher;


public class HomeFragment extends Fragment implements HomeContract.View {
    //    private LinearLayout layoutPlaying;
    private EditText edtSearch;
    private TextView tvNoSong, tvTitle, tvArtist, tvPosition, tvDuration;
    private ImageView imvDelete, imvSearch, imvPullDown, imvImagePlaying, imvPlay, imvPrevious, imvNext, imvCancel, imvRepeat;
    private RelativeLayout.LayoutParams listViewLayoutParams;
    private ListView listView;
    private SongAdapter adapter;
    private List<Song> songList;
    private Song currentSong;
    private ObjectAnimator rotateAnimator;
    private SeekBar seekBar;
    private Handler seekBarHandler;
    private int currentMediaPosition = 0;
    private boolean isPlaying = false, isUserSeeking, seekbarStarted, isRepeat;
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
        registerReceiver();
        setOnclick();

        return view;
    }

    private void init(View view) {
        edtSearch = view.findViewById(R.id.edt_search);
        imvSearch = view.findViewById(R.id.imv_search);
        imvDelete = view.findViewById(R.id.imv_delete);
        tvNoSong = view.findViewById(R.id.tv_no_song);
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
        imvCancel = collapsedView.findViewById(R.id.imv_cancel);
        imvRepeat = collapsedView.findViewById(R.id.imv_repeat);
        seekBar = collapsedView.findViewById(R.id.seekBar);
        listView = view.findViewById(R.id.listView);

        presenter = new HomePresenter(this);
        songList = new ArrayList<>();
        adapter = new SongAdapter(requireContext(), songList);
        listView.setAdapter(adapter);

        rotateAnimator = ObjectAnimator.ofFloat(imvImagePlaying, "rotation", 0f, 360f);
        rotateAnimator.setDuration(10000); // thời gian quay là 10 giây
        rotateAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        rotateAnimator.setRepeatMode(ObjectAnimator.RESTART);
        rotateAnimator.setInterpolator(new LinearInterpolator()); // Sử dụng LinearInterpolator để quay đều

        seekbarStarted = false;
        seekBarHandler = new Handler();
        isRepeat = false;
    }

    private void setOnclick() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                imvDelete.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    imvDelete.setVisibility(View.GONE);
//                    updateSearch();
                }
            }
        });

        edtSearch.setOnClickListener(view -> edtSearch.setCursorVisible(true));

        imvDelete.setOnClickListener(view -> {
            edtSearch.setText("");
//            updateSearch();
        });

        imvSearch.setOnClickListener(view -> {
            String artist = edtSearch.getText().toString().trim();
            if(artist.equals(""))
                Toast.makeText(getContext(), R.string.toast_search, Toast.LENGTH_SHORT).show();
            else{
                songList.clear();
                adapter.setSelectedPosition(-1);
                adapter.notifyDataSetChanged();
                presenter.loadSongs(artist);
                edtSearch.setCursorVisible(false);
            }
        });

        listViewLayoutParams = (RelativeLayout.LayoutParams) listView.getLayoutParams();
        // Thiết lập sự kiện click cho item trong ListView
        listView.setOnItemClickListener((parent, view, position, id) -> {
            SongAdapter adapter = (SongAdapter) parent.getAdapter();
            adapter.setSelectedPosition(position); // Cập nhật vị trí của item được chọn
            currentSong = songList.get(position);  // Lưu song hiện tại
            viewSwitcher.setVisibility(View.VISIBLE);
            listViewLayoutParams.setMargins(0, 0, 0, 290);
            listView.setLayoutParams(listViewLayoutParams);
            updatePlayButton(isPlaying);  // Cập nhật nút play/tạm dừng
            updatePreNextButton();
            presenter.onSongSelected(songList.get(position));
        });

        imvPlay.setOnClickListener(view -> {
            presenter.onPlayPauseClicked();
        });
        imvPrevious.setOnClickListener(view -> {
            presenter.onPreviousClicked();
        });
        imvNext.setOnClickListener(view -> {
            presenter.onNextClicked();
        });

        imvCancel.setOnClickListener(view -> {
            presenter.onCancelClicked();
        });

        imvRepeat.setOnClickListener(view -> {
            isRepeat = !isRepeat;
            imvRepeat.setImageResource(isRepeat ? R.drawable.ic_repeat : R.drawable.ic_unrepeat);
            presenter.onRepeatClicked(isRepeat);
        });


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

    @Override
    public void showSongs(List<Song> songs) {
        songList.clear();
        songList.addAll(songs);
        if(songs.isEmpty()){
            listView.setVisibility(View.GONE);
            tvNoSong.setVisibility(View.VISIBLE);
        }
        else {
            listView.setVisibility(View.VISIBLE);
            tvNoSong.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void showLoadingIndicator(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void updatePlayingSongInfo(Song song) {
        // Sử dụng Glide để tải và hiển thị ảnh từ URL
        Glide.with(this)
                .load(song.getImageUrl()) // URL của ảnh
                .placeholder(R.drawable.ic_logo) // Ảnh placeholder khi đang tải
                .error(R.drawable.ic_logo) // Ảnh hiển thị khi lỗi tải
                .into(imvImagePlaying); // Đưa ảnh vào ImageView
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
        updateActionButton(imvPrevious, currentIndex > 0);

        // Vô hiệu hóa nút Next nếu là bài hát cuối cùng
        updateActionButton(imvNext, currentIndex < songList.size() - 1);
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
    public void onCancelClicked(){
        viewSwitcher.setVisibility(View.GONE);
        listViewLayoutParams.setMargins(0, 0, 0, 0);
        adapter.setSelectedPosition(-1);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void updateAdapter(int position){
        adapter.setSelectedPosition(position); // Cập nhật vị trí item hiện tại
    }

    private void updateSearch(){
        songList.clear();
        adapter.notifyDataSetChanged();
        updateActionButton(imvPrevious, false);
        updateActionButton(imvNext, false);
    }

    private void updateActionButton(ImageView imv, boolean status){
        imv.setEnabled(status);
        imv.setBackgroundTintList(getResources().getColorStateList(status ? R.color.black : R.color.gray));
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

    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
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
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(playStateReceiver);
    }

}