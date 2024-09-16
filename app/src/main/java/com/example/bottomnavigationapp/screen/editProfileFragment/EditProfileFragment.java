package com.example.bottomnavigationapp.screen.editProfileFragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.bottomnavigationapp.R;
import com.example.bottomnavigationapp.model.User;
import com.example.bottomnavigationapp.model.Validate;
import com.example.bottomnavigationapp.screen.changePassword.ChangePasswordActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditProfileFragment extends Fragment {

    private TextInputEditText edtEmail, edtFullName, edtPhone;
    private TextView btnUpdate, btnChangePassword;
    private ImageView imVBack;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

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

        edtEmail = view.findViewById(R.id.edt_email);
        edtFullName = view.findViewById(R.id.edt_fullName);
        edtPhone = view.findViewById(R.id.edt_phone);
        btnUpdate = view.findViewById(R.id.btn_update);
        btnChangePassword = view.findViewById(R.id.btn_change_password);
        imVBack = view.findViewById(R.id.imV_back);
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
                            }
                        }
                    });
        }
    }

    // Hàm bắt sự kiện các button
    private void setOnclick(){
        imVBack.setOnClickListener(v -> goBackToAccountFragment());

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

            db.collection("users").whereEqualTo("email", userEmail)
                    .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                DocumentSnapshot userDoc = task.getResult().getDocuments().get(0);
                                userDoc.getReference().update("fullName", fullName, "phone", phone)
                                        .addOnSuccessListener(aVoid -> {
                                            progressDialog.setMessage(getString(R.string.update_success));
                                            new Handler().postDelayed(() -> {
                                                progressDialog.dismiss();

                                            }, 1000);
                                        })
                                        .addOnFailureListener(e -> {
                                            // Handle failure
                                        });
                            }
                        });
        }
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
