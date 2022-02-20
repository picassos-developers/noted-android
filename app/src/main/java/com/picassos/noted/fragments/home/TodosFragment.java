package com.picassos.noted.fragments.home;

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
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.picassos.noted.R;
import com.picassos.noted.activities.ViewTodoActivity;
import com.picassos.noted.adapters.TodosAdapter;
import com.picassos.noted.constants.RequestCodes;
import com.picassos.noted.databases.APP_DATABASE;
import com.picassos.noted.entities.Todo;
import com.picassos.noted.listeners.TodosListener;
import com.picassos.noted.models.SharedViewModel;
import com.picassos.noted.sheets.AddTodoBottomSheetModal;
import com.picassos.noted.sheets.TodoActionsBottomSheetModal;
import com.picassos.noted.sheets.TodoListsBottomSheetModal;
import com.picassos.noted.sheets.TodoMoreOptionsBottomSheetModal;
import com.picassos.noted.utils.Helper;

import java.util.ArrayList;
import java.util.List;

public class TodosFragment extends Fragment implements TodosListener {
    SharedViewModel sharedViewModel;

    // Bundle
    Bundle bundle;

    // View view
    View view;

    private List<Todo> todos;
    private TodosAdapter todosAdapter;

    boolean navigationHide = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_todos, container, false);

        // Bundle
        bundle = new Bundle();

        // add to-do
        CardView addTodo = view.findViewById(R.id.add_todo);
        addTodo.setOnClickListener(v -> {
            AddTodoBottomSheetModal addTodoBottomSheetModal = new AddTodoBottomSheetModal();
            addTodoBottomSheetModal.setArguments(bundle);
            addTodoBottomSheetModal.show(getChildFragmentManager(), "TAG");
        });

        // more options
        CardView moreOptions = view.findViewById(R.id.more_options);
        moreOptions.setOnClickListener(v -> {
            TodoMoreOptionsBottomSheetModal todoMoreOptionsBottomSheetModal = new TodoMoreOptionsBottomSheetModal();
            todoMoreOptionsBottomSheetModal.show(getChildFragmentManager(), "TAG");
        });

        // to-do lists
        CardView todoLists = view.findViewById(R.id.todo_lists);
        todoLists.setOnClickListener(v -> {
            TodoListsBottomSheetModal todoListsBottomSheetModal = new TodoListsBottomSheetModal();
            todoListsBottomSheetModal.show(getChildFragmentManager(), "TAG");
        });

        // to-dos recyclerview
        RecyclerView todosRecyclerview = view.findViewById(R.id.todos_recyclerview);
        todosRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));

        // to-dos list, adapter
        todos = new ArrayList<>();
        todosAdapter = new TodosAdapter(todos, this);
        todosRecyclerview.setAdapter(todosAdapter);

        requestTodos("todo_id");

        // nested scrollview
        NestedScrollView nestedScrollView = view.findViewById(R.id.nested_scrollview);
        nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY >= oldScrollY) { // down
                if (navigationHide) return;
                Helper.hide_bottom(moreOptions);
                Helper.hide_bottom(todoLists);
                navigationHide = true;
            } else {
                if (!navigationHide) return;
                Helper.show_bottom(moreOptions);
                Helper.show_bottom(todoLists);
                navigationHide = false;
            }
        });

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        sharedViewModel.getRequestCode().observe(requireActivity(), item -> {
            switch (item) {
                case RequestCodes.REQUEST_ACTION_TODO_CODE:
                case RequestCodes.REQUEST_DELETE_ALL_COMPLETED_TASKS_CODE:
                case RequestCodes.CHOOSE_SORT_BY_DEFAULT:
                    refreshTodos("todo_id");
                    break;
                case RequestCodes.CHOOSE_SORT_BY_A_TO_Z:
                    refreshTodos("a_z");
                    break;
                case RequestCodes.CHOOSE_SORT_BY_Z_TO_A:
                    refreshTodos("z_a");
                    break;
            }
        });

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * request uncompleted to-dos from AsyncTask
     */
    private void requestTodos(String sortBy) {

        @SuppressLint("StaticFieldLeak")
        class GetTodosTask extends AsyncTask<Void, Void, List<Todo>> {

            @Override
            protected List<Todo> doInBackground(Void... voids) {
                return APP_DATABASE.requestDatabase(getContext()).dao().request_todos(sortBy);
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void onPostExecute(List<Todo> todos_inline) {
                super.onPostExecute(todos_inline);
                todos.addAll(todos_inline);
                todosAdapter.notifyDataSetChanged();
                if (todosAdapter.getItemCount() == 0) {
                    view.findViewById(R.id.todos_empty_placeholder).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.todos_empty_placeholder).setOnClickListener(v -> {
                        AddTodoBottomSheetModal addTodoBottomSheetModal = new AddTodoBottomSheetModal();
                        addTodoBottomSheetModal.setArguments(bundle);
                        addTodoBottomSheetModal.show(getChildFragmentManager(), "TAG");
                    });
                } else {
                    view.findViewById(R.id.todos_empty_placeholder).setVisibility(View.GONE);
                }
            }

        }
        new GetTodosTask().execute();
    }

    @Override
    public void onTodoClicked(Todo todo, int position) {
        Intent intent = new Intent(requireContext(), ViewTodoActivity.class);
        intent.putExtra("modifier", true);
        intent.putExtra("todo", todo);
        startActivityForResult.launch(intent);
    }

    @Override
    public void onTodoLongClicked(Todo todo, int position) {
        bundle.putSerializable("todo_data", todo);
        TodoActionsBottomSheetModal todoActionsBottomSheetModal = new TodoActionsBottomSheetModal();
        todoActionsBottomSheetModal.setArguments(bundle);
        todoActionsBottomSheetModal.show(getChildFragmentManager(), "TAG");
    }

    @Override
    public void onTodoStateCLicked(Todo todo, int position, boolean checked) {
        todo.setTodo_state(checked);
        requestUpdateTodo(todo);
    }

    private void requestUpdateTodo(Todo todo) {
        @SuppressLint("StaticFieldLeak")
        class UpdateTodoTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                APP_DATABASE.requestDatabase(getContext()).dao().request_insert_todo(todo);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
            }
        }

        new UpdateTodoTask().execute();
    }

    /**
     * refresh todos list
     */
    @SuppressLint("NotifyDataSetChanged")
    private void refreshTodos(String sortBy) {
        todos.clear();
        todosAdapter.notifyDataSetChanged();
        requestTodos(sortBy);
    }

    @SuppressLint("NotifyDataSetChanged")
    ActivityResultLauncher<Intent> startActivityForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result != null) {
            if (result.getResultCode() == RequestCodes.REQUEST_ACTION_TODO_CODE) {
                refreshTodos("todo_id");
            }
        }
    });
}