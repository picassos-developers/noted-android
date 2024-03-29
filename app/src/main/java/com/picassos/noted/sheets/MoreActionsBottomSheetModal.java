package com.picassos.noted.sheets;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.picassos.noted.R;
import com.picassos.noted.activities.CreatePinActivity;
import com.picassos.noted.constants.RequestCodes;
import com.picassos.noted.databases.APP_DATABASE;
import com.picassos.noted.entities.ArchiveNote;
import com.picassos.noted.entities.Note;
import com.picassos.noted.entities.TrashNote;
import com.picassos.noted.sharedPreferences.SharedPref;
import com.picassos.noted.utils.Toasto;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class MoreActionsBottomSheetModal extends BottomSheetDialogFragment {

    Bundle bundle;

    SharedPref sharedPref;

    public interface OnDeleteListener {
        void onDeleteListener(int requestCode);
    }

    public interface OnLockListener {
        void onLockListener(int requestCode);
    }

    public interface OnSpeechInputListener {
        void onSpeechInputListener(String text);
    }

    public interface OnArchiveListener {
        void onArchiveListener(int requestCode);
    }

    private Note note;

    OnDeleteListener onDeleteListener;
    OnLockListener onLockListener;
    OnSpeechInputListener onSpeechInputListener;
    OnArchiveListener onArchiveListener;

    public MoreActionsBottomSheetModal() {

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            onDeleteListener = (OnDeleteListener) context;
        } catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement onDeleteListener");
        }

        try {
            onLockListener = (OnLockListener) context;
        } catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement onLockListener");
        }

        try {
            onSpeechInputListener = (OnSpeechInputListener) context;
        } catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement onSpeechInputListener");
        }

        try {
            onArchiveListener = (OnArchiveListener) context;
        } catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement onArchiveListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.more_actions_bottom_sheet_modal, container, false);

        note = (Note) requireArguments().getSerializable("note_data");

        // delete note
        view.findViewById(R.id.delete_note).setOnClickListener(v -> requestPresetTrashNote());

        // lock note
        view.findViewById(R.id.lock_note).setOnClickListener(v -> {
            if (sharedPref.loadNotePinCode() == 0) {
                // no password pin set
                Intent intent = new Intent(requireContext(), CreatePinActivity.class);
                startActivityForResult.launch(intent);
            } else {
                if (note != null) {
                    if (note.isNote_locked()) {
                        // set password and lock note
                        Toasto.show_toast(requireContext(), getString(R.string.note_unlocked), 0, 0);
                        onLockListener.onLockListener(RequestCodes.REQUEST_UNLOCK_NOTE_CODE);
                    } else {
                        // set password and lock note
                        Toasto.show_toast(requireContext(), getString(R.string.note_locked), 0, 0);
                        onLockListener.onLockListener(RequestCodes.REQUEST_LOCK_NOTE_CODE);
                    }
                    dismiss();
                } else {
                    Toasto.show_toast(requireContext(), getString(R.string.save_note_before_lock), 1, 2);
                }
            }
        });
        TextView lockNoteText = view.findViewById(R.id.lock_note_text);
        if (note != null) {
            if (note.isNote_locked()) {
                lockNoteText.setText(getString(R.string.unlock));
            } else {
                lockNoteText.setText(getString(R.string.note_pin_code));
            }
        } else {
            lockNoteText.setText(getString(R.string.note_pin_code));
        }

        // share note
        view.findViewById(R.id.share_note).setOnClickListener(v -> {
            if (note != null) {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, note.getNote_description());
                startActivity(Intent.createChooser(share, getString(R.string.share_note)));
            } else {
                Toasto.show_toast(requireContext(), getString(R.string.save_note_before_share), 1, 2);
            }
        });

        // archive note
        view.findViewById(R.id.archive_note).setOnClickListener(v -> {
            if (note != null) {
                requestArchiveNote();
            } else {
                Toasto.show_toast(requireContext(), getString(R.string.save_note_before_archive), 1, 2);
            }
        });

        // reader mode
        view.findViewById(R.id.reader_mode).setOnClickListener(v -> {
            if (note != null) {
                showReaderModeDialog();
            } else {
                Toasto.show_toast(requireContext(), getString(R.string.please_save_your_note_first), 1, 2);
            }
        });

        // copy note to clipboard
        view.findViewById(R.id.copy_to_clipboard).setOnClickListener(v -> {
            if (note != null) {
                // get note details
                String note_title = note.getNote_title();
                String note_subtitle = note.getNote_subtitle();
                String note_description = note.getNote_description();
                // format note
                String note_text = "Note title: " + note_title + "\n\nNote subtitle: " + note_subtitle + "\n\nNote description: " + note_description;
                // copy to clipboard
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData data = ClipData.newPlainText("Copy Note", note_text);
                clipboard.setPrimaryClip(data);
                Toasto.show_toast(requireContext(), getString(R.string.copied_to_clipboard), 0, 0);
                // dismiss bottom sheet
                dismiss();
            } else {
                Toasto.show_toast(requireContext(), getString(R.string.save_note_before_copy), 1, 2);
            }
        });

        // export note as
        view.findViewById(R.id.export_note_as).setOnClickListener(v -> {
            if (note != null) {
                showExportAsDialog();
            } else {
                Toasto.show_toast(requireContext(), getString(R.string.please_save_your_note_first), 1, 2);
            }
        });

        // speech to text
        view.findViewById(R.id.speech_to_text).setOnClickListener(v -> {
            if (note != null) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_notes));

                try {
                    startActivityForResult.launch(intent);
                } catch (Exception e) {
                    Toasto.show_toast(requireContext(), e.getMessage(), 1, 0);
                }
            } else {
                Toasto.show_toast(requireContext(), getString(R.string.please_save_your_note_first), 1, 2);
            }
        });

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        sharedPref = new SharedPref(requireContext());
        super.onCreate(savedInstanceState);

        bundle = new Bundle();
    }

    private void requestArchiveNote() {
        if (note != null) {
            final ArchiveNote presetArchiveNote = new ArchiveNote();
            presetArchiveNote.setNote_id(note.getNote_id());
            presetArchiveNote.setNote_title(note.getNote_title());
            presetArchiveNote.setNote_created_at(note.getNote_created_at());
            presetArchiveNote.setNote_subtitle(note.getNote_subtitle());
            presetArchiveNote.setNote_description(note.getNote_description());
            presetArchiveNote.setNote_image_path(note.getNote_image_path());
            presetArchiveNote.setNote_image_uri(note.getNote_image_uri());
            presetArchiveNote.setNote_video_path(note.getNote_video_path());
            presetArchiveNote.setNote_color(note.getNote_color());
            presetArchiveNote.setNote_web_link(note.getNote_web_link());
            presetArchiveNote.setNote_category_id(note.getNote_category_id());
            presetArchiveNote.setNote_locked(note.isNote_locked());

            @SuppressLint("StaticFieldLeak")
            class SaveArchiveNoteTask extends AsyncTask<Void, Void, Void> {

                @Override
                protected Void doInBackground(Void... voids) {
                    APP_DATABASE.requestDatabase(requireContext()).dao().request_insert_archive_note(presetArchiveNote);
                    APP_DATABASE.requestDatabase(requireContext()).dao().request_delete_note(note);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    onArchiveListener.onArchiveListener(RequestCodes.REQUEST_ARCHIVE_NOTE_CODE);
                    dismiss();
                }
            }

            new SaveArchiveNoteTask().execute();
        }
    }

    /**
     * request save data from note class
     * to trash note class
     */
    private void requestPresetTrashNote() {
        if (note != null) {
            final TrashNote presetTrashNote = new TrashNote();
            presetTrashNote.setNote_id(note.getNote_id());
            presetTrashNote.setNote_title(note.getNote_title());
            presetTrashNote.setNote_created_at(note.getNote_created_at());
            presetTrashNote.setNote_subtitle(note.getNote_subtitle());
            presetTrashNote.setNote_description(note.getNote_description());
            presetTrashNote.setNote_image_path(note.getNote_image_path());
            presetTrashNote.setNote_color(note.getNote_color());
            presetTrashNote.setNote_web_link(note.getNote_web_link());
            presetTrashNote.setNote_category_id(note.getNote_category_id());
            presetTrashNote.setNote_reminder(note.getNote_reminder());
            presetTrashNote.setNote_locked(note.isNote_locked());

            requestMoveNoteToTrash(presetTrashNote);
        } else {
            onDeleteListener.onDeleteListener(RequestCodes.REQUEST_DISCARD_NOTE_CODE);
            Toasto.show_toast(requireContext(), getString(R.string.note_discarded), 1, 0);
            dismiss();
        }
    }

    /** request move the preset note
     * to trash before delete
     * @param trash_note for class
     */
    private void requestMoveNoteToTrash(TrashNote trash_note) {
        if (note != null) {
            @SuppressLint("StaticFieldLeak")
            class MoveNoteTask extends AsyncTask<Void, Void, Void> {
                @Override
                protected Void doInBackground(Void... voids) {
                    APP_DATABASE.requestDatabase(getContext()).dao().request_insert_trash_note(trash_note);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    requestDeleteNote(note);
                    dismiss();
                }
            }

            new MoveNoteTask().execute();
        }
    }

    /**
     * request to delete a preset note
     * @param note for class
     */
    private void requestDeleteNote(Note note) {
        if (note != null) {
            @SuppressLint("StaticFieldLeak")
            class DeleteNoteTask extends AsyncTask<Void, Void, Void> {
                @Override
                protected Void doInBackground(Void... voids) {
                    APP_DATABASE.requestDatabase(requireContext()).dao().request_delete_note(note);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    onDeleteListener.onDeleteListener(RequestCodes.REQUEST_DELETE_NOTE_CODE);
                    dismiss();
                }
            }
            new DeleteNoteTask().execute();
        } else {
            onDeleteListener.onDeleteListener(RequestCodes.REQUEST_DISCARD_NOTE_CODE);
            dismiss();
        }
    }

    /**
     * show reader mode dialog
     */
    private void showReaderModeDialog() {
        Dialog readerModeDialog = new Dialog(requireContext());

        readerModeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        readerModeDialog.setContentView(R.layout.reader_mode_dialog);

        // enable dialog cancel
        readerModeDialog.setCancelable(true);
        readerModeDialog.setOnCancelListener(dialog -> readerModeDialog.dismiss());

        // note description
        TextView note_description = readerModeDialog.findViewById(R.id.note_description);
        note_description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        note_description.setText(note.getNote_description());

        // text minus
        CardView minus = readerModeDialog.findViewById(R.id.minus);
        minus.setOnClickListener(v -> {
            float px = note_description.getTextSize();
            float sp = px / getResources().getDisplayMetrics().scaledDensity;
            if (sp > 12) {
                note_description.setTextSize(TypedValue.COMPLEX_UNIT_SP, (sp - 2));
            }
        });
        // text plus
        CardView plus = readerModeDialog.findViewById(R.id.plus);
        plus.setOnClickListener(v -> {
            float px = note_description.getTextSize();
            float sp = px / getResources().getDisplayMetrics().scaledDensity;
            if (sp < 25) {
                note_description.setTextSize(TypedValue.COMPLEX_UNIT_SP, (sp + 2));
            }
        });
        // close reader mode
        readerModeDialog.findViewById(R.id.close_reader_mode).setOnClickListener(v -> readerModeDialog.dismiss());

        if (readerModeDialog.getWindow() != null) {
            readerModeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            readerModeDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }

        readerModeDialog.show();
    }

    /**
     * show export note as dialog
     */
    private void showExportAsDialog() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }

        Dialog exportNoteDialog = new Dialog(requireContext());

        exportNoteDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        exportNoteDialog.setContentView(R.layout.export_note_as_dialog);

        // enable dialog cancel
        exportNoteDialog.setCancelable(true);
        exportNoteDialog.setOnCancelListener(dialog -> exportNoteDialog.dismiss());

        // export as txt
        exportNoteDialog.findViewById(R.id.export_as_txt).setOnClickListener(v -> {
            requestExportFile("txt");
            exportNoteDialog.dismiss();
        });

        // export as csv
        exportNoteDialog.findViewById(R.id.export_as_csv).setOnClickListener(v -> {
            requestExportFile("csv");
            exportNoteDialog.dismiss();
        });

        // cancel
        exportNoteDialog.findViewById(R.id.confirm_deny).setOnClickListener(v -> exportNoteDialog.dismiss());

        if (exportNoteDialog.getWindow() != null) {
            exportNoteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            exportNoteDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }

        exportNoteDialog.show();
    }

    /**
     * request export note as file
     * @param format for format
     */
    private void requestExportFile(String format) {
        String FILE_NAME = note.getNote_title().replaceAll(" ", "_").toLowerCase() + "." + format;
        // format note
        String note_text = "Note title: " + note.getNote_title() + "\n\nNote subtitle: " + note.getNote_subtitle() + "\n\nNote description: " + note.getNote_description();
        // create directory
        File directory = new File(Environment.getExternalStorageDirectory(), "test");
        if (directory.exists()) {
            Toasto.show_toast(requireContext(), "already exists", 1, 0);
        } else {
            requireContext().getExternalFilesDir("test");
        }


        /*// save file
        File file = new File(Environment.getExternalStorageDirectory() + "/" + requireContext().getPackageName() + "/",  FILE_NAME);
        // write to file
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(note_text.getBytes());
            fos.close();
            Toasto.show_toast(requireContext(), getString(R.string.saved_to) + Environment.getExternalStorageDirectory() + "/" + requireContext().getPackageName() + "/" + FILE_NAME, 1, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    @SuppressLint("NotifyDataSetChanged")
    ActivityResultLauncher<Intent> startActivityForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result != null) {
            if (result.getResultCode() == RequestCodes.REQUEST_SET_PIN_CODE) {
                onLockListener.onLockListener(RequestCodes.REQUEST_LOCK_NOTE_CODE);
            } else {
                ArrayList<String> callback = Objects.requireNonNull(result.getData()).getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (callback != null) {
                    onSpeechInputListener.onSpeechInputListener(callback.get(0));
                    dismiss();
                }
            }
        }
    });

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1000) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toasto.show_toast(requireContext(), getString(R.string.permission_granted), 1, 0);
            } else {
                Toasto.show_toast(requireContext(), getString(R.string.permission_denied), 1, 1);
            }
        }
    }
}
