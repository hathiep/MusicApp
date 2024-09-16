package com.example.bottomnavigationapp.model;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.regex.Pattern;

public class Validate {
    private Context context;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^\\d{10}$");
    public Validate() {
    }

    public Validate(Context context) {
        this.context = context;
    }
    // Hàm kiểm tra xâu rỗng
    public boolean validateEmpty(String x){
        return TextUtils.isEmpty(x);
    }
    // Hàm thông báo validate xâu rỗng 2 tham số
    public boolean checkValidateEmpty(String a, String b){
        if(validateEmpty(a) || validateEmpty(b)){
            show_dialog("Vui lòng nhập đầy đủ thông tin!", 1);
            return false;
        }
        return true;
    }
    // Hàm thông báo validate xâu rỗng 3 tham số
    public boolean checkValidateEmpty(String a, String b, String c){
        if(validateEmpty(a) || validateEmpty(b)  || validateEmpty(c)){
            show_dialog("Vui lòng nhập đầy đủ thông tin!", 1);
            return false;
        }
        return true;
    }
    // Hàm thông báo validate xâu rỗng 4 tham số
    public boolean checkValidateEmpty(String a, String b, String c, String d){
        if(validateEmpty(a) || validateEmpty(b)  || validateEmpty(c)  || validateEmpty(d)){
            show_dialog("Vui lòng nhập đầy đủ thông tin!", 1);
            return false;
        }
        return true;
    }
    // Hàm thông báo validate xâu rỗng 5 tham số
    public boolean checkValidateEmpty(String a, String b, String c, String d, String e){
        if(validateEmpty(a) || validateEmpty(b)  || validateEmpty(c)  || validateEmpty(d)  || validateEmpty(e) ){
            show_dialog("Vui lòng nhập đầy đủ thông tin!", 1);
            return false;
        }
        return true;
    }
    // Hàm thông báo validate xâu rỗng n tham số qua mảng
    public boolean checkValidateEmpty(String[] list){
        for(int i=0; i<list.length; i++){
            if(validateEmpty(list[i])){
                show_dialog("Vui lòng nhập đầy đủ thông tin!", 1);
                return false;
            }
        }
        return true;
    }
    // Hàm thông báo validate xâu rỗng n tham số qua list
    public boolean checkValidateEmpty(List<String> list){
        for(int i=0; i<list.size(); i++){
            if(validateEmpty(list.get(i))){
                show_dialog("Vui lòng nhập đầy đủ thông tin!", 1);
                return false;
            }
        }
        return true;
    }
    // Hàm validate email đúng định dạng
    public boolean validateEmail(String email) {
        // Kiểm tra định dạng chính xác của email
        return EMAIL_PATTERN.matcher(email).matches();
    }
    // Hàm thông báo validate email
    public boolean checkValidateEmail(String email){
        if(!validateEmail(email)){
            show_dialog("Email không đúng. Vui lòng nhập lại!", 2);
            return false;
        }
        return true;
    }
    // Hàm validate số điện thoại đúng định dạng
    public boolean validatePhone(String phone){
        String sanitizedPhoneNumber = phone.replaceAll("\\s|-", "");

        // Kiểm tra độ dài phù hợp của số điện thoại
        if (sanitizedPhoneNumber.length() != 10) {
            return false;
        }

        // Kiểm tra định dạng chính xác của số điện thoại
        return PHONE_NUMBER_PATTERN.matcher(sanitizedPhoneNumber).matches();
    }
    // Hàm thông báo validate phone
    public boolean checkValidatePhone(String phone){
        if(!validatePhone(phone)){
            show_dialog("Số điện thoại không đúng. Vui lòng nhập lại!", 2);
            return false;
        }
        return true;
    }
    // Hàm validate mật khẩu đúng định dạng độ dài từ 8 đến 20, bao gồm cả chữ hoa, chữ thường và số
    public boolean validatePassword(String password) {
        // Kiểm tra độ dài phù hợp của mật khẩu
        if (password.length() < 8 || password.length() > 20) {
            return false;
        }

        // Kiểm tra sự tồn tại của ít nhất một chữ cái viết thường, một chữ cái viết hoa và số
        boolean hasLowerCase = false;
        boolean hasUpperCase = false;
        boolean hasDigit = false;

        for (char ch : password.toCharArray()) {
            if (Character.isLowerCase(ch)) {
                hasLowerCase = true;
            } else if (Character.isUpperCase(ch)) {
                hasUpperCase = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            }
        }

        return hasLowerCase && hasUpperCase && hasDigit;
    }
    // Hàm thông báo validate password
    public boolean checkValidatePassword(String password){
        if(!validatePassword(password)){
            show_dialog("Mật khẩu dài từ 8 đến 20 kí tự, bao gồm cả chữ hoa, chữ thường và số. Vui lòng nhập lại!", 3);
            return false;
        }
        return true;
    }
    // Hàm validate mật khẩu trùng khớp
    public boolean validatePasswordEqual(String password, String passwordequal){
        return password.equals(passwordequal);
    }
    // Hàm thông báo validate phone
    public boolean checkValidatePasswordEqual(String password, String passwordequal){
        if(!validatePasswordEqual(password, passwordequal)){
            show_dialog("Mật khẩu không trùng khớp. Vui lòng nhập lại!", 2);
            return false;
        }
        return true;
    }

    public void validateEmail(TextInputLayout layoutEdt, TextInputEditText edt){
        edt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String email = edt.getText().toString().trim();
                layoutEdt.setErrorEnabled(true);
                if (email.isEmpty()) {
                    layoutEdt.setError("Email không được để trống");
                } else if (!validateEmail(email)) {
                    layoutEdt.setError("Email không hợp lệ!");
                } else {
                    layoutEdt.setErrorEnabled(false);  // Tắt chế độ hiển thị lỗi để không có khoảng trống
                }
            }
        });
    }

    public void validatePassword(TextInputLayout layoutEdt, TextInputEditText edt, ImageView imv){
        edt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String password = edt.getText().toString().trim();
                layoutEdt.setErrorEnabled(true);
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) imv.getLayoutParams();
                int marginInPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, context.getResources().getDisplayMetrics());
                if (password.isEmpty()) {
                    layoutEdt.setError("Mật khẩu không được để trống");
                } else if (!validatePassword(password)) {
                    layoutEdt.setError("Mật khẩu dài 8 - 20 kí tự, gồm cả chữ hoa, chữ thường và số");
                } else {
                    layoutEdt.setErrorEnabled(false);  // Tắt chế độ hiển thị lỗi để không có khoảng trống
                    marginInPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, context.getResources().getDisplayMetrics());
                }
                params.setMargins(params.leftMargin, params.topMargin, marginInPx, params.bottomMargin);
                imv.setLayoutParams(params);
            }
        });
    }

    public void validatePassword(TextInputLayout layoutEdt, TextInputEditText edt1, TextInputEditText edt2, ImageView imv){
        edt1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String password = edt1.getText().toString().trim();
                layoutEdt.setErrorEnabled(true);
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) imv.getLayoutParams();
                int marginInPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, context.getResources().getDisplayMetrics());
                if (password.isEmpty()) {
                    layoutEdt.setError("Mật khẩu không được để trống");
                } else if (!validatePassword(password)) {
                    layoutEdt.setError("Mật khẩu dài 8 - 20 kí tự, gồm cả chữ hoa, chữ thường và số");
                } else if (!password.equals(edt2.getText().toString().trim())) {
                    layoutEdt.setError("Mật khẩu không trùng khớp");
                } else {
                    layoutEdt.setErrorEnabled(false);  // Tắt chế độ hiển thị lỗi để không có khoảng trống
                    marginInPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, context.getResources().getDisplayMetrics());
                }
                params.setMargins(params.leftMargin, params.topMargin, marginInPx, params.bottomMargin);
                imv.setLayoutParams(params);
            }
        });
    }

    public void validateFullName(TextInputLayout layoutEdt, TextInputEditText edt){
        edt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String fullName = edt.getText().toString().trim();
                layoutEdt.setErrorEnabled(true);
                if (fullName.isEmpty()) {
                    layoutEdt.setError("Họ và tên không được để trống");
                } else {
                    layoutEdt.setErrorEnabled(false);  // Tắt chế độ hiển thị lỗi để không có khoảng trống
                }
            }
        });
    }

    public void validatePhone(TextInputLayout layoutEdt, TextInputEditText edt){
        edt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String phone = edt.getText().toString().trim();

                layoutEdt.setErrorEnabled(true);
                if (phone.isEmpty()) {
                    layoutEdt.setError("Số điện thoại không được để trống");
                } else if (!validatePhone(phone)) {
                    layoutEdt.setError("Số điện thoại không đúng");
                } else {
                    layoutEdt.setErrorEnabled(false);  // Tắt chế độ hiển thị lỗi để không có khoảng trống
                }
            }
        });
    }

    public boolean validateLogin(String email, String password){
        if(!checkValidateEmpty(email, password) || !checkValidateEmail(email) || !checkValidatePassword(password)) return false;
        return true;
    }
    public boolean validateRegister(String name, String email, String phone, String password, String passwordagain){
        if(!checkValidateEmpty(name, email, phone, password, passwordagain)
                || !checkValidateEmail(email) || !checkValidatePhone(phone)
                || !checkValidatePassword(password) || !checkValidatePasswordEqual(password, passwordagain)) return false;
        return true;
    }
    public boolean validateProfile(String name, String phone){
        if(!checkValidateEmpty(name, phone) || !checkValidatePhone(phone)) return false;
        return true;
    }
    public boolean validateChangePassword(String old_password, String new_password, String new_password_again){
        if(!checkValidateEmpty(old_password, new_password, new_password_again) || !checkValidatePassword(new_password)
        || !checkValidatePasswordEqual(new_password, new_password_again)) return false;
        return true;
    }
    private void show_dialog(String s, int time){
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Thông báo");
        progressDialog.setMessage(s);
        progressDialog.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressDialog.dismiss();
            }
        }, time * 1000);
    }
}
