package com.picassos.noted.sheets;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.picassos.noted.R;
import com.picassos.noted.constants.RequestCodes;
import com.picassos.noted.databases.APP_DATABASE;
import com.picassos.noted.entities.Note;
import com.picassos.noted.entities.TrashNote;
import com.picassos.noted.models.SharedViewModel;

public class TrashNoteActionsBottomSheetModal extends BottomSheetDialogFragment {
    SharedViewModel sharedViewModel;

    TrashNote trashNote;

    public TrashNoteActionsBottomSheetModal() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.trash_note_actions_bottom_sheet_modal, container, false);

        trashNote = (TrashNote) requireArguments().getSerializable("trash_note_data");

        // restore from trash
        Button restoreNote = view.findViewById(R.id.restore_note);
        restoreNote.setOnClickListener(v -> requestPresetTrashNote());

        // delete note
        Button deleteNote = view.findViewById(R.id.delete_note);
        deleteNote.setOnClickListener(v -> requestDeleteTrashNote(trashNote, 1));

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
    }

    /**
     * request save data from trash
     * note class to note class
     */
    private void requestPresetTrashNote() {
        if (trashNote != null) {
            final Note preset_note = new Note();
            preset_note.setNote_category_id(trashNote.getNote_id());
            preset_note.setNote_title(trashNote.getNote_title());
            preset_note.setNote_created_at(trashNote.getNote_created_at());
            preset_note.setNote_subtitle(trashNote.getNote_subtitle());
            preset_note.setNote_description(trashNote.getNote_description());
            preset_note.setNote_image_path(trashNote.getNote_image_path());
            preset_note.setNote_color(trashNote.getNote_color());
            preset_note.setNote_web_link(trashNote.getNote_web_link());
            preset_note.setNote_category_id(trashNote.getNote_category_id());
            preset_note.setNote_reminder(trashNote.getNote_reminder());
            preset_note.setNote_locked(trashNote.isNote_locked());

            requestRestoreNote(preset_note);
        } else {
            sendResult(RequestCodes.REQUEST_DISCARD_NOTE_CODE);
        }
    }

    /** request move the preset trash
     * note to notes before delete
     * @param note for class
     */
    private void requestRestoreNote(Note note) {
        if (trashNote != null) {
            @SuppressLint("StaticFieldLeak")
            class MoveTrashNoteTask extends AsyncTask<Void, Void, Void> {
                @Override
                protected Void doInBackground(Void... voids) {
                    APP_DATABASE.requestDatabase(getContext()).dao().request_insert_note(note);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    requestDeleteTrashNote(trashNote, 0);
                    dismiss();
                }
            }

            new MoveTrashNoteTask().execute();
        }
    }

    /**
     * request to delete a preset trash note
     * @param trash_note for class
     */
    private void requestDeleteTrashNote(TrashNote trash_note, int is_permanent) {
        if (trash_note != null) {
            @SuppressLint("StaticFieldLeak")
            class DeleteTrashNoteTask extends AsyncTask<Void, Void, Void> {
                @Override
                protected Void doInBackground(Void... voids) {
                    APP_DATABASE.requestDatabase(getContext()).dao().request_delete_trash_note(trash_note);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    if (is_permanent == 1) {
                        sendResult(RequestCodes.REQUEST_DELETE_NOTE_PERMANENTLY_CODE);
                    } else {
                        sendResult(RequestCodes.REQUEST_RESTORE_NOTE_CODE);
                    }
                }
            }

            new DeleteTrashNoteTask().execute();
        } else {
            sendResult(RequestCodes.REQUEST_DISCARD_NOTE_CODE);
        }
    }

    private void sendResult(int REQUEST_CODE) {
        sharedViewModel.setRequestCode(REQUEST_CODE);
        dismiss();
    }
}
