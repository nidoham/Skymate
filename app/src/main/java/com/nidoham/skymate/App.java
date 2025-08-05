package com.nidoham.skymate;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.nidoham.skymate.error.ReCaptchaActivity;
import com.nidoham.strivo.Localization.Localizations;
import com.nidoham.strivo.settings.ApplicationSettings;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.util.image.PicassoHelper;

public class App extends Application {

    private static final String TAG = "SkymateApp";
    private static final String CRASH_LOG_KEY = "crash_log";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize settings
        ApplicationSettings.getInstance(this);
        PicassoHelper.init(this);
        
        // Setup crash handler
        Thread.setDefaultUncaughtExceptionHandler(this::handleAppCrash);

        // Initialize NewPipe
        try {
            initializeNewPipe();
            setupLocalization();
        } catch (Exception e) {
            Log.e(TAG, "Critical initialization error", e);
            handleAppCrash(Thread.currentThread(), e);
        }
    }

    private void handleAppCrash(Thread thread, Throwable throwable) {
        try {
            Log.e(TAG, "App crashed", throwable);
            
            String crashLog = Log.getStackTraceString(throwable);
            saveCrashLog(crashLog);
            launchDebugActivity(crashLog);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling crash", e);
        } finally {
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    private void launchDebugActivity(String crashLog) {
        try {
            Intent intent = new Intent();
            intent.setClassName(getPackageName(), DebugActivity.class.getName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                          Intent.FLAG_ACTIVITY_CLEAR_TOP |
                          Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("error_message", crashLog);
            intent.putExtra("crash_time", System.currentTimeMillis());
            
            if (Looper.myLooper() == Looper.getMainLooper()) {
                startActivity(intent);
            } else {
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start debug activity", e);
                    }
                });
            }
            
            Thread.sleep(1000); // Wait for activity launch
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch debug activity", e);
        }
    }

    private void initializeNewPipe() throws ExtractionException {
        NewPipe.init(createDownloader());
    }

    private Downloader createDownloader() {
        final DownloaderImpl downloader = DownloaderImpl.init(null);
        setCookies(downloader);
        return downloader;
    }

    private void setCookies(DownloaderImpl downloader) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String cookieKey = getString(R.string.recaptcha_cookies_key);
            downloader.setCookie(ReCaptchaActivity.RECAPTCHA_COOKIES_KEY, prefs.getString(cookieKey, null));
            downloader.updateYoutubeRestrictedModeCookies(this);
        } catch (Exception e) {
            Log.w(TAG, "Error setting cookies", e);
        }
    }

    private void setupLocalization() {
        try {
            Localizations.applySettingsLocale(this);
        } catch (Exception e) {
            Log.w(TAG, "Error applying localization", e);
        }
    }

    private void saveCrashLog(String crashLog) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit()
                .putString(CRASH_LOG_KEY, crashLog)
                .putLong(CRASH_LOG_KEY + "_time", System.currentTimeMillis())
                .commit();
            Log.d(TAG, "Crash log saved");
        } catch (Exception e) {
            Log.w(TAG, "Failed to save crash log", e);
        }
    }

    public static String getSavedCrashLog(Application app) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
            return prefs.getString(CRASH_LOG_KEY, null);
        } catch (Exception e) {
            return null;
        }
    }

    public static void clearSavedCrashLog(Application app) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
            prefs.edit()
                .remove(CRASH_LOG_KEY)
                .remove(CRASH_LOG_KEY + "_time")
                .apply();
        } catch (Exception e) {
            Log.w(TAG, "Failed to clear crash log", e);
        }
    }
}