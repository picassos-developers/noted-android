package com.picassos.noted.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.picassos.noted.R;
import com.picassos.noted.activities.MainActivity;
import com.picassos.noted.adapters.TrashNotesAdapter;
import com.picassos.noted.constants.RequestCodes;
import com.picassos.noted.databases.APP_DATABASE;
import com.picassos.noted.entities.TrashNote;
import com.picassos.noted.listeners.TrashNotesListener;
import com.picassos.noted.models.SharedViewModel;
import com.picassos.noted.sheets.TrashNoteActionsBottomSheetModal;
import com.picassos.noted.utils.Toasto;

import java.util.ArrayList;
import java.util.List;

public class TrashFragment extends Fragment implements TrashNotesListener {
    SharedViewModel sharedViewModel;

    // Bundle
    Bundle bundle;

    // View view
    View view;

    private List<TrashNote> trashNotes;
    private TrashNotesAdapter trashNotesAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_trash, container, false);

        // initialize bundle
        bundle = new Bundle();

        // notes recyclerview
        RecyclerView trashNotesRecyclerview = view.findViewById(R.id.notes_recyclerview);
        trashNotesRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));

        // notes list, adapter
        trashNotes = new ArrayList<>();
        trashNotesAdapter = new TrashNotesAdapter(trashNotes, this);
        trashNotesRecyclerview.setAdapter(trashNotesAdapter);

        requestTrashNotes();

        // delete all trash notes
        ((MainActivity) requireActivity()).extraAction.setOnClickListener(v -> {
            Dialog confirmDialog = new Dialog(requireContext());

            confirmDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            confirmDialog.setContentView(R.layout.popup_confirm);

            // enable dialog cancel
            confirmDialog.setCancelable(true);
            confirmDialog.setOnCancelListener(dialog -> confirmDialog.dismiss());

            // confirm header
            TextView confirmHeader = confirmDialog.findViewById(R.id.confirm_header);
            confirmHeader.setText(getString(R.string.delete_all_notes));

            // confirm text
            TextView confirmText = confirmDialog.findViewById(R.id.confirm_text);
            confirmText.setText(getString(R.string.delete_all_notes_description));

            // confirm allow
            TextView confirmAllow = confirmDialog.findViewById(R.id.confirm_allow);
            confirmAllow.setOnClickListener(v1 -> {
                requestDeleteAllTrashNotes();
                confirmDialog.dismiss();
            });

            // confirm cancel
            TextView confirmCancel = confirmDialog.findViewById(R.id.confirm_deny);
            confirmCancel.setOnClickListener(v2 -> confirmDialog.dismiss());

            if (confirmDialog.getWindow() != null) {
                confirmDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                confirmDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            }

            confirmDialog.show();
        });

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        sharedViewModel.getRequestCode().observe(requireActivity(), item -> {
            switch (item) {
                case RequestCodes.REQUEST_RESTORE_NOTE_CODE:
                    requestTrashNotes();
                    Toasto.show_toast(requireContext(), getString(R.string.note_restored), 1, 0);
                    break;
                case RequestCodes.REQUEST_DELETE_NOTE_PERMANENTLY_CODE:
                    requestTrashNotes();
                    Toasto.show_toast(requireContext(), getString(R.string.note_deleted_permanently), 1, 0);
                    break;
                case RequestCodes.REQUEST_DISCARD_NOTE_CODE:
                    Toasto.show_toast(requireContext(), getString(R.string.note_discarded), 1, 0);
                    break;
            }
        });

        return view;
    }

    /**
     * request trash notes
     */
    @SuppressLint("NotifyDataSetChanged")
    private void requestTrashNotes() {
        trashNotes.clear();
        trashNotesAdapter.notifyDataSetChanged();

        @SuppressLint("StaticFieldLeak")
        class GetTrashNotesTask extends AsyncTask<Void, Void, List<TrashNote>> {

            @Override
            protected List<TrashNote> doInBackground(Void... voids) {
                return APP_DATABASE.requestDatabase(requireContext()).dao().request_trash_notes();
            }

            @Override
            protected void onPostExecute(List<TrashNote> trash_notes_inline) {
                super.onPostExecute(trash_notes_inline);
                trashNotes.addAll(trash_notes_inline);
                trashNotesAdapter.notifyDataSetChanged();

                if (trashNotesAdapter.getItemCount() == 0) {
                    view.findViewById(R.id.no_items).setVisibility(View.VISIBLE);
                    ((MainActivity) requireActivity()).extraAction.setEnabled(false);
                } else {
                    view.findViewById(R.id.no_items).setVisibility(View.GONE);
                    ((MainActivity) requireActivity()).extraAction.setEnabled(true);
                }
            }

        }
        new GetTrashNotesTask().execute();
    }

    /**
     * request to delete all trash
     * notes records from database
     */
    private void requestDeleteAllTrashNotes() {
        @SuppressLint("StaticFieldLeak")
        class DeleteAllTrashNotesTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... voids) {
                APP_DATABASE.requestDatabase(requireContext()).dao().request_delete_all_trash_note();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                requestTrashNotes();
            }
        }

        new DeleteAllTrashNotesTask().execute();
    }

    @Override
    public void onNoteClicked(TrashNote trashNote, int position) {
        // on trash note click event listener
    }

    @Override
    public void onNoteLongClicked(TrashNote trashNote, int position) {
        bundle.putSerializable("trash_note_data", trashNote);

        TrashNoteActionsBottomSheetModal trashNoteActionsBottomSheetModal = new TrashNoteActionsBottomSheetModal();
        trashNoteActionsBottomSheetModal.setArguments(bundle);
        trashNoteActionsBottomSheetModal.show(getChildFragmentManager(), "TAG");
    }
}
