package com.example.bottomnavigationapp.screen.forgotPassword;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

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
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ImageView imV_back;
    private TextInputEditText editTextEmail;
    private ProgressDialog progressDialog;
    private Button btnGetPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Gọi hàm ánh xạ view
        initUi();
        // Gọi hàm onclick
        onClickListener();
    }

    // Hàm ánh xạ view
    private void initUi(){
        imV_back = findViewById(R.id.imV_back);
        editTextEmail = findViewById(R.id.edt_email);
        btnGetPassword = findViewById(R.id.btn_get_password);
        progressDialog = new ProgressDialog(this);
    }

    // Hàm bắt sự kiện click button
    private void onClickListener(){
        imV_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnGetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = editTextEmail.getText().toString().trim();
                if(email == null || email == ""){
                    Toast.makeText(ForgotPasswordActivity.this, "Vui lòng nhập email!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Validate validate = new Validate(ForgotPasswordActivity.this);
                if(!validate.checkValidateEmail(email)) return;
                onClickGetPassword(email);
            }
        });
    }

    // Hiển thị thông báo và gửi email xác thực
    private void onClickGetPassword(String email){
        progressDialog.setTitle("Loading...");
        progressDialog.setMessage("Đang gửi email xác nhận!");
        progressDialog.show();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPasswordActivity.this, "Đã gửi email thành công. Vui lòng truy cập email để đổi mật khẩu!",
                                    Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else{
                            Toast.makeText(ForgotPasswordActivity.this, "Đã có lỗi xảy ra. Vui lòng thử lại!",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}