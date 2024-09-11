package com.example.bottomnavigationapp.screen.notificationFragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.bottomnavigationapp.R;
import com.example.bottomnavigationapp.screen.notificationFragment.adapter.AudioAdapter;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment implements NotificationContract.View {
    private SwipeRefreshLayout swipeRefreshLayout;
    private Button btnUpload;
    private static final int PICK_AUDIO_REQUEST = 1;
    private RecyclerView rcvAudios;
    private List<String> audioList;
    private AudioAdapter audioAdapter;
    private Uri audioUri;
    private ProgressBar progressBar;

    private NotificationPresenter presenter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        init(view);
        setOnclick();
        presenter.loadAudioList(); // Load danh sách audio khi bắt đầu

        return view;
    }

    private void init(View view){
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        progressBar = view.findViewById(R.id.progressBar);
        btnUpload = view.findViewById(R.id.btn_upload);
        audioList = new ArrayList<>();
        audioAdapter = new AudioAdapter(audioList);
        rcvAudios = view.findViewById(R.id.rcv_audios);
        rcvAudios.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvAudios.setAdapter(audioAdapter);

        presenter = new NotificationPresenter(this, getContext());
    }

    private void setOnclick(){
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Tải lại danh sách audio
            presenter.loadAudioList();

            // Ẩn vòng xoay sau khi làm mới dữ liệu
            swipeRefreshLayout.setRefreshing(false);
        });

        btnUpload.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            startActivityForResult(intent, PICK_AUDIO_REQUEST);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            audioUri = data.getData();
            presenter.uploadAudio(audioUri);
        }
    }

    @Override
    public void showUploadSuccess(String audioUrl) {
        Toast.makeText(getContext(), "Upload thành công!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showUploadFailure(String errorMessage) {
        Toast.makeText(getContext(), "Upload thất bại: " + errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showAudioList(List<String> audioList) {
        this.audioList.clear();
        this.audioList.addAll(audioList);
        audioAdapter.notifyDataSetChanged();
    }

    @Override
    public void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        progressBar.setVisibility(View.GONE);
    }
}