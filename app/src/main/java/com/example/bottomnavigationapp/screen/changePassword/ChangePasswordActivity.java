package com.example.bottomnavigationapp.screen.changePassword;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bottomnavigationapp.R;
import com.example.bottomnavigationapp.model.Validate;
import com.example.bottomnavigationapp.screen.login.LoginActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {
    private TextInputLayout layoutOldPassword, layoutNewPassword, layoutNewPasswordAgain;
    private TextInputEditText edtOldPassword, edtNewPassword, edtNewPasswordAgain;
    private Button btnChangePassword;
    private ImageView imvBack, imvEye1, imvEye2, imvEye3;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private Integer eye1, eye2, eye3;
    private Validate validate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initUi();

        setOnClickListener();
    }

    // Hàm ánh xạ view
    private void initUi(){
        layoutOldPassword = findViewById(R.id.layout_pw_old);
        layoutNewPassword = findViewById(R.id.layout_pw_new);
        layoutNewPasswordAgain = findViewById(R.id.layout_pw_new_ag);
        edtOldPassword = findViewById(R.id.edt_old_password);
        edtNewPassword = findViewById(R.id.edt_new_password);
        edtNewPasswordAgain = findViewById(R.id.edt_new_password_again);
        btnChangePassword = findViewById(R.id.btn_change_password);
        imvBack = findViewById(R.id.imV_back);
        imvEye1 = findViewById(R.id.imV_eye1);
        imvEye2 = findViewById(R.id.imV_eye2);
        imvEye3 = findViewById(R.id.imV_eye3);
        validate = new Validate(this);
        validate.validatePassword(layoutOldPassword, edtOldPassword, imvEye1);
        validate.validatePassword(layoutNewPassword, edtNewPassword, imvEye2);
        validate.validatePassword(layoutNewPasswordAgain, edtNewPasswordAgain, edtNewPassword, imvEye3);
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        eye1 = eye2 = eye3 = 0;
        setUiEye(imvEye1, edtOldPassword, 1);
        setUiEye(imvEye2, edtNewPassword, 2);
        setUiEye(imvEye3, edtNewPasswordAgain, 3);
    }

    // Hàm hiển thị trạng thái mắt
    private void setUiEye(ImageView imv_eye, EditText edt, int x){
        imv_eye.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int eye = 0;
                if(x==1) eye = eye1;
                if(x==2) eye = eye2;
                if(x==3) eye = eye3;
                if(eye == 0){
                    // Chuyển icon unhide thành hide
                    imv_eye.setImageResource(R.drawable.ic_hide);
                    // Chuyển text từ hide thành unhide
                    edt.setInputType(InputType.TYPE_CLASS_TEXT);
                    // Đặt con trỏ nháy ở cuối input đã nhập
                    edt.setSelection(edt.getText().length());
                    // Đảo lại trạng thái mắt
                    if(x==1) eye1 = 1;
                    else if(x==2) eye2 = 1;
                    else eye3 = 1;
                }
                else {
                    // Chuyển icon hide thành unhide
                    imv_eye.setImageResource(R.drawable.ic_unhide);
                    // Chuyển text từ unhide thành hide
                    int inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
                    edt.setInputType(inputType);
                    // Đặt con trỏ nháy ở cuối input đã nhập
                    edt.setSelection(edt.getText().length());
                    // Đảo lại trạng thái mắt
                    if(x==1) eye1 = 0;
                    else if(x==2) eye2 = 0;
                    else eye3 = 0;
                }
            }
        });
    }

    // Hàm bắt sự kiện các button
    private void setOnClickListener(){
        imvBack.setOnClickListener(view -> {
            finish();
            overridePendingTransition(0, 0);
        });

        btnChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Gọi đối tượng validate
                Validate validate = new Validate(ChangePasswordActivity.this);
                if(!validate.validateChangePassword(getInput(edtOldPassword),
                        getInput(edtNewPassword), getInput(edtNewPasswordAgain))) return;
                // Check đổi mật khẩu
                reAuthenticateUser();
            }
        });
    }
    private String getInput(EditText edt){
        return edt.getText().toString().trim();
    }

    // Hàm check đổi mật khẩu
    private void reAuthenticateUser(){
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), getInput(edtOldPassword));
        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            if(getInput(edtOldPassword).equals(getInput(edtNewPassword))){
                                show_dialog("Vui lòng nhập mật khẩu mới khác mật khẩu cũ!", 2);
                                return;
                            }
                            onClickChangePassword();
                        }
                        else{
                            show_dialog("Mật khẩu cũ không đúng. Vui lòng nhập lại!", 2);
                        }
                    }
                });
    }

    // Hàm đổi mật khẩu trên Authentication
    private void onClickChangePassword(){
        user.updatePassword(getInput(edtNewPassword))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            show_dialog("Đổi mật khẩu thành công!", 2);
                            //Back to login
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    auth.signOut();
                                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                                    startActivity(intent);
                                    overridePendingTransition(0, 0);
                                    finish();
                                }
                            }, 2000);
                        }
                    }
                });
    }

    // Hàm thông báo dialog
    private void show_dialog(String s, int time){
        ProgressDialog progressDialog = new ProgressDialog(ChangePasswordActivity.this);
        progressDialog.setTitle("Thông báo");
        progressDialog.setMessage(s);
        progressDialog.show();

        // Sử dụng Handler để gửi một tin nhắn hoạt động sau một khoảng thời gian
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Ẩn Dialog sau khi đã qua một khoảng thời gian nhất định
                progressDialog.dismiss();
            }
        }, time * 1000); // Số milliseconds bạn muốn Dialog biến mất sau đó
    }
    @Override
    public void onBackPressed() {
        overridePendingTransition(0, 0);
        finish();
    }
}