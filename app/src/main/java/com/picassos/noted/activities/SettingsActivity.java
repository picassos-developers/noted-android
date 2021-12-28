package com.picassos.noted.activities;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.biometric.BiometricManager;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


import com.picassos.noted.R;
import com.picassos.noted.constants.Constants;
import com.picassos.noted.databases.APP_DATABASE;
import com.picassos.noted.sharedPreferences.SharedPref;
import com.picassos.noted.sheets.PinOptionsBottomSheetModal;
import com.picassos.noted.utils.Helper;
import com.picassos.noted.utils.Toasto;

import de.raphaelebner.roomdatabasebackup.core.RoomBackup;

public class SettingsActivity extends AppCompatActivity implements PinOptionsBottomSheetModal.OnRemoveListener {

    private SharedPref sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new SharedPref(this);

        super.onCreate(savedInstanceState);

        // OPTIONS
        Helper.dark_mode(this);
        Helper.fullscreen_mode(this);
        Helper.screen_state(this);

        setContentView(R.layout.activity_settings);

        // initialize room backup
        final RoomBackup backupDatabase = new RoomBackup(SettingsActivity.this);

        // return back and finish activity
        ImageView go_back = findViewById(R.id.go_back);
        go_back.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, MainActivity.class));
            finish();
        });

        // options
        SwitchCompat darkMode = findViewById(R.id.dark_mode);
        SwitchCompat fullscreenMode = findViewById(R.id.fullscreen_mode);
        SwitchCompat screenOnMode = findViewById(R.id.screen_on_mode);
        SwitchCompat loginWithFingerprint = findViewById(R.id.login_with_fingerprint);

        // check if dark mode is enabled
        if (sharedPref.loadNightModeState()) {
            darkMode.setChecked(true);
        }

        // check if fullscreen mode is enabled
        if (sharedPref.loadFullscreenState()) {
            fullscreenMode.setChecked(true);
        }

        // check if screen state is enabled
        if (sharedPref.loadScreenState()) {
            screenOnMode.setChecked(true);
        }

        // check if login with fingerprint is enabled
        if (sharedPref.loadFingerprintOption()) {
            loginWithFingerprint.setChecked(true);
        }

        // dark mode
        darkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPref.setNightModeState(isChecked);
            restartContext();
        });

        // fullscreen mode
        fullscreenMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPref.setFullscreenState(isChecked);
            restartContext();
        });

        // screen on mode
        screenOnMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPref.setScreenState(isChecked);
            restartContext();
        });

        // check if fingerprint is enabled
        if (Constants.ENABLE_FINGERPRINT_LOGIN) {
            // check if fingerprint is supported
            BiometricManager biometricManager = BiometricManager.from(this);

            int authenticators;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                authenticators = BIOMETRIC_STRONG | DEVICE_CREDENTIAL;
            } else {
                authenticators = BIOMETRIC_STRONG;
            }

            // check if can authenticate
            switch (biometricManager.canAuthenticate(authenticators)) {
                case BiometricManager.BIOMETRIC_SUCCESS:
                    findViewById(R.id.login_with_fingerprint_container).setVisibility(View.VISIBLE);
                    break;
                case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
                case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:
                    findViewById(R.id.login_with_fingerprint_container).setVisibility(View.GONE);
                    break;
            }
            // login with fingerprint
            loginWithFingerprint.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sharedPref.setFingerprintOption(isChecked);
                restartContext();
            });
        } else {
            findViewById(R.id.login_with_fingerprint_container).setVisibility(View.GONE);
        }

        // note pin code
        findViewById(R.id.pin_lock).setOnClickListener(v -> pinLock());

        // share app
        findViewById(R.id.share_app).setOnClickListener(v -> shareApp());

        // backup notes
        findViewById(R.id.backup_notes).setOnClickListener(v -> {
            backupDatabase.database(APP_DATABASE.requestDatabase(this))
                    .enableLogDebug(true)
                    .backupIsEncrypted(false)
                    .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_INTERNAL)
                    .maxFileCount(5)
                    .customBackupFileName("database.sqlite3")
                    .onCompleteListener((success, message) -> {
                        Toasto.show_toast(this, message, 1, 0);
                    }).backup();
        });



        // restore notes
        findViewById(R.id.restore_notes).setOnClickListener(v -> {
            backupDatabase.database(APP_DATABASE.requestDatabase(this))
                    .enableLogDebug(true)
                    .backupIsEncrypted(false)
                    .customBackupFileName("database.sqlite3")
                    .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_INTERNAL)
                    .onCompleteListener((success, message) -> {
                        if (success) {
                            startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                            finishAffinity();
                        } else {
                            finish();
                        }
                    }).restore();
        });

        // send feedback
        findViewById(R.id.send_feedback).setOnClickListener(v -> sendFeedback());

        // privacy policy
        findViewById(R.id.privacy_policy).setOnClickListener(v -> privacyPolicy());

        // terms of use
        findViewById(R.id.terms_of_use).setOnClickListener(v -> termsOfUse());

        // about app
        findViewById(R.id.about_app).setOnClickListener(v -> aboutApp());

        // copyright notice
        TextView copyrightNotice = findViewById(R.id.copyright_notice);
        copyrightNotice.setText(Helper.copyright(getApplicationContext()));
    }

    /**
     * request note pin code
     */
    private void pinLock() {
        PinOptionsBottomSheetModal pinOptionsBottomSheetModal = new PinOptionsBottomSheetModal();
        pinOptionsBottomSheetModal.show(getSupportFragmentManager(), "TAG");
    }

    /**
     * share app method so users
     * can share the app.
     */
    private void shareApp() {
        Intent share = new Intent(Intent.ACTION_SEND);

        share.setType("text/plain");

        share.putExtra(Intent.EXTRA_TEXT, R.string.share_description);

        startActivity(Intent.createChooser(share, getString(R.string.share) + " " + getString(R.string.app_name)));
    }

    /**
     * users can send feedback
     * directly to the app developer's
     * email address. Gmail is required.
     */
    private void sendFeedback() {
        Intent feedbackIntent = new Intent(Intent.ACTION_SEND);

        feedbackIntent.setType("text/pain");

        // check if GMAIL app is installed
        feedbackIntent.setPackage("com.google.android.gm");

        feedbackIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{Constants.EMAIL_ADDRESS});

        feedbackIntent.putExtra(Intent.EXTRA_SUBJECT, new String[]{getString(R.string.feedback_subject)});

        // try to open gmail app, if !installed, exception will be showed

        try {

            startActivity(feedbackIntent);

        } catch (android.content.ActivityNotFoundException ex) {

            Toasto.show_toast(this, getString(R.string.gmail_not_installed), 1, 2);

        }
    }

    /**
     * open privacy policy link on
     * the default browser.
     */
    private void privacyPolicy() {
        Intent privacyPolicy = new Intent(Intent.ACTION_VIEW);
        privacyPolicy.setData(Uri.parse(Constants.PRIVACY_POLICY_URL));
        startActivity(privacyPolicy);
    }

    /**
     * open terms of use link on
     * the default browser.
     */
    private void termsOfUse() {
        Intent termsOfUse = new Intent(Intent.ACTION_VIEW);
        termsOfUse.setData(Uri.parse(Constants.TERMS_OF_USE_URL));
        startActivity(termsOfUse);
    }

    /**
     * open about app activity
     */
    private void aboutApp() {
        Intent about = new Intent(SettingsActivity.this, AboutActivity.class);
        startActivity(about);
    }

    /**
     * restart activity to apply
     * all changes made by user.
     */
    private void restartContext() {
        Intent restart = new Intent(getApplicationContext(), SettingsActivity.class);
        restart.putExtra("is_option", true);
        startActivity(restart);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(SettingsActivity.this, MainActivity.class));
    }

    @Override
    public void onRemoveListener(int requestCode) {
        if (requestCode == PinOptionsBottomSheetModal.REQUEST_REMOVE_PIN_CODE) {
            Toasto.show_toast(this, getString(R.string.pin_code_unset), 1, 2);
        }
    }
}