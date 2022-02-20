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
import com.picassos.noted.entities.Todo;
import com.picassos.noted.models.SharedViewModel;

public class TodoActionsBottomSheetModal extends BottomSheetDialogFragment {
    SharedViewModel sharedViewModel;

    private Todo todo;

    public TodoActionsBottomSheetModal() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.todo_actions_bottom_sheet_modal, container, false);

        todo = (Todo) requireArguments().getSerializable("todo_data");

        // delete to-do
        view.findViewById(R.id.todo_delete).setOnClickListener(v -> requestDeleteNote(todo));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * request to delete a preset to-do
     * @param todo for class
     */
    private void requestDeleteNote(Todo todo) {
        if (todo != null) {
            @SuppressLint("StaticFieldLeak")
            class DeleteNoteTask extends AsyncTask<Void, Void, Void> {
                @Override
                protected Void doInBackground(Void... voids) {
                    APP_DATABASE.requestDatabase(getContext()).dao().request_delete_todo(todo);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    sharedViewModel.setRequestCode(RequestCodes.REQUEST_ACTION_TODO_CODE);
                    dismiss();
                }
            }

            new DeleteNoteTask().execute();
        }
    }
}
