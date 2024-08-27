package com.example.bottomnavigationapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    // Khai báo các Fragment để sử dụng trong Activity
    private Fragment homeFragment;
    private Fragment notificationFragment;
    private Fragment accountFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        init();

        setOnMenuClick();
    }

    private void init() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Khởi tạo các Fragment
        homeFragment = new HomeFragment();
        notificationFragment = new NotificationFragment();
        accountFragment = new AccountFragment();

        // Thêm các Fragment vào FragmentTransaction và hiển thị HomeFragment mặc định
        getSupportFragmentManager().beginTransaction()
                .add(R.id.frame_container, homeFragment)
                .commit();
    }

    private void setOnMenuClick() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                showFragment(homeFragment);
                return true;
            }

            if (itemId == R.id.navigation_notification) {
                if (!notificationFragment.isAdded()) {
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.frame_container, notificationFragment)
                            .commit();
                }
                showFragment(notificationFragment);
                return true;
            }

            if (itemId == R.id.navigation_account) {
                if (!accountFragment.isAdded()) {
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.frame_container, accountFragment)
                            .commit();
                }
                showFragment(accountFragment);
                return true;
            }
            return false;
        });
    }

    private void showFragment(Fragment fragmentToShow) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Hide all fragments
        if (homeFragment.isAdded()) transaction.hide(homeFragment);
        if (notificationFragment.isAdded()) transaction.hide(notificationFragment);
        if (accountFragment.isAdded()) transaction.hide(accountFragment);

        // Show the selected fragment
        transaction.show(fragmentToShow);

        transaction.commit();
    }
}
