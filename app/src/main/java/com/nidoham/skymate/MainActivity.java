package com.nidoham.skymate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.nidoham.skymate.databinding.ActivityMainBinding;

import com.nidoham.skymate.fragment.HomeFragment;
import com.nidoham.skymate.fragment.ShortFragment;
import com.nidoham.skymate.fragment.SubscriptionFragment;
import com.nidoham.skymate.fragment.MeFragment;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private ViewPagerAdapter viewPagerAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check for crash recovery BEFORE setting up UI
        if (handleCrashRecovery()) {
            return; // Don't continue with normal setup if handling crash
        }
        
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupViewPager();
        setupBottomNavigation();
        
        // Add test crash button for debugging (remove in production)
        // addTestCrashButton();
    }
    
    private boolean handleCrashRecovery() {
        try {
            Intent intent = getIntent();
            
            // Check if launched after crash
            boolean shouldLaunchDebug = intent.getBooleanExtra("launch_debug", false);
            String crashMessage = intent.getStringExtra("error_message");
            
            if (shouldLaunchDebug && crashMessage != null) {
                Log.d("MainActivity", "Launching DebugActivity for crash recovery");
                // Launch DebugActivity immediately
                Intent debugIntent = new Intent(this, DebugActivity.class);
                debugIntent.putExtra("error_message", crashMessage);
                debugIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(debugIntent);
                finish(); // Close MainActivity
                return true;
            }
            
            // Check for saved crash log from previous session
            String savedCrashLog = App.getSavedCrashLog(getApplication());
            if (savedCrashLog != null) {
                Log.d("MainActivity", "Found saved crash log, launching DebugActivity");
                // Launch DebugActivity with saved crash
                Intent debugIntent = new Intent(this, DebugActivity.class);
                debugIntent.putExtra("error_message", savedCrashLog);
                debugIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(debugIntent);
                
                // Clear the saved crash log
                App.clearSavedCrashLog(getApplication());
                finish(); // Close MainActivity
                return true;
            }
            
        } catch (Exception e) {
            Log.e("MainActivity", "Error in crash recovery", e);
        }
        
        return false; // No crash to handle, continue normally
    }
    
    private void setupViewPager() {
        viewPagerAdapter = new ViewPagerAdapter(this);
        binding.contentPage.setAdapter(viewPagerAdapter);
        binding.contentPage.setOffscreenPageLimit(1); // Keep all fragments in memory
        binding.contentPage.setUserInputEnabled(false);
        
        // Handle page changes
        binding.contentPage.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Update bottom navigation selection when page changes
                switch (position) {
                    case 0:
                        binding.bottomNav.setSelectedItemId(R.id.nav_home);
                        break;
                    case 1:
                        binding.bottomNav.setSelectedItemId(R.id.nav_short);
                        break;
                    case 2:
                        binding.bottomNav.setSelectedItemId(R.id.nav_subscription);
                        break;
                    case 3:
                        binding.bottomNav.setSelectedItemId(R.id.nav_me);
                        break;
                }
            }
        });
    }
    
    private void setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                binding.contentPage.setCurrentItem(0, true);
                return true;
            } else if (itemId == R.id.nav_short) {
                binding.contentPage.setCurrentItem(1, true);
                return true;
            } else if (itemId == R.id.nav_subscription) {
                binding.contentPage.setCurrentItem(2, true);
                return true;
            } else if (itemId == R.id.nav_me) {
                binding.contentPage.setCurrentItem(3, true);
                return true;
            }
            
            return false;
        });
        
        // Set default selection
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
    }
    
    @Override
    public void onBackPressed() {
        // If not on home tab, go to home tab
        if (binding.contentPage.getCurrentItem() != 0) {
            binding.contentPage.setCurrentItem(0, true);
        } else {
            // If on home tab, exit app
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
    
    // Test crash functionality (for debugging only - remove in production)
    private void addTestCrashButton() {
        // Only add this in debug builds for testing
        if (BuildConfig.DEBUG) {
            try {
                Button testCrashButton = new Button(this);
                testCrashButton.setText("Test Crash");
                testCrashButton.setOnClickListener(v -> {
                    Log.d("TestCrash", "Triggering test crash");
                    throw new RuntimeException("Test crash - DebugActivity should open after this");
                });
                
                // Add the button to your layout programmatically
                // You can also add it to your XML layout instead
                
            } catch (Exception e) {
                Log.w("TestCrash", "Could not add test crash button", e);
            }
        }
    }
    
    // ViewPager2 Adapter
    private static class ViewPagerAdapter extends FragmentStateAdapter {
        
        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new HomeFragment();
                case 1:
                    return new ShortFragment();
                case 2:
                    return new SubscriptionFragment();
                case 3:
                    return new MeFragment();
                default:
                    return new HomeFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 4; // Number of tabs
        }
    }
}