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
import android.view.View;
import android.widget.TextView;

import com.picassos.noted.R;
import com.picassos.noted.adapters.TodosAdapter;
import com.picassos.noted.constants.RequestCodes;
import com.picassos.noted.databases.APP_DATABASE;
import com.picassos.noted.entities.Todo;
import com.picassos.noted.entities.TodosList;
import com.picassos.noted.listeners.TodosListener;
import com.picassos.noted.utils.Helper;
import com.picassos.noted.utils.Toasto;

import java.util.ArrayList;
import java.util.List;

public class TodoListActivity extends AppCompatActivity implements TodosListener {

    // Bundle
    Bundle bundle;

    // to-do list
    private TodosList todosList;

    private List<Todo> todos;
    private TodosAdapter todosAdapter;

    // updated state
    boolean is_updated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OPTIONS
        Helper.dark_mode(this);
        Helper.fullscreen_mode(this);
        Helper.screen_state(this);

        setContentView(R.layout.activity_todo_list);

        // Bundle
        bundle = new Bundle();

        // set to-do list
        if (getIntent().getSerializableExtra("todo_list") != null) {
            todosList = (TodosList) getIntent().getSerializableExtra("todo_list");
        } else {
            Toasto.show_toast(this, getString(R.string.error_empty_list), 1, 2);
        }

        // return back and finish activity
        findViewById(R.id.go_back).setOnClickListener(v -> setResult());

        // list title
        TextView listTitle = findViewById(R.id.todo_list_title);
        listTitle.setText(todosList.getTodo_list_title());

        // to-dos recyclerview
        RecyclerView todosRecyclerview = findViewById(R.id.todos_recyclerview);
        todosRecyclerview.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false));

        // to-dos list, adapter
        todos = new ArrayList<>();
        todosAdapter = new TodosAdapter(todos, this);
        todosRecyclerview.setAdapter(todosAdapter);

        requestTodos(todosList);
    }

    /**
     * request uncompleted to-dos from AsyncTask
     */
    private void requestTodos(TodosList todosList) {

        @SuppressLint("StaticFieldLeak")
        class GetTodosTask extends AsyncTask<Void, Void, List<Todo>> {

            @Override
            protected List<Todo> doInBackground(Void... voids) {
                return APP_DATABASE.requestDatabase(getApplicationContext()).dao().request_todos_by_list(todosList.getTodo_list_identifier());
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void onPostExecute(List<Todo> todos_inline) {
                super.onPostExecute(todos_inline);
                todos.addAll(todos_inline);
                todosAdapter.notifyDataSetChanged();
                if (todosAdapter.getItemCount() == 0) {
                    findViewById(R.id.no_items).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.no_items).setVisibility(View.GONE);
                }
            }

        }
        new GetTodosTask().execute();
    }

    @Override
    public void onTodoClicked(Todo todo, int position) {
        Intent intent = new Intent(TodoListActivity.this, ViewTodoActivity.class);
        intent.putExtra("modifier", true);
        intent.putExtra("todo", todo);
        startActivityForResult.launch(intent);
    }

    @Override
    public void onTodoLongClicked(Todo todo, int position) {

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
                APP_DATABASE.requestDatabase(getApplicationContext()).dao().request_insert_todo(todo);
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
    private void refreshTodos() {
        todos.clear();
        todosAdapter.notifyDataSetChanged();
        requestTodos(todosList);
    }

    @SuppressLint("NotifyDataSetChanged")
    ActivityResultLauncher<Intent> startActivityForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result != null) {
            if (result.getResultCode() == RequestCodes.REQUEST_ACTION_TODO_CODE) {
                refreshTodos();
                is_updated = true;
            }
        }
    });

    private void setResult() {
        Intent intent = new Intent();
        setResult(RequestCodes.REQUEST_UPDATE_LIST_CODE, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        setResult();
    }
}