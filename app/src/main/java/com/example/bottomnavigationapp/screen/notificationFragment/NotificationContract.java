package com.example.bottomnavigationapp.screen.notificationFragment;

import android.net.Uri;

import java.util.List;

public interface NotificationContract {

    interface View {
        void showUploadSuccess(String audioUrl);
        void showUploadFailure(String errorMessage);
        void showAudioList(List<String> audioList);
        void showProgress();
        void hideProgress();
    }

    interface Presenter {
        void uploadAudio(Uri audioUri);
        void loadAudioList();
    }
}
