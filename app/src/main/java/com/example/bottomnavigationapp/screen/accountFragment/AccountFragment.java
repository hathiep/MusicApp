package com.example.bottomnavigationapp.screen.accountFragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.bottomnavigationapp.R;
import com.example.bottomnavigationapp.model.User;
import com.example.bottomnavigationapp.screen.editProfileFragment.EditProfileFragment;
import com.example.bottomnavigationapp.screen.login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


public class AccountFragment extends Fragment {
    private ImageView imvInformation, imvAvatar, imvEdit;
    private TextView tvFullName, btnLogout;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // Gọi hàm ánh xạ view
        init(view);

        // Gọi hàm lấy user hiện tại
        getCurrentUser();

        return view;
    }

    // Hàm ánh xạ view
    private void init(View view){
//        imvInformation = view.findViewById(R.id.imv_information);
        imvAvatar = view.findViewById(R.id.imv_avatar);
        imvEdit = view.findViewById(R.id.imv_edit);
        tvFullName = view.findViewById(R.id.tv_fullName);
        btnLogout = view.findViewById(R.id.btn_logout);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        setOnclick(imvEdit, new EditProfileFragment());

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    // Hàm bắt sự kiện click chuyển hướng các Fragment
    private void setOnclick(ImageView imv, Fragment fragment){
        imv.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(fragment.getClass().getSimpleName())
                    .commit();
        });
    }

    // Hàm lấy user hiện tại
    private void getCurrentUser() {
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            db.collection("users").whereEqualTo("email", userEmail)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            DocumentSnapshot userDoc = task.getResult().getDocuments().get(0);
                            User user = userDoc.toObject(User.class);

                            // Lấy URL ảnh avatar từ Firestore và hiển thị vào ImageView
                            String avatarUrl = user.getAvatarUrl();
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                // Sử dụng Glide để tải và hiển thị ảnh
                                Glide.with(getContext())
                                        .load(avatarUrl)
                                        .placeholder(R.drawable.avatar_alt)  // Ảnh placeholder khi tải ảnh
                                        .error(R.drawable.avatar_alt)        // Ảnh mặc định khi lỗi
                                        .into(imvAvatar);
                            }

                            tvFullName.setText(user.getFullName());
                        }
                    });
        }
    }


    // Hàm thông báo xác nhận đăng xuất
    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Bạn có muốn đăng xuất?");
        builder.setMessage("Xác nhận đăng xuất khỏi ứng dụng?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            auth.signOut();
            dialog.dismiss();
            show_dialog("Đăng xuất thành công!", 1);
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(getContext(), LoginActivity.class);
                startActivity(intent);
            }, 1000);
        });


        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Hiển thị Dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void show_dialog(String s, int time){
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Thông báo");
        progressDialog.setMessage(s);
        progressDialog.show();

        new Handler().postDelayed(progressDialog::dismiss, time * 1000);
    }
}
