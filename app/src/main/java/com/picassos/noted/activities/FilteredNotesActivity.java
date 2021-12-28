package com.picassos.noted.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.picassos.noted.R;
import com.picassos.noted.adapters.NotesTypeListAdapter;
import com.picassos.noted.constants.RequestCodes;
import com.picassos.noted.databases.APP_DATABASE;
import com.picassos.noted.entities.Note;
import com.picassos.noted.listeners.NotesListener;
import com.picassos.noted.sharedPreferences.SharedPref;
import com.picassos.noted.sheets.ImplementedPasswordBottomSheetModal;
import com.picassos.noted.utils.Helper;
import com.picassos.noted.utils.Toasto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FilteredNotesActivity extends AppCompatActivity implements NotesListener, ImplementedPasswordBottomSheetModal.OnUnlockListener {

    Bundle bundle;
    SharedPref sharedPref;

    private RecyclerView notesRecyclerview;

    private List<Note> notes;
    private NotesTypeListAdapter notesAdapter;

    private int noteClickedPosition = -1;

    private EditText searchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = new SharedPref(this);

        // OPTIONS
        Helper.dark_mode(this);
        Helper.fullscreen_mode(this);
        Helper.screen_state(this);

        setContentView(R.layout.activity_filtered_notes);

        // Bundle
        bundle = new Bundle();

        // finish activity
        findViewById(R.id.go_back).setOnClickListener(v -> finish());

        // category title
        TextView categoryTitle = findViewById(R.id.category_title);
        categoryTitle.setText(getIntent().getStringExtra("title"));

        // search bar
        searchBar = findViewById(R.id.search_bar);
        searchBar.addTextChangedListener(searchTextWatcher);

        // search mic (search with voice)
        ImageView searchMic = findViewById(R.id.search_mic);
        searchMic.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra("request", RequestCodes.REQUEST_CODE_TEXT_TO_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_notes));

            try {
                startActivityForResult.launch(intent);
            } catch (Exception e) {
                Toasto.show_toast(this, e.getMessage(), 1, 1);
            }
        });

        // notes recyclerview
        notesRecyclerview = findViewById(R.id.notes_recyclerview);
        notesRecyclerview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        // notes list, adapter
        notes = new ArrayList<>();
        notesAdapter = new NotesTypeListAdapter(notes, this);
        notesRecyclerview.setAdapter(notesAdapter);

        requestNotes(RequestCodes.REQUEST_CODE_VIEW_NOTE_OK, false, getIntent().getIntExtra("identifier", 0));
    }

    /**
     * request notes from AsyncTask
     * @param identifier for category id
     */
    private void requestNotes(final int requestCode, final boolean isDeleted, int identifier) {

        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>> {

            @Override
            protected List<Note> doInBackground(Void... voids) {
                return APP_DATABASE.requestDatabase(getApplicationContext()).dao().request_notes_by_category(identifier);
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void onPostExecute(List<Note> notes_inline) {
                super.onPostExecute(notes_inline);
                if (requestCode == RequestCodes.REQUEST_CODE_VIEW_NOTE_OK) {
                    notes.addAll(notes_inline);
                    notesAdapter.notifyDataSetChanged();
                } else if (requestCode == RequestCodes.REQUEST_CODE_ADD_NOTE_OK) {
                    notes.add(0, notes_inline.get(0));
                    notesAdapter.notifyItemInserted(0);
                    notesRecyclerview.smoothScrollToPosition(0);
                } else if (requestCode == RequestCodes.REQUEST_CODE_UPDATE_NOTE_OK) {
                    notes.remove(noteClickedPosition);
                    if (isDeleted) {
                        notesAdapter.notifyItemRemoved(noteClickedPosition);
                    } else {
                        notes.add(noteClickedPosition, notes_inline.get(noteClickedPosition));
                        notesAdapter.notifyItemChanged(noteClickedPosition);
                    }
                }

                if (notesAdapter.getItemCount() == 0) {
                    findViewById(R.id.no_items).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.no_items).setVisibility(View.GONE);
                }
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

        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            notesAdapter.requestCancelTimer();
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (notes.size() != 0) {
                notesAdapter.requestSearchNotes(s.toString());
            }
        }
    };

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;

        if (sharedPref.loadNotePinCode() == 0) {
            Intent intent = new Intent(FilteredNotesActivity.this, AddNoteActivity.class);
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
                Intent intent = new Intent(FilteredNotesActivity.this, AddNoteActivity.class);
                intent.putExtra("modifier", true);
                intent.putExtra("note", note);
                startActivityForResult.launch(intent);
            }
        }
    }

    @Override
    public void onNoteLongClicked(Note note, int position) {
        noteClickedPosition = position;
    }

    @Override
    public void onUnlockListener(Note note) {
        Intent intent = new Intent(FilteredNotesActivity.this, AddNoteActivity.class);
        intent.putExtra("modifier", true);
        intent.putExtra("note", note);
        startActivityForResult.launch(intent);
    }

    ActivityResultLauncher<Intent> startActivityForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result != null && result.getResultCode() == RESULT_OK) {
            if (result.getData() != null) {
                switch (result.getData().getIntExtra("request", 0)) {
                    case RequestCodes.REQUEST_CODE_TEXT_TO_SPEECH:
                        ArrayList<String> callback = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (callback != null) {
                            searchBar.setText(callback.get(0));
                        }
                        break;
                    case RequestCodes.REQUEST_CODE_ADD_NOTE_OK:
                        requestNotes(RequestCodes.REQUEST_CODE_ADD_NOTE_OK, false, getIntent().getIntExtra("identifier", 0));
                        break;
                    case RequestCodes.REQUEST_CODE_UPDATE_NOTE_OK:
                        requestNotes(RequestCodes.REQUEST_CODE_UPDATE_NOTE_OK, result.getData().getBooleanExtra("is_note_removed", false), getIntent().getIntExtra("identifier", 0));
                        break;
                }
            }
        }
    });
}