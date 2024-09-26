package com.example.bottomnavigationapp.screen.homeFragment;

import static com.example.bottomnavigationapp.service.PlayService.CHANNEL_ID;

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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.bottomnavigationapp.R;
import com.example.bottomnavigationapp.model.Song;
import com.example.bottomnavigationapp.screen.homeFragment.adapter.SongAdapter;

import java.util.ArrayList;
import java.util.List;

import android.widget.Toast;
import android.widget.ViewSwitcher;


public class HomeFragment extends Fragment implements HomeContract.View {
    private EditText edtSearch;
    private TextView tvNoSong, tvTitle, tvArtist, tvPosition, tvDuration;
    private ImageView imvDelete, imvSearch, imvPullDown, imvImagePlaying, imvPlay, imvPrevious, imvNext, imvCancel, imvRepeat;
    private ListView listView;
    private SongAdapter adapter;
    private List<Song> songList;
    private Song currentSong;
    private ObjectAnimator rotateAnimator;
    private SeekBar seekBar;
    private Handler seekBarHandler;
    private int currentMediaPosition = 0;
    private boolean isPlaying = false, isUserSeeking, seekbarStarted, isRepeat, isExpanded = false;
    private ViewSwitcher viewSwitcher;
    private View view, collapsedView, expandedView;
    private float currentRotation = 0f;
    private ProgressBar progressBar;
    private HomeContract.Presenter presenter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_home, container, false);

        init();
        createNotificationChannel();
        registerReceiver();
        setOnclick();

        return view;
    }

    private void init() {
        edtSearch = view.findViewById(R.id.edt_search);
        imvSearch = view.findViewById(R.id.imv_search);
        imvDelete = view.findViewById(R.id.imv_delete);
        tvNoSong = view.findViewById(R.id.tv_no_song);
        progressBar = view.findViewById(R.id.progressBar);

        viewSwitcher = getActivity().findViewById(R.id.view_switcher);

        if (viewSwitcher != null) {
            collapsedView = viewSwitcher.getChildAt(0);
            expandedView = viewSwitcher.getChildAt(1);
            viewSwitcher.setDisplayedChild(0);

            imvPullDown = viewSwitcher.findViewById(R.id.imv_pull_down);
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
        } else {
            Log.e("HomeFragment", "ViewSwitcher not found in MainActivity layout.");
        }

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
                if (s.length() == 0) imvDelete.setVisibility(View.GONE);
            }
        });

        edtSearch.setOnClickListener(view -> edtSearch.setCursorVisible(true));

        edtSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // Kiểm tra xem người dùng có nhấn nút "Tìm kiếm" trên bàn phím hay không
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // Gọi hàm tìm kiếm
                    performSearch();
                    return true;
                }
                return false;
            }
        });

        imvDelete.setOnClickListener(view -> edtSearch.setText(""));

        imvSearch.setOnClickListener(view -> performSearch());

        // Thiết lập sự kiện click cho item trong ListView
        listView.setOnItemClickListener((parent, view, position, id) -> {
            SongAdapter adapter = (SongAdapter)  parent.getAdapter();
            adapter.setSelectedPosition(position); // Cập nhật vị trí của item được chọn
            currentSong = songList.get(position);  // Lưu song hiện tại
            presenter.onSongSelected(songList.get(position));
            updatePlayingLayout(0, viewSwitcher.getChildAt(0));
        });

        imvPlay.setOnClickListener(view -> presenter.onPlayPauseClicked());
        imvPrevious.setOnClickListener(view -> presenter.onPreviousClicked());
        imvNext.setOnClickListener(view -> presenter.onNextClicked());
        imvCancel.setOnClickListener(view -> presenter.onCancelClicked());

        imvRepeat.setOnClickListener(view -> {
            isRepeat = !isRepeat;
            imvRepeat.setBackgroundResource(isRepeat ? R.drawable.ic_repeat : R.drawable.ic_unrepeat);
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

    private void performSearch() {
        // Gọi hàm ẩn bàn phím
        hideKeyboard();
        String artist = edtSearch.getText().toString().trim();
        if (artist.equals(""))
            Toast.makeText(getContext(), R.string.toast_search, Toast.LENGTH_SHORT).show();
        else {
            songList.clear();
            adapter.setSelectedPosition(-1);
            adapter.notifyDataSetChanged();
            presenter.loadSongs(artist);
            edtSearch.setCursorVisible(false);
        }
    }

    // Hàm ẩn bàn phím
    private void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) ContextCompat.getSystemService(getActivity(), InputMethodManager.class);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void showSongs(List<Song> songs) {
        songList.clear();
        songList.addAll(songs);
        if (songs.isEmpty()) {
            listView.setVisibility(View.GONE);
            tvNoSong.setVisibility(View.VISIBLE);
        } else {
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
        Glide.with(this).load(song.getImageUrl()) // URL của ảnh
                .placeholder(R.drawable.ic_logo) // Ảnh placeholder khi đang tải
                .error(R.drawable.ic_logo) // Ảnh hiển thị khi lỗi tải
                .into(imvImagePlaying); // Đưa ảnh vào ImageView
        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtist());
    }

    @Override
    public void updatePlayButton(boolean isPlaying) {
        imvPlay.setBackgroundResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
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
    public void updatePreNextButton(boolean isCollapsed) {
        currentSong = presenter.getCurrentSong();
        int currentIndex = songList.indexOf(currentSong);

        // Vô hiệu hóa nút Previous nếu là bài hát đầu tiên
        updateActionButton(imvPrevious, isCollapsed, currentIndex > 0);

        // Vô hiệu hóa nút Next nếu là bài hát cuối cùng
        updateActionButton(imvNext, isCollapsed, currentIndex < songList.size() - 1);
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
        } else {
            isUserSeeking = false;
        }
    }

    @Override
    public void onCancelClicked() {
        viewSwitcher.setDisplayedChild(0);
        viewSwitcher.setVisibility(View.GONE);
        adapter.setSelectedPosition(-1);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void updateAdapter(int position) {
        adapter.setSelectedPosition(position); // Cập nhật vị trí item hiện tại
    }

    private void updateActionButton(ImageView imv, boolean isCollapsed, boolean status) {
        int colorEnable = isCollapsed ? R.color.black : R.color.white;
        int colorUnable = isCollapsed ? R.color.gray : R.color.gray2;
        imv.setEnabled(status);
        imv.setBackgroundTintList(getResources().getColorStateList(status ? colorEnable : colorUnable));
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
                if (!isUserSeeking) {
                    seekBar.setProgress(currentPosition);
                    tvPosition.setText(formatTime(currentPosition));
                }
                tvDuration.setText(formatTime(duration));
            }
        }
    };

    public void updatePlayingLayout(int i, View view) {
        saveCurrentRotation();
        viewSwitcher.setVisibility(View.VISIBLE);
        isExpanded = (i == 1);
        imvImagePlaying = view.findViewById(R.id.imv_image_playing);
        tvTitle = view.findViewById(R.id.tv_title);
        tvArtist = view.findViewById(R.id.tv_artist);
        tvPosition = view.findViewById(R.id.tv_position);
        tvDuration = view.findViewById(R.id.tv_duration);
        imvPlay = view.findViewById(R.id.imv_play);
        imvPrevious = view.findViewById(R.id.imv_previous);
        imvNext = view.findViewById(R.id.imv_next);
        imvCancel = view.findViewById(R.id.imv_cancel);
        imvRepeat = view.findViewById(R.id.imv_repeat);
        imvRepeat.setBackgroundResource(isRepeat ? R.drawable.ic_repeat : R.drawable.ic_unrepeat);
        seekBar = view.findViewById(R.id.seekBar);

        rotateAnimator.pause();
        rotateAnimator = ObjectAnimator.ofFloat(imvImagePlaying, "rotation", 0f, 360f);
        rotateAnimator.setDuration(10000); // thời gian quay là 10 giây
        rotateAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        rotateAnimator.setInterpolator(new LinearInterpolator()); // Sử dụng LinearInterpolator để quay đều
        restoreRotation();

        updatePlayingSongInfo(currentSong);
        updatePlayButton(isPlaying);
        updatePreNextButton(!isExpanded);
        updateSeekBar();
        presenter.setIsCollapsed(!isExpanded);
        setOnclick();
        viewSwitcher.setDisplayedChild(i);
        setLayoutPlayingHeight(i);
        listView.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
        imvCancel.setOnClickListener(view1 -> {
            presenter.onCancelClicked();
            presenter.setIsCollapsed(true);
            listView.setVisibility(View.VISIBLE);
            setLayoutPlayingHeight(0);
        });
    }

    private void setLayoutPlayingHeight(int i){
        ViewGroup.LayoutParams layoutParams = viewSwitcher.getLayoutParams();
        layoutParams.height = i == 1 ? ViewGroup.LayoutParams.MATCH_PARENT :
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 85, getResources().getDisplayMetrics());
        viewSwitcher.setLayoutParams(layoutParams);
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
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Background Sound Channel", NotificationManager.IMPORTANCE_LOW);
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