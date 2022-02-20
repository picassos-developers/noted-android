package com.picassos.noted.fragments;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.picassos.noted.R;
import com.picassos.noted.adapters.ArchiveNotesAdapter;
import com.picassos.noted.constants.RequestCodes;
import com.picassos.noted.databases.APP_DATABASE;
import com.picassos.noted.entities.ArchiveNote;
import com.picassos.noted.listeners.ArchiveNotesListener;
import com.picassos.noted.models.SharedViewModel;
import com.picassos.noted.sharedPreferences.SharedPref;
import com.picassos.noted.sheets.ArchiveNoteActionsBottomSheetModal;
import com.picassos.noted.sheets.ArchivedNoteViewBottomSheetModal;
import com.picassos.noted.sheets.PasswordBottomSheetModal;
import com.picassos.noted.utils.Toasto;

import java.util.ArrayList;
import java.util.List;

public class ArchiveFragment extends Fragment implements ArchiveNotesListener {
    SharedViewModel sharedViewModel;

    // Bundle
    Bundle bundle;

    // View view
    View view;

    SharedPref sharedPref;

    private List<ArchiveNote> archive_notes;
    private ArchiveNotesAdapter archive_notes_adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_archive, container, false);

        // initialize bundle
        bundle = new Bundle();

        // notes recyclerview
        RecyclerView archive_notes_recyclerview = view.findViewById(R.id.notes_recyclerview);
        archive_notes_recyclerview.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));

        // notes list, adapter
        archive_notes = new ArrayList<>();
        archive_notes_adapter = new ArchiveNotesAdapter(archive_notes, this);
        archive_notes_recyclerview.setAdapter(archive_notes_adapter);

        request_archived_notes();

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        sharedViewModel.getRequestCode().observe(requireActivity(), item -> {
            if (item == RequestCodes.REQUEST_UNARCHIVE_NOTE) {
                request_archived_notes();
                Toasto.show_toast(requireContext(), getString(R.string.note_restored_from_archive), 1, 0);
            }
        });
        sharedViewModel.getData().observe(requireActivity(), item -> {
            ArchivedNoteViewBottomSheetModal archivedNoteViewBottomSheetModal = new ArchivedNoteViewBottomSheetModal();
            archivedNoteViewBottomSheetModal.setArguments(bundle);
            archivedNoteViewBottomSheetModal.show(getChildFragmentManager(), "TAG");
        });

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        sharedPref = new SharedPref(requireContext());
        super.onCreate(savedInstanceState);
    }

    /**
     * request archive notes
     */
    @SuppressLint("NotifyDataSetChanged")
    private void request_archived_notes() {
        archive_notes.clear();
        archive_notes_adapter.notifyDataSetChanged();

        @SuppressLint("StaticFieldLeak")
        class GetArchiveNotesTask extends AsyncTask<Void, Void, List<ArchiveNote>> {

            @Override
            protected List<ArchiveNote> doInBackground(Void... voids) {
                return APP_DATABASE.requestDatabase(getContext()).dao().request_archive_notes();
            }

            @Override
            protected void onPostExecute(List<ArchiveNote> archive_notes_inline) {
                super.onPostExecute(archive_notes_inline);
                archive_notes.addAll(archive_notes_inline);
                archive_notes_adapter.notifyDataSetChanged();

                if (archive_notes_adapter.getItemCount() == 0) {
                    view.findViewById(R.id.no_items).setVisibility(View.VISIBLE);
                } else {
                    view.findViewById(R.id.no_items).setVisibility(View.GONE);
                }
            }

        }
        new GetArchiveNotesTask().execute();
    }

    @Override
    public void onNoteClicked(ArchiveNote archive_note, int position) {
        bundle.putSerializable("archive_note_data", archive_note);

        if (sharedPref.loadNotePinCode() == 0) {
            ArchivedNoteViewBottomSheetModal archivedNoteViewBottomSheetModal = new ArchivedNoteViewBottomSheetModal();
            archivedNoteViewBottomSheetModal.setArguments(bundle);
            archivedNoteViewBottomSheetModal.show(getChildFragmentManager(), "TAG");
        } else {
            if (archive_note.isNote_locked()) {
                PasswordBottomSheetModal passwordBottomSheetModal = new PasswordBottomSheetModal();
                passwordBottomSheetModal.show(getChildFragmentManager(), "TAG");
            } else {
                ArchivedNoteViewBottomSheetModal archivedNoteViewBottomSheetModal = new ArchivedNoteViewBottomSheetModal();
                archivedNoteViewBottomSheetModal.setArguments(bundle);
                archivedNoteViewBottomSheetModal.show(getChildFragmentManager(), "TAG");
            }
        }
    }

    @Override
    public void onNoteLongClicked(ArchiveNote archive_note, int position) {
        bundle.putSerializable("archive_note_data", archive_note);

        ArchiveNoteActionsBottomSheetModal archiveNoteActionsBottomSheetModal = new ArchiveNoteActionsBottomSheetModal();
        archiveNoteActionsBottomSheetModal.setArguments(bundle);
        archiveNoteActionsBottomSheetModal.show(getChildFragmentManager(), "TAG");
    }
}