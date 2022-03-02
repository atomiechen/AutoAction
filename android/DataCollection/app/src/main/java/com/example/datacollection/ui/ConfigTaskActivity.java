package com.example.datacollection.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;

import com.example.datacollection.R;
import com.example.datacollection.utils.bean.TaskListBean;
import com.example.datacollection.ui.adapter.TaskAdapter;
import com.example.datacollection.utils.NetworkUtils;
import com.example.datacollection.utils.bean.StringListBean;
import com.google.gson.Gson;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;

public class ConfigTaskActivity extends AppCompatActivity {
    private ListView taskListView;

    private TaskListBean taskList;
    private TaskAdapter taskAdapter;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        mContext = this;

        Button backButton = findViewById(R.id.taskBackButton);
        backButton.setOnClickListener((v) -> this.finish());

        Button addTaskButton = findViewById(R.id.addTaskButton);
        addTaskButton.setOnClickListener((v) -> {
            Intent intent = new Intent(ConfigTaskActivity.this, AddTaskActivity.class);
            startActivity(intent);
        });

        taskListView = findViewById(R.id.trainListView);
        loadTaskListViaNetwork();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTaskListViaNetwork();
    }

    private void loadTaskListViaNetwork() {
        NetworkUtils.getAllTaskList(mContext, new StringCallback() {
            @Override
            public void onSuccess(Response<String> response) {
                StringListBean taskLists = new Gson().fromJson(response.body(), StringListBean.class);
                if (taskLists.getResult().size() > 0) {
                    String taskListId = taskLists.getResult().get(0);
                    NetworkUtils.getTaskList(mContext, taskListId, 0, new StringCallback() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            taskList = new Gson().fromJson(response.body(), TaskListBean.class);
                            taskAdapter = new TaskAdapter(mContext, taskList);
                            taskListView.setAdapter(taskAdapter);
                        }
                    });
                }
            }
        });
    }
}