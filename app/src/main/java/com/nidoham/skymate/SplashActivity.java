package com.nidoham.skymate;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.nidoham.localization.CountryCode;
import com.nidoham.localization.LanguageCode;
import com.nidoham.skymate.databinding.ActivitySplashBinding;
import com.nidoham.strivo.settings.ApplicationSettings;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DURATION = 2000; // 2 seconds
    
    private ActivitySplashBinding binding;
    private ApplicationSettings settings;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check for crash recovery FIRST - before any UI setup
        if (handleCrashRecovery()) {
            return; // Don't continue with normal splash if handling crash
        }

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize settings
        settings = ApplicationSettings.getInstance(this);
        setupSystemSettings();
        
        // Enable YouTube restricted mode by default
        settings.setRestrictedModeEnabled(true);

        // Launch MainActivity after splash duration
        new Handler().postDelayed(() -> {
            try {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } catch (Exception e) {
                Log.e(TAG, "Error launching MainActivity", e);
                String crashLog = "Failed to launch MainActivity: " + Log.getStackTraceString(e);
                launchDebugActivity(crashLog);
            }
        }, SPLASH_DURATION);
    }

    private boolean handleCrashRecovery() {
        try {
            Intent intent = getIntent();

            // Check if launched for debug purposes
            boolean shouldLaunchDebug = intent.getBooleanExtra("launch_debug", false);
            String crashMessage = intent.getStringExtra("error_message");

            if (shouldLaunchDebug && crashMessage != null) {
                Log.d(TAG, "Launching DebugActivity for crash recovery from intent");
                launchDebugActivity(crashMessage);
                return true;
            }

            // Check for saved crash log
            String savedCrashLog = App.getSavedCrashLog(getApplication());
            if (savedCrashLog != null) {
                Log.d(TAG, "Found saved crash log, launching DebugActivity");
                launchDebugActivity(savedCrashLog);
                App.clearSavedCrashLog(getApplication());
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in crash recovery", e);
            try {
                String crashLog = "Crash recovery failed: " + Log.getStackTraceString(e);
                launchDebugActivity(crashLog);
                return true;
            } catch (Exception e2) {
                Log.e(TAG, "Complete crash recovery failure", e2);
            }
        }

        return false;
    }

    private void launchDebugActivity(String crashMessage) {
        try {
            Intent debugIntent = new Intent(this, DebugActivity.class);
            debugIntent.putExtra("error_message", crashMessage);
            debugIntent.putExtra("crash_time", System.currentTimeMillis());
            debugIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(debugIntent);
            finish();
            Log.d(TAG, "DebugActivity launched successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch DebugActivity", e);
            finish();
        }
    }
    
    private void setupSystemSettings() {
    	/// Currently only support inside BD-bn
        settings.setRegionCode(CountryCode.BANGLADESH.getCode());
        settings.setDisplayLanguage(LanguageCode.BANGLA.getCode());
        settings.setRestrictedModeEnabled(true);
        
        //Toast.makeText(getApplicationContext(), settings.getDisplayLanguage() + "-" + settings.getRegionCode() ,Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        Log.d(TAG, "SplashActivity destroyed");
    }
}