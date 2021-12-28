package com.picassos.noted.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.picassos.noted.R;
import com.picassos.noted.constants.RequestCodes;
import com.picassos.noted.databases.APP_DATABASE;
import com.picassos.noted.entities.TodosList;
import com.picassos.noted.utils.Helper;
import com.picassos.noted.utils.Toasto;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class TodoCreateListActivity extends AppCompatActivity {

    Button saveList;
    EditText listTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OPTIONS
        Helper.dark_mode(this);
        Helper.fullscreen_mode(this);
        Helper.screen_state(this);

        setContentView(R.layout.activity_todo_create_list);

        // finish activity
        findViewById(R.id.go_back).setOnClickListener(v -> finish());

        // list title
        listTitle = findViewById(R.id.todo_list_title);
        listTitle.addTextChangedListener(listTitleTextWatcher);
        listTitle.requestFocus();
        // show keyboard
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        Objects.requireNonNull(inputMethodManager).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        // save list
        saveList = findViewById(R.id.todo_list_save);
        saveList.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(listTitle.getText().toString())) {
                requestSaveList(listTitle.getText().toString());
            } else {
                Toasto.show_toast(this, getString(R.string.todo_list_title_required), 1, 2);
            }
        });
    }

    /**
     * request save to-do list
     * @param title for to-do list title
     */
    private void requestSaveList(String title) {
        Date date = new Date();
        final TodosList todosList = new TodosList();
        todosList.setTodo_list_identifier(Integer.parseInt(new SimpleDateFormat("ddHHmmss", Locale.US).format(date)));
        todosList.setTodo_list_title(title);

        @SuppressLint("StaticFieldLeak")
        class SaveTodoListTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                APP_DATABASE.requestDatabase(getApplicationContext()).dao().request_insert_todo_list(todosList);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                Objects.requireNonNull(inputMethodManager).toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

                Intent intent = new Intent();
                intent.putExtra("is_added", true);
                setResult(RequestCodes.REQUEST_CREATE_LIST_CODE, intent);
                finish();
            }

        }

        new SaveTodoListTask().execute();
    }

    /**
     * text watcher enables save
     * button when list title is not
     * empty and any fields
     */
    private final TextWatcher listTitleTextWatcher = new TextWatcher() {
        @SuppressLint("SetTextI18n")
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            saveList.setEnabled(!TextUtils.isEmpty(listTitle.getText().toString()));
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
}