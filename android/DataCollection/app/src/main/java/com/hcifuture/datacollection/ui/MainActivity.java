package com.hcifuture.datacollection.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.hcifuture.datacollection.R;
import com.hcifuture.datacollection.inference.IMUSensorManager;
import com.hcifuture.datacollection.inference.Inferencer;
import com.hcifuture.datacollection.service.MainService;
import com.hcifuture.datacollection.utils.GlobalVariable;
import com.hcifuture.datacollection.utils.bean.TaskListBean;
import com.hcifuture.datacollection.data.Recorder;
import com.hcifuture.datacollection.utils.NetworkUtils;
import com.hcifuture.datacollection.utils.bean.StringListBean;
import com.hcifuture.datacollection.visual.RecordListActivity;
import com.google.gson.Gson;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

public class MainActivity extends AppCompatActivity {

    private Context mContext;
    private AppCompatActivity mActivity;
    private Vibrator vibrator;

    // ui
    private EditText user;
    private Button startButton;
    private Button stopButton;
    private TextView description;
    private TextView counter;

    private Spinner taskSpinner;
    private ArrayAdapter<String> taskAdapter;

    private Spinner subtaskSpinner;
    private ArrayAdapter<String> subtaskAdapter;

    // task
    private TaskListBean taskList;  // queried from the backend
    private String[] taskName;
    private String[] subtaskName;
    private int curTaskId = 0;
    private int curSubtaskId = 0;

    private boolean isVideo;

    private CheckBox cameraSwitch;

    private Recorder recorder;

    // permission
    private static final int RC_PERMISSIONS = 0;
    private String[] permissions = new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.VIBRATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACTIVITY_RECOGNITION
    };

    private Inferencer inferencer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ask for permissions
        requestPermissions();

        mContext = this;
        mActivity = this;

        // vibrate to indicate data collection progress
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        recorder = new Recorder(this, new Recorder.RecorderListener() {
            @Override
            public void onTick(int tickCount, int times) {
                counter.setText(tickCount + " / " + times);
                vibrator.vibrate(VibrationEffect.createOneShot(200, 128));
            }

            @Override
            public void onFinish() {
                vibrator.vibrate(VibrationEffect.createOneShot(600, 128));
                enableButtons(false);
            }
        });

        // jump to accessibility settings
        Button accessibilityButton = findViewById(R.id.accessibilityButton);
        accessibilityButton.setOnClickListener((v) -> {
            Intent settingIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(settingIntent);
        });

        Button upgradeButton = findViewById(R.id.upgradeButton);
        upgradeButton.setOnClickListener((v) -> {
            MainService.getInstance().upgrade();
        });

        // goto test activity.
        Button testButton = findViewById(R.id.testButton);
        testButton.setOnClickListener((v) -> {
            Intent intent = new Intent(MainActivity.this, TestModelActivity.class);
            startActivity(intent);
        });

        inferencer = Inferencer.getInstance();
        inferencer.start(this);
    }

    /**
     * Called in onResume().
     */
    private void loadTaskListViaNetwork() {
        NetworkUtils.getAllTaskList(this, new StringCallback() {
            @Override
            public void onSuccess(Response<String> response) {
                StringListBean taskLists = new Gson().fromJson(response.body(), StringListBean.class);
                if (taskLists.getResult().size() > 0) {
                    String taskListId = taskLists.getResult().get(0);
                    GlobalVariable.getInstance().putString("taskListId", taskListId);
                    NetworkUtils.getTaskList(mContext, taskListId, 0, new StringCallback() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            taskList = new Gson().fromJson(response.body(), TaskListBean.class);
                            initView();
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTaskListViaNetwork();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * Pop up dialog windows to ask users for system permissions.
     */
    @AfterPermissionGranted(RC_PERMISSIONS)
    private void requestPermissions() {
        if (EasyPermissions.hasPermissions(this, permissions)) {
            // have permissions
        } else {
            // no permissions, request dynamically
            EasyPermissions.requestPermissions(
                    new PermissionRequest.Builder(this, RC_PERMISSIONS, permissions)
                            .setRationale(R.string.rationale)
                            .setPositiveButtonText(R.string.rationale_ask_ok)
                            .setNegativeButtonText(R.string.rationale_ask_cancel)
                            .setTheme(R.style.Theme_AppCompat)
                            .build());
        }
    }

    /**
     * Init the status of all UI components in main activity.
     * Called in loadTaskListViaNetwork().
     */
    private void initView() {
        user = findViewById(R.id.user);
        user.setText("SomeUser");
        description = findViewById(R.id.description);
        counter = findViewById(R.id.counter);

        // Spinner
        taskSpinner = findViewById(R.id.task_spinner);
        subtaskSpinner = findViewById(R.id.subtask_spinner);

        // choose tasks and subtasks
        taskName = taskList.getTaskName();
        taskAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, taskName);
        taskAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        taskSpinner.setAdapter(taskAdapter);
        taskSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                curTaskId = position;
                subtaskName = taskList.getTask().get(curTaskId).getSubtaskName();
                subtaskAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, subtaskName);
                subtaskAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                subtaskSpinner.setAdapter(subtaskAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if (taskName.length == 0) {
            subtaskName = new String[0];
        }
        else {
            subtaskName = taskList.getTask().get(curTaskId).getSubtaskName();
        }
        subtaskAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subtaskName);
        subtaskAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subtaskSpinner.setAdapter(subtaskAdapter);
        subtaskSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                curSubtaskId = position;
                description.setText(subtaskName[curSubtaskId]);
                isVideo = taskList.getTask().get(curTaskId).getSubtask().get(curSubtaskId).isVideo() |
                          taskList.getTask().get(curTaskId).isAudio();
                cameraSwitch.setChecked(isVideo);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // whether to record the video
        cameraSwitch = findViewById(R.id.video_switch);
        cameraSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            recorder.setCamera(b);
        });
        cameraSwitch.setEnabled(false); // disabled

        startButton = findViewById(R.id.start);
        stopButton = findViewById(R.id.stop);

        Button configButton = findViewById(R.id.configButton);
        Button trainButton = findViewById(R.id.trainButton);
        Button visualButton = findViewById(R.id.visualButton);

        // click the start button to start recorder
        startButton.setOnClickListener(view -> {
            enableButtons(true);
            recorder.start(
                    user.getText().toString(),
                    curTaskId,
                    curSubtaskId,
                    taskList
            );
        });

        // click the stop button to end recording
        stopButton.setOnClickListener(view -> {
            recorder.interrupt();
            enableButtons(false);
        });

        // goto config task activity
        configButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ConfigTaskActivity.class);
            startActivity(intent);
        });

        // goto train activity
        trainButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, TrainActivity.class);
            startActivity(intent);
        });

        // goto record list activity
        visualButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, RecordListActivity.class);
            startActivity(intent);
        });

        // set the default status of the start and end buttons
        enableButtons(false);
    }

    /**
     * Set the availability of the start and stop buttons.
     * Ensures the status of these two buttons are opposite.
     * @param isRecording Whether the current task is ongoing.
     */
    private void enableButtons(boolean isRecording) {
        startButton.setEnabled(!isRecording);
        stopButton.setEnabled(isRecording);
    }

    /**
     * Cancel the vibrator.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vibrator != null) {
            vibrator.cancel();
        }
    }
}