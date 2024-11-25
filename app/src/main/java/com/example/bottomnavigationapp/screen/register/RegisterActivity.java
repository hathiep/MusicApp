package com.example.bottomnavigationapp.screen.register;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout layoutEmail, layoutFullName, layoutPhone, layoutPassword, layoutPasswordAg;
    private TextInputEditText edtFullName, edtEmail, edtPhone, edtPassword, edtPasswordAgain;
    private Button btnRegister;
    private ImageView imV_back, imV_eye1, imV_eye2;
    private ProgressDialog progressDialog;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private Integer eye1, eye2;
    private FirebaseFirestore db;
    private Validate validate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initUi();
        setOnClickListener();
    }

    // Ánh xạ view
    private void initUi() {
        layoutEmail = findViewById(R.id.layout_email);
        layoutFullName = findViewById(R.id.layout_fullName);
        layoutPhone = findViewById(R.id.layout_phone);
        layoutPassword = findViewById(R.id.layout_password);
        layoutPasswordAg = findViewById(R.id.layout_password_ag);
        edtEmail = findViewById(R.id.email);
        edtFullName = findViewById(R.id.fullname);
        edtPhone = findViewById(R.id.phone);
        edtPassword = findViewById(R.id.password);
        edtPasswordAgain = findViewById(R.id.password_again);
        btnRegister = findViewById(R.id.btn_register);
        imV_back = findViewById(R.id.imV_back);
        imV_eye1 = findViewById(R.id.imV_eye1);
        imV_eye2 = findViewById(R.id.imV_eye2);
        validate = new Validate(this);
        validate.validateEmail(layoutEmail, edtEmail);
        validate.validateFullName(layoutFullName, edtFullName);
        validate.validatePhone(layoutPhone, edtPhone);
        validate.validatePassword(layoutPassword, edtPassword, imV_eye1);
        validate.validatePassword(layoutPasswordAg, edtPasswordAgain, edtPassword, imV_eye2);
        eye1 = 0;
        eye2 = 0;
        auth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
    }

    // Set onclick cho các button
    private void setOnClickListener() {
        setUiEye(imV_eye1, edtPassword, 1);
        setUiEye(imV_eye2, edtPasswordAgain, 2);
        imV_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
            }
        });
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lấy các giá trị từ input
                String name, email, phone, password, passwordagain;
                name = edtFullName.getText().toString().trim();
                email = edtEmail.getText().toString().trim();
                phone = edtPhone.getText().toString().trim();
                password = edtPassword.getText().toString().trim();
                passwordagain = edtPasswordAgain.getText().toString().trim();
                // Gọi đối tượng validate
                Validate validate = new Validate(RegisterActivity.this);
                if (!validate.validateRegister(name, email, phone, password, passwordagain)) return;
                // Hiển thị ProgressDialog với thông báo "Đang xử lý"
                show_dialog("Đang xử lý...", 0);
                // Check đăng ký
                createUserWithEmailAndPassword(email, password);
            }
        });
    }

    // Set trạng thái mắt cho mật khẩu
    private void setUiEye(ImageView imv_eye, EditText edt, int x) {
        imv_eye.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int eye = 0;
                if (x == 1) eye = eye1;
                if (x == 2) eye = eye2;
                if (eye == 0) {
                    // Chuyển icon unhide thành hide
                    imv_eye.setImageResource(R.drawable.ic_hide);
                    // Chuyển text từ hide thành unhide
                    edt.setInputType(InputType.TYPE_CLASS_TEXT);
                    // Đặt con trỏ nháy ở cuối input đã nhập
                    edt.setSelection(edt.getText().length());
                    // Đảo lại trạng thái mắt
                    if (x == 1) eye1 = 1;
                    else eye2 = 1;
                } else {
                    // Chuyển icon hide thành unhide
                    imv_eye.setImageResource(R.drawable.ic_unhide);
                    // Chuyển text từ unhide thành hide
                    int inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
                    edt.setInputType(inputType);
                    // Đặt con trỏ nháy ở cuối input đã nhập
                    edt.setSelection(edt.getText().length());
                    // Đảo lại trạng thái mắt
                    if (x == 1) eye1 = 0;
                    else eye2 = 0;
                }
            }
        });
    }

    // Kiểm tra email và đăng ký
    private void createUserWithEmailAndPassword(String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Nếu hoàn thành gửi email xác thực
                        if (task.isSuccessful()) {
                            user = auth.getCurrentUser();
                            if (user != null) {
                                sendEmailVerify(email);
                            }

                        } else {
                            // Email đã tồn tại. Hiển thị thông báo
                            show_dialog(getString(R.string.message_email_exist), 2);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    edtEmail.requestFocus(); // Yêu cầu EditText nhận focus
                                    edtEmail.setSelection(edtEmail.getText().length()); // Di chuyển con trỏ nháy đến cuối của EditText
                                }
                            }, 100);
                        }
                    }
                });
    }

    // Hiển thị thông báo gửi email xác thực
    private void sendEmailVerify(String email) {
        user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Email xác thực đã được gửi thành công
                    show_dialog(getString(R.string.message_register_success), 3);
                    // Lưu thông tin user vào Firestore Database
                    insertUserToFirestoreDatabase();
                    //Trở về trang đăng nhập
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }, 3000);
                } else {
                    // Không thể gửi email xác thực
                    show_dialog(getString(R.string.message_wrong_email), 3);
                }
            }
        });
    }

    private void insertUserToFirestoreDatabase() {
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser(); // Lấy người dùng hiện tại từ FirebaseAuth

        if (user != null) {
            String uid = user.getUid(); // Lấy UID của người dùng từ FirebaseAuth

            // Tạo Map chứa thông tin người dùng
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("fullName", edtFullName.getText().toString());
            userMap.put("email", edtEmail.getText().toString());
            userMap.put("phone", edtPhone.getText().toString());
            userMap.put("avatarUrl", "avatar");

            // Lưu user vào Firestore với ID là UID của họ
            db.collection("users")
                    .document(uid) // Sử dụng UID làm ID cho document
                    .set(userMap) // Lưu Map vào Firestore
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "User added successfully with UID: " + uid);
                    })
                    .addOnFailureListener(e -> {
                        Log.d("Firestore", "Error adding user", e);
                    });
        } else {
            Log.d("Firestore", "No current user logged in");
        }
    }


    private void show_dialog(String s, int time) {
        progressDialog.setTitle("Thông báo");
        progressDialog.setMessage(s);
        progressDialog.show();

        // Sử dụng Handler để gửi một tin nhắn hoạt động sau một khoảng thời gian
        if (time != 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Ẩn Dialog sau khi đã qua một khoảng thời gian nhất định
                    progressDialog.dismiss();
                }
            }, time * 1000); // Số milliseconds Dialog biến mất sau đó
        }
    }
}