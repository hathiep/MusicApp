package com.example.bottomnavigationapp.screen.editProfileFragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.example.bottomnavigationapp.R;
import com.example.bottomnavigationapp.model.User;
import com.example.bottomnavigationapp.model.Validate;
import com.example.bottomnavigationapp.screen.changePassword.ChangePasswordActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class EditProfileFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int STORAGE_PERMISSION_CODE = 123;

    private TextInputLayout layoutFullName, layoutPhone;
    private TextInputEditText edtEmail, edtFullName, edtPhone;
    private TextView btnUpdate, btnChangePassword;
    private ImageView imVBack, imvAvatar, imvCamera;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;
    private Uri imageUri;
    private Validate validate;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                fragmentManager.popBackStack();
            }
        });

        // Gọi hàm ánh xạ view
        init(view);

        // Gọi hàm hiển thị thông tin user hiện tại
        getCurrentUser();

        setOnclick();

        return view;
    }

    // Hàm ánh xạ view
    private void init(View view){
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        layoutFullName = view.findViewById(R.id.layout_fullName);
        layoutPhone = view.findViewById(R.id.layout_phone);
        edtEmail = view.findViewById(R.id.edt_email);
        edtFullName = view.findViewById(R.id.edt_fullName);
        edtPhone = view.findViewById(R.id.edt_phone);
        btnUpdate = view.findViewById(R.id.btn_update);
        btnChangePassword = view.findViewById(R.id.btn_change_password);
        imVBack = view.findViewById(R.id.imV_back);
        imvAvatar = view.findViewById(R.id.imv_avatar);
        imvCamera = view.findViewById(R.id.imv_camera);
        validate = new Validate(getContext());
        validate.validateFullName(layoutFullName, edtFullName);
        validate.validatePhone(layoutPhone, edtPhone);
    }

    // Hàm hiển thị thông tin user hiện tại
    private void getCurrentUser() {
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            edtEmail.setText(userEmail);

            db.collection("users").whereEqualTo("email", userEmail)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            DocumentSnapshot userDoc = task.getResult().getDocuments().get(0);
                            User user = userDoc.toObject(User.class);
                            if (user != null) {
                                edtFullName.setText(user.getFullName());
                                edtPhone.setText(user.getPhone());
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
                            }
                        }
                    });
        }
    }

    // Hàm bắt sự kiện các button
    private void setOnclick(){
        imVBack.setOnClickListener(v -> goBackToAccountFragment());

        imvCamera.setOnClickListener(v -> {
            openFileChooser();
        });

        btnUpdate.setOnClickListener(v -> {
            String fullName = edtFullName.getText().toString();
            String phone = edtPhone.getText().toString();
            Validate validate = new Validate(getContext());
            if(!validate.checkValidateEmpty(fullName, phone)) return;
            if(!validate.checkValidatePhone(phone)) return;
            showConfirmDialog();
        });

        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null) {
            imageUri = data.getData();  // Lấy URI của ảnh đã chọn
            if (imageUri != null) {
                imvAvatar.setImageURI(imageUri);  // Hiển thị ảnh vào imvAvatar
            } else {
                Log.e("EditProfileFragment", "Failed to select image");
            }
        }
    }

    // Hàm trở về fragment trước đó
    private void goBackToAccountFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        fragmentManager.popBackStack();
    }

    // Hàm hiển thị thông báo và cập nhật thông tin user
    private void updateUserInformation() {
        String fullName = edtFullName.getText().toString();
        String phone = edtPhone.getText().toString();
        showProgressDialog(getString(R.string.updating));

        if (currentUser != null) {
            String userEmail = currentUser.getEmail();

            // Nếu người dùng đã chọn ảnh (imageUri không null), tải ảnh lên trước
            if (imageUri != null) {
                uploadImageToStorage(imageUri, (avatarUrl) -> {
                    // Sau khi ảnh được tải lên thành công và URL được lấy, tiếp tục cập nhật thông tin người dùng
                    updateUserFirestore(fullName, phone, avatarUrl);
                });
            } else {
                // Nếu không có ảnh để tải lên, chỉ cập nhật thông tin cá nhân mà không có URL avatar
                updateUserFirestore(fullName, phone, null);
            }
        }
    }

    // Hàm tải ảnh lên Storage và trả về URL
    private void uploadImageToStorage(Uri imageUri, OnImageUploadSuccessListener listener) {
        String uid = currentUser.getUid();  // Lấy UID của người dùng hiện tại
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("users/" + uid + "/avatar.jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Sau khi tải lên thành công, lấy URL tải xuống
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String avatarUrl = uri.toString();
                        listener.onSuccess(avatarUrl);  // Gọi listener khi có URL
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Hàm cập nhật thông tin người dùng vào Firestore (bao gồm URL avatar nếu có)
    private void updateUserFirestore(String fullName, String phone, @Nullable String avatarUrl) {
        String userEmail = currentUser.getEmail();

        db.collection("users").whereEqualTo("email", userEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot userDoc = task.getResult().getDocuments().get(0);
                        if (avatarUrl != null) {
                            // Cập nhật cả thông tin cá nhân và URL avatar
                            userDoc.getReference().update("fullName", fullName, "phone", phone, "avatarUrl", avatarUrl)
                                    .addOnSuccessListener(aVoid -> {
                                        showUpdateSuccessMessage();
                                    })
                                    .addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(getContext(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            // Cập nhật chỉ thông tin cá nhân
                            userDoc.getReference().update("fullName", fullName, "phone")
                                    .addOnSuccessListener(aVoid -> {
                                        showUpdateSuccessMessage();
                                    })
                                    .addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(getContext(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                });
    }

    // Hàm hiển thị thông báo thành công sau khi cập nhật
    private void showUpdateSuccessMessage() {
        progressDialog.setMessage(getString(R.string.update_success));
        new Handler().postDelayed(() -> {
            progressDialog.dismiss();
        }, 1000);
    }

    // Listener để xử lý sự kiện khi tải ảnh thành công và nhận URL
    interface OnImageUploadSuccessListener {
        void onSuccess(String avatarUrl);
    }

    // Hàm thông báo xác nhận
    private void showConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setMessage(R.string.update_confirm_message)
                .setPositiveButton(R.string.yes, null)
                .setNegativeButton(R.string.no, null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.red));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(true);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(true);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                updateUserInformation();
                dialog.dismiss();
            });
        });

        dialog.show();
    }
    private void showProgressDialog(String message) {
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }
}
