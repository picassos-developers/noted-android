package com.picassos.noted.activities;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.picassos.noted.R;
import com.picassos.noted.constants.Constants;
import com.picassos.noted.constants.RequestCodes;
import com.picassos.noted.databases.APP_DATABASE;
import com.picassos.noted.databases.DAO;
import com.picassos.noted.entities.Category;
import com.picassos.noted.entities.Notification;
import com.picassos.noted.sharedPreferences.SharedPref;
import com.picassos.noted.utils.Toasto;

import java.util.concurrent.Executor;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    SharedPref sharedPref;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("NEW_INTENT", "On new intent started");

        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                Log.d("GOOGLE_FIREBASE", "Extras received at onCreate: Key: " + key + " Value: " + value);
            }
            String title = extras.getString("title");
            String message = extras.getString("body");
            if (message != null && message.length() > 0) {
                getIntent().removeExtra("body");
                saveNotification(title, message);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new SharedPref(this);

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                Log.d("GOOGLE_FIREBASE", "Extras received at onCreate: Key: " + key + " Value: " + value);
            }
            String title = extras.getString("title");
            String message = extras.getString("body");
            if (message != null && message.length() > 0) {
                getIntent().removeExtra("body");
                saveNotification(title, message);
            }
        }

        setContentView(R.layout.activity_splash);

        if (sharedPref.loadFirstTimeCategories()) {
            /* default categories start */
            requestAddDefaultCategories("Dream");
            requestAddDefaultCategories("Shopping");
            requestAddDefaultCategories("Study");
            requestAddDefaultCategories("Work");
            requestAddDefaultCategories("Home");
            /* default categories end */
            sharedPref.setFirstTimeCategories(false);
        }

        new Handler().postDelayed(() -> {

            if (Constants.ENABLE_FINGERPRINT_LOGIN) {
                if (sharedPref.loadFingerprintOption()) {
                    requestBiometricInstance();
                } else {
                    requestLaunchMainInstance();
                }
            } else {
                requestLaunchMainInstance();
            }

        }, Constants.SPLASH_SCREEN_TIMEOUT);

    }

    /**
     * request to save category
     * @param title for category title
     */
    private void requestAddDefaultCategories(String title) {
        final Category category = new Category();

        category.setCategory_title(title);
        category.setCategory_is_primary(false);

        @SuppressLint("StaticFieldLeak")
        class SaveCategoryTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                APP_DATABASE.requestDatabase(getApplicationContext()).dao().request_insert_category(category);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Log.e("MY_NOTES", "Default category is added.");
            }
        }

        new SaveCategoryTask().execute();
    }

    private void requestLaunchMainInstance() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }

    private void requestBiometricInstance() {
        // create a new executor
        Executor executor = ContextCompat.getMainExecutor(this);

        // create biometric prompt for callback
        BiometricPrompt biometricPrompt = new BiometricPrompt(SplashActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    finish();
                    System.exit(0);
                } else if (errorCode == 7 || errorCode == 4) {
                    KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                    if (keyguardManager.isKeyguardSecure()) {
                        Intent intent = keyguardManager.createConfirmDeviceCredentialIntent("", "");
                        intent.putExtra("request", RequestCodes.INTENT_AUTH_REQUEST_CODE);
                        startActivityForResult.launch(intent);
                    } else {
                        requestLaunchMainInstance();
                    }
                } else {
                    Toasto.show_toast(getApplicationContext(), errString.toString(), 1, 1);
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                requestLaunchMainInstance();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toasto.show_toast(getApplicationContext(), getString(R.string.failed_to_authenticate_fingerprint), 1, 2);
            }
        });

        BiometricPrompt.PromptInfo promptInfo;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Confirm fingerprint to continue")
                    .setAllowedAuthenticators(BIOMETRIC_STRONG | DEVICE_CREDENTIAL)
                    .setConfirmationRequired(true)
                    .build();
        } else {
            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Confirm fingerprint to continue")
                    .setAllowedAuthenticators(BIOMETRIC_STRONG)
                    .setNegativeButtonText("Cancel")
                    .setConfirmationRequired(true)
                    .build();
        }

        biometricPrompt.authenticate(promptInfo);
    }

    /**
     * request save notification into room db
     * @param title for notification title
     * @param message for notification message
     */
    private void saveNotification(String title, String message) {
        DAO dao = APP_DATABASE.requestDatabase(this).dao();
        Notification notification = new Notification();
        notification.title = title;
        notification.content = message;
        notification.id = System.currentTimeMillis();
        notification.created_at = System.currentTimeMillis();
        notification.read = false;
        dao.requestInsertNotification(notification);
    }

    @SuppressLint("NotifyDataSetChanged")
    ActivityResultLauncher<Intent> startActivityForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result != null && result.getResultCode() == RESULT_OK) {
            if (result.getData() != null) {
                if (result.getData().getIntExtra("request", 0) == RequestCodes.INTENT_AUTH_REQUEST_CODE) {
                    requestLaunchMainInstance();
                }
            }
        }
    });
}
