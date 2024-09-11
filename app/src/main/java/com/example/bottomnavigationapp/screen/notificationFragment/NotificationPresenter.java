package com.example.bottomnavigationapp.screen.notificationFragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class NotificationPresenter implements NotificationContract.Presenter {

    private NotificationContract.View view;
    private StorageReference storageReference;
    private List<String> audioList;
    private Context context;

    public NotificationPresenter(NotificationContract.View view, Context context) {
        this.view = view;
        this.context = context;
        this.storageReference = FirebaseStorage.getInstance().getReference().child("audios");
        this.audioList = new ArrayList<>();
    }

    @Override
    public void uploadAudio(Uri audioUri) {
        if (audioUri != null) {
            view.showProgress();

            // Lấy tên file từ URI
//          String fileName = "A" + String.format("%06d", audioList.size()) + "." + getFileName(audioUri); //Đặt mã là thứ tự tải lên
            String fileName = "A" + System.currentTimeMillis() + "." + getFileName(audioUri); // Đặt mã là thời gian tải lên

            // Tham chiếu đến file với tên gốc
            StorageReference audioRef = storageReference.child(fileName);

            audioRef.putFile(audioUri).addOnSuccessListener(taskSnapshot -> {
                // Sau khi tải lên thành công, thêm tên file vào danh sách
                audioList.add(fileName);
                view.showUploadSuccess(fileName);

                // Cập nhật danh sách trên giao diện
                view.showAudioList(audioList);
                view.hideProgress();
            }).addOnFailureListener(e -> {
                view.showUploadFailure(e.getMessage());
                view.hideProgress();
            });
        } else {
            view.showUploadFailure("No file selected");
        }
    }

    // Phương thức để lấy tên file từ URI
    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    public void loadAudioList() {
        view.showProgress();

        // Tải danh sách các file từ Firebase Storage
        storageReference.listAll().addOnSuccessListener(listResult -> {
            audioList.clear(); // Xóa danh sách cũ

            // Lấy tên của từng file
            for (StorageReference fileRef : listResult.getItems()) {
                // Lấy tên file từ đường dẫn và thêm vào danh sách
                String fileName = fileRef.getName();
                audioList.add(fileName);
            }

            // Cập nhật giao diện với danh sách file
            view.showAudioList(audioList);
            view.hideProgress();
        }).addOnFailureListener(e -> {
            view.hideProgress();
            view.showUploadFailure("Failed to load audio files: " + e.getMessage());
        });
    }
}
