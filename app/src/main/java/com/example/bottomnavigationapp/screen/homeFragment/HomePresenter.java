package com.example.bottomnavigationapp.screen.homeFragment;

import android.content.Intent;

import com.example.bottomnavigationapp.model.Song;
import com.example.bottomnavigationapp.service.ApiResponse;
import com.example.bottomnavigationapp.service.ApiService;
import com.example.bottomnavigationapp.service.BackgroundSoundService;
import com.example.bottomnavigationapp.service.RetrofitClient;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class HomePresenter implements HomeContract.Presenter {

    private final HomeContract.View view;
    private final List<Song> songList = new ArrayList<>();
    private Song currentSong;
    private boolean isPlaying;

    public HomePresenter(HomeContract.View view) {
        this.view = view;
    }

    @Override
    public void loadSongs() {
        view.showLoadingIndicator(true);

        ApiService apiService = RetrofitClient.getClient("https://itunes.apple.com/").create(ApiService.class);
        Call<ApiResponse> call = apiService.searchSongs("sontungmtp", "music", "musicTrack");

        call.enqueue(new retrofit2.Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, retrofit2.Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    songList.clear();
                    if (response.body() != null) {
                        for (ApiResponse.Result result : response.body().getResults()) {
                            // Tạo đối tượng Song từ dữ liệu trả về
                            Song song = new Song(result.getTrackId(), result.getTrackName(), result.getArtistName(), result.getPreviewUrl());
                            songList.add(song);
                        }
                    }
                    view.showLoadingIndicator(false);
                    view.showSongs(songList);
                } else {
                    view.showLoadingIndicator(false);
                    // Xử lý lỗi ở đây, chẳng hạn như hiển thị thông báo lỗi
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                view.showLoadingIndicator(false);
                // Xử lý lỗi ở đây, chẳng hạn như hiển thị thông báo lỗi
            }
        });
    }



//    private void fetchAudioPaths() {
//        // Assuming the API provides direct URLs for audio files, otherwise you might need to use Firebase Storage as in the original code
//        for (Song song : songList) {
//            // Here you would handle audio paths if necessary
//            // If you have URLs directly from the API, you can skip this step
//        }
//        view.showLoadingIndicator(false);
//        view.showSongs(songList);
//    }


//    @Override
//    public void loadSongs() {
//        view.showLoadingIndicator(true);
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        db.collection("songs").get().addOnCompleteListener(task -> {
//            if (task.isSuccessful()) {
//                songList.clear();
//                for (QueryDocumentSnapshot document : task.getResult()) {
//                    Song song = document.toObject(Song.class);
//                    song.setId(document.getId());
//                    songList.add(song);
//                }
//
//                fetchAudioPaths();
//            } else {
//                view.showLoadingIndicator(false);
//                // Handle errors here, such as showing an error message
//            }
//        });
//    }
//
//    private void fetchAudioPaths() {
//        FirebaseStorage storage = FirebaseStorage.getInstance();
//        StorageReference storageRef = storage.getReference().child("songs");
//
//        for (Song song : songList) {
//            StorageReference songRef = storageRef.child(song.getId() + ".mp3");
//            songRef.getDownloadUrl().addOnSuccessListener(uri -> {
//                song.setAudioPath(uri.toString());
//                view.showLoadingIndicator(false);
//                view.showSongs(songList);
//            }).addOnFailureListener(exception -> {
//                view.showLoadingIndicator(false);
//                // Handle errors here, such as showing an error message
//            });
//        }
//    }

    @Override
    public void onPlayPauseClicked() {
        if (currentSong != null) {
            isPlaying = !isPlaying;
            sendActionToService(isPlaying ? "ACTION_PLAY" : "ACTION_PAUSE");
            view.updatePlayButton(isPlaying);
        }
    }

    @Override
    public void onPreviousClicked() {
        int currentIndex = songList.indexOf(currentSong);
        if (currentIndex > 0) {
            currentSong = songList.get(currentIndex - 1);
            view.updateAdapter(currentIndex - 1);
            view.updateSeekBar();
            startPlayingCurrentSong();
        }
    }

    @Override
    public void onNextClicked() {
        int currentIndex = songList.indexOf(currentSong);
        if (currentIndex < songList.size() - 1) {
            currentSong = songList.get(currentIndex + 1);
            view.updateAdapter(currentIndex + 1);
            view.updateSeekBar();
            startPlayingCurrentSong();
        }
    }

    @Override
    public void onSeekBarStopTrackingTouch(int position) {
        sendActionToService("ACTION_PAUSE");
        sendActionToService("ACTION_SEEK", position);
        sendActionToService("ACTION_PLAY");
    }

    @Override
    public void onSongSelected(Song song) {
        currentSong = song;
        startPlayingCurrentSong();
    }

    private void startPlayingCurrentSong() {
        if (currentSong != null) {
            view.updateSeekBar();
            Intent intent = new Intent(view.getContext(), BackgroundSoundService.class);
            intent.setAction("ACTION_PLAY");
            intent.putExtra("SONG", currentSong);
            intent.putExtra("MEDIA_POSITION", 0); // Truyền vị trí hiện tại
            view.getContext().startService(intent);
            view.updatePlayingSongInfo(currentSong);
            view.updatePlayButton(true);
            view.updatePreNextButton();
            isPlaying = true;
        }
    }

    private void sendActionToService(String action) {
        Intent intent = new Intent(view.getContext(), BackgroundSoundService.class);
        intent.setAction(action);
        intent.putExtra("SONG", currentSong);  // Truyền đối tượng Song
        view.getContext().startService(intent);
    }

    private void sendActionToService(String action, int position) {
        Intent intent = new Intent(view.getContext(), BackgroundSoundService.class);
        intent.setAction(action);
        intent.putExtra("SONG", currentSong);  // Truyền đối tượng Song
        intent.putExtra("MEDIA_POSITION", position);
        view.getContext().startService(intent);
    }

    public Song getCurrentSong() {
        return currentSong;
    }
}
