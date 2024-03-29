package com.picassos.noted.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.picassos.noted.R;
import com.picassos.noted.adapters.NotesAdapter;
import com.picassos.noted.constants.RequestCodes;
import com.picassos.noted.databases.APP_DATABASE;
import com.picassos.noted.entities.Note;
import com.picassos.noted.listeners.NotesActionListener;
import com.picassos.noted.listeners.NotesListener;
import com.picassos.noted.sharedPreferences.SharedPref;
import com.picassos.noted.sheets.ImplementedPasswordBottomSheetModal;
import com.picassos.noted.utils.Helper;
import com.picassos.noted.utils.Toasto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SearchActivity extends AppCompatActivity implements NotesListener, NotesActionListener, ImplementedPasswordBottomSheetModal.OnUnlockListener {

    Bundle bundle;
    SharedPref sharedPref;

    private List<Note> notes;
    private NotesAdapter notesAdapter;

    // selected filter
    private String selectedFilterResource;
    private char selectedFilter;

    boolean isClosed = false;

    EditText searchBar;

    int noteClickedPosition = -1;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = new SharedPref(this);

        // OPTIONS
        Helper.dark_mode(this);
        Helper.fullscreen_mode(this);
        Helper.screen_state(this);

        setContentView(R.layout.activity_search);

        // bundle
        bundle = new Bundle();

        // finish activity
        findViewById(R.id.go_back).setOnClickListener(v -> {
            startActivity(new Intent(SearchActivity.this, MainActivity.class));
            finish();
        });

        // notes recyclerview
        RecyclerView notesRecyclerview = findViewById(R.id.notes_recyclerview);
        notesRecyclerview.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        // notes list, adapter
        notes = new ArrayList<>();
        notesAdapter = new NotesAdapter(notes, this, this);
        notesRecyclerview.setAdapter(notesAdapter);

        // check if notes are not empty
        if (notes.size() != 0) {
            notes.clear();
            notesAdapter.notifyDataSetChanged();
        }

        // selected filter
        // default -> global
        selectedFilterResource = "";
        selectedFilter = 'G'; // FOR GLOBAL

        // search bar
        searchBar = findViewById(R.id.search_bar);
        searchBar.addTextChangedListener(searchTextWatcher);

        // search mic (search with voice)
        ImageView searchMic = findViewById(R.id.search_mic);
        searchMic.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_notes));

            try {
                startActivityForResult.launch(intent);
            } catch (Exception e) {
                Toasto.show_toast(this, e.getMessage(), 1, 1);
            }
        });

        // initialize color search
        requestInitializeColorSearch();

        // initialize type search
        requestInitializeTypes();
    }

    private void requestSetSearchAction() {
        isClosed = false;
        // hide search content container
        findViewById(R.id.search_content_container).setVisibility(View.GONE);
        // hide back arrow
        // and show close icon
        findViewById(R.id.go_back).setVisibility(View.GONE);
        ImageView closeSearch = findViewById(R.id.close_search);
        closeSearch.setVisibility(View.VISIBLE);
        // request focus for search bar
        // and show the keyboard
        searchBar.requestFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        Objects.requireNonNull(inputMethodManager).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        // initialize action for close search
        closeSearch.setOnClickListener(v -> request_close_search());
    }

    private void request_close_search() {
        // hide keyboard
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        Objects.requireNonNull(inputMethodManager).hideSoftInputFromWindow(Objects.requireNonNull(getCurrentFocus()).getWindowToken(), 0);
        // hide close icon and
        // and show back arrow and
        // search content container
        findViewById(R.id.close_search).setVisibility(View.GONE);
        findViewById(R.id.go_back).setVisibility(View.VISIBLE);
        findViewById(R.id.search_content_container).setVisibility(View.VISIBLE);
        // set selected filter
        // for global search
        selectedFilterResource = "";
        selectedFilter = 'G'; // FOR GLOBAL
        // clear search bar text
        isClosed = true;
        searchBar.getText().clear();
    }

    private void request_search_notes(String keyword) {
        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>> {

            @Override
            protected List<Note> doInBackground(Void... voids) {
                switch (selectedFilter) {
                    case 'C':
                        return APP_DATABASE.requestDatabase(getApplicationContext()).dao().request_search_notes_by_color(keyword, selectedFilterResource);
                    case 'T':
                        switch (selectedFilterResource) {
                            case "_images":
                                return APP_DATABASE.requestDatabase(getApplicationContext()).dao().request_search_notes_by_images(keyword);
                            case "_videos":
                                return APP_DATABASE.requestDatabase(getApplicationContext()).dao().request_search_notes_by_video(keyword);
                            case "_reminders":
                                return APP_DATABASE.requestDatabase(getApplicationContext()).dao().request_search_notes_by_reminder(keyword);
                        }
                    case 'G':
                    default:
                        return APP_DATABASE.requestDatabase(getApplicationContext()).dao().request_search_notes_by_global(keyword);
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void onPostExecute(List<Note> notes_inline) {
                super.onPostExecute(notes_inline);
                notes.addAll(notes_inline);
                notesAdapter.notifyDataSetChanged();
            }

        }
        new GetNotesTask().execute();
    }

    /**
     * text watcher for notes search bar
     */
    private final TextWatcher searchTextWatcher = new TextWatcher() {
        @SuppressLint("SetTextI18n")
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (count == 0) {
                requestSetSearchAction();
            }
        }

        @SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            notes.clear();
            notesAdapter.notifyDataSetChanged();
        }

        @Override
        public void afterTextChanged(Editable s) {

            if (!isClosed) {
                request_search_notes(s.toString());
            }
        }
    };

    @Override
    public void onNoteAction(Note note, int position, boolean isSelected) {
        noteClickedPosition = position;

        if (sharedPref.loadNotePinCode() == 0) {
            Intent intent = new Intent(SearchActivity.this, AddNoteActivity.class);
            intent.putExtra("modifier", true);
            intent.putExtra("note", note);
            startActivityForResult.launch(intent);
        } else {
            if (note.isNote_locked()) {
                bundle.putSerializable("data", note);
                bundle.putInt("activity", 1);
                ImplementedPasswordBottomSheetModal passwordBottomSheetModal = new ImplementedPasswordBottomSheetModal();
                passwordBottomSheetModal.setArguments(bundle);
                passwordBottomSheetModal.show(getSupportFragmentManager(), "TAG");
            } else {
                Intent intent = new Intent(SearchActivity.this, AddNoteActivity.class);
                intent.putExtra("modifier", true);
                intent.putExtra("note", note);
                startActivityForResult.launch(intent);
            }
        }
    }

    @Override
    public void onNoteClicked(Note note, int position) {

    }

    @Override
    public void onNoteLongClicked(Note note, int position) {

    }

    @Override
    public void onUnlockListener(Note note) {
        Intent intent = new Intent(SearchActivity.this, AddNoteActivity.class);
        intent.putExtra("modifier", true);
        intent.putExtra("note", note);
        startActivityForResult.launch(intent);
    }

    /**
     * request initialize type search
     * by setting filter for search
     */
    private void requestInitializeTypes() {
        // type images
        findViewById(R.id.type_images).setOnClickListener(v -> {
            selectedFilter = 'T';
            selectedFilterResource = "_images";
            requestSetSearchAction();
        });

        // type videos
        findViewById(R.id.type_videos).setOnClickListener(v -> {
            selectedFilter = 'T';
            selectedFilterResource = "_videos";
            requestSetSearchAction();
        });

        // type reminders
        findViewById(R.id.type_reminders).setOnClickListener(v -> {
            selectedFilter = 'T';
            selectedFilterResource = "_reminders";
            requestSetSearchAction();
        });
    }

    /**
     * request initialize color search
     * by setting filter for search
     */
    private void requestInitializeColorSearch() {
        // color theme one
        findViewById(R.id.note_theme_one).setOnClickListener(v -> {
            selectedFilter = 'C';
            selectedFilterResource = "#fffee7ab";
            requestSetSearchAction();
        });

        // color theme two
        findViewById(R.id.note_theme_two).setOnClickListener(v -> {
            selectedFilter = 'C';
            selectedFilterResource = "#ffffdbc3";
            requestSetSearchAction();
        });

        // color theme three
        findViewById(R.id.note_theme_three).setOnClickListener(v -> {
            selectedFilter = 'C';
            selectedFilterResource = "#ffffc5d1";
            requestSetSearchAction();
        });

        // color theme four
        findViewById(R.id.note_theme_four).setOnClickListener(v -> {
            selectedFilter = 'C';
            selectedFilterResource = "#ffe7d0f9";
            requestSetSearchAction();
        });

        // color theme five
        findViewById(R.id.note_theme_five).setOnClickListener(v -> {
            selectedFilter = 'C';
            selectedFilterResource = "#ffcdccfe";
            requestSetSearchAction();
        });

        // color theme six
        findViewById(R.id.note_theme_six).setOnClickListener(v -> {
            selectedFilter = 'C';
            selectedFilterResource = "#ffb5e9d3";
            requestSetSearchAction();
        });

        // color theme seven
        findViewById(R.id.note_theme_seven).setOnClickListener(v -> {
            selectedFilter = 'C';
            selectedFilterResource = "#ffb3e5fd";
            requestSetSearchAction();
        });

        // color theme eight
        findViewById(R.id.note_theme_eight).setOnClickListener(v -> {
            selectedFilter = 'C';
            selectedFilterResource = "#ffb5d8ff";
            requestSetSearchAction();
        });

        // color theme nine
        findViewById(R.id.note_theme_nine).setOnClickListener(v -> {
            selectedFilter = 'C';
            selectedFilterResource = "#ffe5e5e5";
            requestSetSearchAction();
        });

        // color theme ten
        findViewById(R.id.note_theme_ten).setOnClickListener(v -> {
            selectedFilter = 'C';
            selectedFilterResource = "#ffbcbcbc";
            requestSetSearchAction();
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    ActivityResultLauncher<Intent> startActivityForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result != null) {
            if (result.getResultCode() == RequestCodes.REQUEST_CODE_UPDATE_NOTE_OK) {
                notes.clear();
                notesAdapter.notifyDataSetChanged();
                request_search_notes(searchBar.getText().toString());
            } else {
                ArrayList<String> callback = Objects.requireNonNull(result.getData()).getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (callback != null) {
                    searchBar.setText(callback.get(0));
                }
            }
        }
    });

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.close_search).getVisibility() == View.VISIBLE) {
            request_close_search();
        } else {
            finish();
        }
    }
}