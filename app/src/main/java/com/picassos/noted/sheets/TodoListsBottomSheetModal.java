package com.picassos.noted.sheets;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.picassos.noted.R;
import com.picassos.noted.activities.TodoCreateListActivity;
import com.picassos.noted.activities.TodoListActivity;
import com.picassos.noted.adapters.TodoListAdapter;
import com.picassos.noted.constants.RequestCodes;
import com.picassos.noted.databases.APP_DATABASE;
import com.picassos.noted.entities.TodosList;
import com.picassos.noted.listeners.TodoListListener;
import com.picassos.noted.models.SharedViewModel;

import java.util.ArrayList;
import java.util.List;

public class TodoListsBottomSheetModal extends BottomSheetDialogFragment implements TodoListListener {
    SharedViewModel sharedViewModel;

    private List<TodosList> todosLists;
    private TodoListAdapter todoListAdapter;

    public TodoListsBottomSheetModal() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.todo_lists_bottom_sheet_modal, container, false);

        // to-dos recyclerview
        RecyclerView todosRecyclerview = view.findViewById(R.id.todo_list_recyclerview);
        todosRecyclerview.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        // to-dos list, adapter
        todosLists = new ArrayList<>();
        todoListAdapter = new TodoListAdapter(todosLists, this);
        todosRecyclerview.setAdapter(todoListAdapter);

        requestTodoList();

        // create a new list
        view.findViewById(R.id.create_list).setOnClickListener(v -> startActivityForResult.launch(new Intent(requireContext(), TodoCreateListActivity.class)));

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
     * request to-dos list from AsyncTask
     */
    private void requestTodoList() {

        @SuppressLint("StaticFieldLeak")
        class GetTodoListsTask extends AsyncTask<Void, Void, List<TodosList>> {

            @Override
            protected List<TodosList> doInBackground(Void... voids) {
                return APP_DATABASE.requestDatabase(getContext()).dao().request_todos_lists();
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void onPostExecute(List<TodosList> todo_list_inline) {
                super.onPostExecute(todo_list_inline);
                todosLists.addAll(todo_list_inline);
                todoListAdapter.notifyDataSetChanged();
            }

        }
        new GetTodoListsTask().execute();
    }

    @Override
    public void onTodoListClicked(TodosList todosList, int position) {
        Intent intent = new Intent(requireContext(), TodoListActivity.class);
        intent.putExtra("todo_list", todosList);
        startActivityForResult.launch(intent);
    }

    @SuppressLint("NotifyDataSetChanged")
    ActivityResultLauncher<Intent> startActivityForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result != null) {
            switch (result.getResultCode()) {
                case RequestCodes.REQUEST_CREATE_LIST_CODE:
                    todosLists.clear();
                    todoListAdapter.notifyDataSetChanged();
                    requestTodoList();
                    break;
                case RequestCodes.REQUEST_UPDATE_LIST_CODE:
                    sharedViewModel.setRequestCode(RequestCodes.REQUEST_ACTION_TODO_CODE);
                    dismiss();
                    break;
            }
        }
    });
}