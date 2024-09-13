package com.example.bottomnavigationapp.mainActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewSwitcher;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.bottomnavigationapp.screen.homeFragment.HomeFragment;
import com.example.bottomnavigationapp.service.BackgroundSoundService;
import com.example.bottomnavigationapp.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements MainContract.View{

    private BottomNavigationView bottomNavigationView;
    private MainContract.Presenter presenter;
    private ViewSwitcher viewSwitcher;

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

        // Xử lý Intent nếu có khi Activity khởi tạo
        if (getIntent() != null) {
            handleIntent(getIntent());
        }

    }

    private void init(){
        presenter = new MainPresenter(this);
        viewSwitcher = this.findViewById(R.id.view_switcher);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setOnMenuClick() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            presenter.onNavigationItemSelected(item.getItemId());
            return true;
        });
    }

    @Override
    public void showFragment(Fragment fragmentToShow) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Hide all fragments
        if (presenter instanceof MainPresenter) {
            MainPresenter mainPresenter = (MainPresenter) presenter;
            if (mainPresenter.getHomeFragment().isAdded())
                transaction.hide(mainPresenter.getHomeFragment());
            if (mainPresenter.getNotificationFragment().isAdded())
                transaction.hide(mainPresenter.getNotificationFragment());
            if (mainPresenter.getAccountFragment().isAdded())
                transaction.hide(mainPresenter.getAccountFragment());
        }

        // Add the fragment if it hasn't been added yet
        if (!fragmentToShow.isAdded()) {
            transaction.add(R.id.frame_container, fragmentToShow);
        }

        // Show the selected fragment
        transaction.show(fragmentToShow);
        transaction.commit();
    }

    @Override
    public void setLayoutPlayingCollapsed() {
        if (presenter instanceof MainPresenter) {
            MainPresenter mainPresenter = (MainPresenter) presenter;
            HomeFragment homeFragment = (HomeFragment) mainPresenter.getHomeFragment();

            // Gọi hàm public trong HomeFragment
            if (homeFragment != null && homeFragment.isAdded() && viewSwitcher.getVisibility() == View.VISIBLE) {
                homeFragment.updatePlayingLayout(0, viewSwitcher.getChildAt(0));
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("NAVIGATE_TO_FRAGMENT")) {
            String fragmentToNavigate = intent.getStringExtra("NAVIGATE_TO_FRAGMENT");
            if ("FragmentHome".equals(fragmentToNavigate)) {
                // Gọi phương thức để hiện thị FragmentHome
                bottomNavigationView.setSelectedItemId(R.id.navigation_home);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dừng dịch vụ phát nhạc
        Intent serviceIntent = new Intent(this, BackgroundSoundService.class);
        stopService(serviceIntent);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage(R.string.cancel_app)
                .setPositiveButton(R.string.yes, (dialog, which) -> finish())
                .setNegativeButton(R.string.no, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.red));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(true);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(true);
        });
        dialog.show();
    }
}
