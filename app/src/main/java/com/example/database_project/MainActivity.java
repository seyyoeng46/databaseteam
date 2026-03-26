package com.example.database_project;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // 시작 탭: 홈
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            loadFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();

            if (id == R.id.nav_routine) {
                fragment = new RoutineFragment();
            } else if (id == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.nav_diary) {
                fragment = new DiaryFragment();
            } else if (id == R.id.nav_mypage) {
                fragment = new MypageFragment();
            } else {
                return false;
            }

            loadFragment(fragment);
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}