package com.example.bottomnavigationapp.mainActivity;

public interface MainContract {
    interface View {
        void showFragment(androidx.fragment.app.Fragment fragment);
//        void showPlayingFragment();
//        void hidePlayingFragment();
    }

    interface Presenter {
        void onNavigationItemSelected(int itemId);
        void initializeFragments();
    }
}
