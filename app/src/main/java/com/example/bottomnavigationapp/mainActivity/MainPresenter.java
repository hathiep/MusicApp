package com.example.bottomnavigationapp.mainActivity;

import androidx.fragment.app.Fragment;

import com.example.bottomnavigationapp.accountFragment.AccountFragment;
import com.example.bottomnavigationapp.homeFragment.HomeFragment;
import com.example.bottomnavigationapp.notificationFragment.NotificationFragment;
import com.example.bottomnavigationapp.R;

public class MainPresenter implements MainContract.Presenter {
    private MainContract.View view;
    private Fragment homeFragment;
    private Fragment notificationFragment;
    private Fragment accountFragment;

    public MainPresenter(MainContract.View view) {
        this.view = view;
        initializeFragments();
    }

    @Override
    public void initializeFragments() {
        homeFragment = new HomeFragment();
        notificationFragment = new NotificationFragment();
        accountFragment = new AccountFragment();
        view.showFragment(homeFragment); // Show HomeFragment by default
    }

    @Override
    public void onNavigationItemSelected(int itemId) {
        Fragment fragmentToShow = null;

        if (itemId == R.id.navigation_home) {
            fragmentToShow = homeFragment;
        } else if (itemId == R.id.navigation_notification) {
            fragmentToShow = notificationFragment;
        } else if (itemId == R.id.navigation_account) {
            fragmentToShow = accountFragment;
        }

        if (fragmentToShow != null) {
            view.showFragment(fragmentToShow);
        }
    }

    public MainContract.View getView() {
        return view;
    }

    public void setView(MainContract.View view) {
        this.view = view;
    }

    public Fragment getHomeFragment() {
        return homeFragment;
    }

    public void setHomeFragment(Fragment homeFragment) {
        this.homeFragment = homeFragment;
    }

    public Fragment getNotificationFragment() {
        return notificationFragment;
    }

    public void setNotificationFragment(Fragment notificationFragment) {
        this.notificationFragment = notificationFragment;
    }

    public Fragment getAccountFragment() {
        return accountFragment;
    }

    public void setAccountFragment(Fragment accountFragment) {
        this.accountFragment = accountFragment;
    }
}
