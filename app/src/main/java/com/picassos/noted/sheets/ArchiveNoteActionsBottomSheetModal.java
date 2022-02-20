package com.picassos.noted.sheets;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.picassos.noted.R;
import com.picassos.noted.constants.RequestCodes;
import com.picassos.noted.databases.APP_DATABASE;
import com.picassos.noted.entities.ArchiveNote;
import com.picassos.noted.entities.Note;
import com.picassos.noted.models.SharedViewModel;

public class ArchiveNoteActionsBottomSheetModal extends BottomSheetDialogFragment {
    SharedViewModel sharedViewModel;

    private ArchiveNote archiveNote;

    public ArchiveNoteActionsBottomSheetModal() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.archive_note_actions_bottom_sheet_modal, container, false);

        archiveNote = (ArchiveNote) requireArguments().getSerializable("archive_note_data");

        // unarchive
        view.findViewById(R.id.unarchive).setOnClickListener(v -> requestDeleteArchiveNote(archiveNote));

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
     * request to delete a preset note
     * @param archive_note for class
     */
    private void requestDeleteArchiveNote(ArchiveNote archive_note) {
        if (archive_note != null) {
            @SuppressLint("StaticFieldLeak")
            class DeleteNoteTask extends AsyncTask<Void, Void, Void> {
                @Override
                protected Void doInBackground(Void... voids) {
                    final Note presetNote = new Note();
                    presetNote.setNote_id(archive_note.getNote_id());
                    presetNote.setNote_title(archive_note.getNote_title());
                    presetNote.setNote_created_at(archive_note.getNote_created_at());
                    presetNote.setNote_subtitle(archive_note.getNote_subtitle());
                    presetNote.setNote_description(archive_note.getNote_description());
                    presetNote.setNote_image_path(archive_note.getNote_image_path());
                    presetNote.setNote_image_uri(archive_note.getNote_image_uri());
                    presetNote.setNote_video_path(archive_note.getNote_video_path());
                    presetNote.setNote_color(archive_note.getNote_color());
                    presetNote.setNote_web_link(archive_note.getNote_web_link());
                    presetNote.setNote_category_id(archive_note.getNote_category_id());
                    presetNote.setNote_locked(archive_note.isNote_locked());

                    APP_DATABASE.requestDatabase(requireContext()).dao().request_insert_note(presetNote);
                    APP_DATABASE.requestDatabase(requireContext()).dao().request_delete_archive_note(archive_note);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    sharedViewModel.setRequestCode(RequestCodes.REQUEST_UNARCHIVE_NOTE);
                    dismiss();
                }
            }

            new DeleteNoteTask().execute();
        }
    }
}
