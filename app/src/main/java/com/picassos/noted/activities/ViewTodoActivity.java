package com.picassos.noted.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.picassos.noted.R;
import com.picassos.noted.constants.Constants;
import com.picassos.noted.constants.RequestCodes;
import com.picassos.noted.databases.APP_DATABASE;
import com.picassos.noted.entities.Todo;
import com.picassos.noted.sharedPreferences.SharedPref;
import com.picassos.noted.sheets.TodoMoveToBottomSheetModal;
import com.picassos.noted.utils.Helper;
import com.picassos.noted.utils.Toasto;

public class ViewTodoActivity extends AppCompatActivity implements TodoMoveToBottomSheetModal.OnMoveListener {

    // Shared Preferences
    SharedPref sharedPref;

    // States
    private final static int STATE_UNCOMPLETED = 0;
    private final static int STATE_COMPLETED = 1;

    // Bundle
    private Bundle bundle;

    // rewarded video ad
    private RewardedAd rewardedAd;

    // preset to-do
    private Todo presetTodo;

    private EditText todoTitle;
    private EditText todoDetails;
    private TextView todoCreatedAt;
    private SwitchCompat todoPriority;

    // to-do list
    private int todoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        sharedPref = new SharedPref(this);

        super.onCreate(savedInstanceState);

        // OPTIONS
        Helper.dark_mode(this);
        Helper.fullscreen_mode(this);
        Helper.screen_state(this);

        setContentView(R.layout.activity_view_todo);

        // initialize bundle
        bundle = new Bundle();

        if (Constants.ENABLE_GOOGLE_ADMOB_ADS) {
            MobileAds.initialize(this, initializationStatus -> {});
            loadRewardedVideoAd();
        }

        /* check if the note is preset (available or exist) */
        if (getIntent().getBooleanExtra("modifier", false)) {
            presetTodo = (Todo) getIntent().getSerializableExtra("todo");
            bundle.putSerializable("todo_data", presetTodo);
        }

        // finish activity
        findViewById(R.id.go_back).setOnClickListener(v -> requestSaveTodo(presetTodo));

        // delete to-do
        findViewById(R.id.todo_delete).setOnClickListener(v -> requestDeleteTodo(presetTodo));

        // more options
        findViewById(R.id.more_options).setOnClickListener(v -> {
            bundle.putSerializable("todo_data", presetTodo);
            TodoMoveToBottomSheetModal todoMoveToBottomSheetModal = new TodoMoveToBottomSheetModal();
            todoMoveToBottomSheetModal.setArguments(bundle);
            todoMoveToBottomSheetModal.show(getSupportFragmentManager(), "TAG");
        });

        // to-do list
        todoList = presetTodo.getTodo_list();

        // to-do title
        todoTitle = findViewById(R.id.todo_title);
        todoTitle.setText(presetTodo.getTodo_title());

        // to-do details
        todoDetails = findViewById(R.id.todo_details);
        todoDetails.setText(presetTodo.getTodo_details());

        // to-do created at
        todoCreatedAt = findViewById(R.id.todo_created_at);
        todoCreatedAt.setText(presetTodo.getTodo_created_at());

        // to-do priority
        todoPriority = findViewById(R.id.todo_priority);
        todoPriority.setChecked(presetTodo.isTodo_priority());

        // mark to-do
        TextView markTodo = findViewById(R.id.mark_todo);
        if (presetTodo.isTodo_state()) {
            markTodo.setText(getString(R.string.mark_uncompleted));
        } else {
            markTodo.setText(getString(R.string.mark_completed));
        }
        markTodo.setOnClickListener(v -> {
            if (presetTodo.isTodo_state()) {
                requestMarkTodo(presetTodo, STATE_UNCOMPLETED);
            } else {
                requestMarkTodo(presetTodo, STATE_COMPLETED);
            }
        });
    }

    /**
     * request delete to-do
     * @param todo for to-do
     */
    private void requestDeleteTodo(Todo todo) {
        if (todo != null) {
            @SuppressLint("StaticFieldLeak")
            class DeleteTodoTask extends AsyncTask<Void, Void, Void> {
                @Override
                protected Void doInBackground(Void... voids) {
                    APP_DATABASE.requestDatabase(getApplicationContext()).dao().request_delete_todo(todo);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    Intent intent = new Intent();
                    setResult(RequestCodes.REQUEST_ACTION_TODO_CODE, intent);
                    finish();
                }
            }

            new DeleteTodoTask().execute();
        }
    }

    /**
     * request mark to-do as completed
     * @param todo for to-do
     */
    private void requestMarkTodo(Todo todo, int state) {
        if (todo != null) {
            @SuppressLint("StaticFieldLeak")
            class MarkTodoTask extends AsyncTask<Void, Void, Void> {
                @Override
                protected Void doInBackground(Void... voids) {
                    APP_DATABASE.requestDatabase(getApplicationContext()).dao().request_mark_todo(todo.getTodo_id(), state);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    Intent intent = new Intent();
                    setResult(RequestCodes.REQUEST_ACTION_TODO_CODE, intent);
                    finish();
                }
            }

            new MarkTodoTask().execute();
        }
    }

    /**
     * request save preset to-do
     * @param todo for to-do
     */
    private void requestSaveTodo(Todo todo) {
        if (todo != null) {
            if (!TextUtils.isEmpty(todoTitle.getText().toString())) {
                todo.setTodo_title(todoTitle.getText().toString());
                todo.setTodo_details(todoDetails.getText().toString());
                todo.setTodo_created_at(todoCreatedAt.getText().toString());
                todo.setTodo_priority(todoPriority.isChecked());
                todo.setTodo_list(todoList);

                @SuppressLint("StaticFieldLeak")
                class SaveTodoTask extends AsyncTask<Void, Void, Void> {

                    @Override
                    protected Void doInBackground(Void... voids) {
                        APP_DATABASE.requestDatabase(getApplicationContext()).dao().request_insert_todo(todo);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        if (Constants.ENABLE_GOOGLE_ADMOB_ADS) {
                            loadRewardedVideoAd();
                            if (rewardedAd != null) {
                                if (sharedPref.loadRewardedAmount() >= 3) {
                                    sharedPref.setRewardedAmount(0);
                                    showRewardedVideo();
                                }
                            }
                            sharedPref.setRewardedAmount(sharedPref.loadRewardedAmount() + 1);

                            Intent intent = new Intent();
                            setResult(RequestCodes.REQUEST_ACTION_TODO_CODE, intent);
                        } else {
                            Intent intent = new Intent();
                            setResult(RequestCodes.REQUEST_ACTION_TODO_CODE, intent);
                        }
                        finish();
                    }
                }

                new SaveTodoTask().execute();
            } else {
                Toasto.show_toast(this, getString(R.string.todo_title_required), 1, 2);
            }
        }
    }

    /**
     * load Google AdMob rewarded ad
     */
    private void loadRewardedVideoAd() {
        if (rewardedAd == null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            RewardedAd.load(
                    this,
                    Constants.GOOGLE_ADMOB_REWARDED_AD_UNIT_ID,
                    adRequest,
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            rewardedAd = null;
                        }

                        @Override
                        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                            ViewTodoActivity.this.rewardedAd = rewardedAd;
                        }
                    });
        }
    }

    private void showRewardedVideo() {
        rewardedAd.setFullScreenContentCallback(
                new FullScreenContentCallback() {
                    @Override
                    public void onAdShowedFullScreenContent() {
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        rewardedAd = null;
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        rewardedAd = null;
                        ViewTodoActivity.this.loadRewardedVideoAd();
                    }
                });
        rewardedAd.show(
                this,
                rewardItem -> finish());
    }

    @Override
    public void onBackPressed() {
        requestSaveTodo(presetTodo);
    }

    @Override
    public void onMoveListener(int requestCode, int identifier) {
        if (requestCode == RequestCodes.REQUEST_MOVE_TASK_CODE) {
            todoList = identifier;
        }
    }
}