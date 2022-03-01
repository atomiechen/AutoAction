package com.example.datacollection;

import android.util.Log;

import com.example.datacollection.utils.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TaskList implements Serializable {
    private String id;
    private String date;
    private String description;
    private List<Task> task;
    private static final String ID_ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";

    public enum FILE_TYPE {
        SENSOR,
        TIMESTAMP,
        AUDIO,
        VIDEO
    }

    public static String generateRandomId(int size) {
        Random random = new Random();
        StringBuilder builder = new StringBuilder(size);
        for(int i = 0; i < size; i++) {
            builder.append(ID_ALLOWED_CHARACTERS.charAt(random.nextInt(ID_ALLOWED_CHARACTERS.length())));
        }
        return builder.toString();
    }

    public static String generateRandomTaskListId() {
        return "TL" + generateRandomId(8);
    }

    public static String generateRandomTaskId() {
        return "TK" + generateRandomId(8);
    }

    public static String generateRandomSubtaskId() {
        return "ST" + generateRandomId(8);
    }

    public static String generateRandomRecordId() {
        return "RD" + generateRandomId(8);
    }

    public static TaskList parseFromFile(InputStream is) {
        try {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }

            String jsonString = writer.toString();
            Gson gson = new GsonBuilder().create();
            TaskList taskList = gson.fromJson(jsonString, TaskList.class);
            // taskList.updateSubtask();
            return taskList;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static TaskList parseFromLocalFile() {
        TaskList taskList = null;
        try {
            taskList = TaskList.parseFromFile(new FileInputStream(BuildConfig.SAVE_PATH + "tasklist.json"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return taskList;
    }

    public static void saveToLocalFile(TaskList taskList) {
        FileUtils.writeStringToFile(new Gson().toJson(taskList), new File(BuildConfig.SAVE_PATH + "tasklist.json"));
    }

    public String[] getTaskName() {
        int size = getTask().size();
        String[] taskName = new String[size];
        for(int i = 0; i < size; i++) {
            Task t = getTask().get(i);
            taskName[i] = t.getId() + ". " + t.getName();
        }
        return taskName;
    }

    /*
    private void updateSubtask() {
        for(Task task: getTask()) {
            for(Task.Subtask subtask: task.getSubtask()) {
                if (subtask.getTimes() == 0) {
                    subtask.setTimes(task.getTimes());
                }
                if (subtask.getDuration() == 0) {
                    subtask.setDuration(task.getDuration());
                }
                subtask.setAudio(subtask.isAudio() | task.isAudio());
                subtask.setVideo(subtask.isVideo() | task.isVideo());
            }
        }
    }
     */

    public void addTask(Task newTask) {
        task.add(newTask);
    }

    /*
    public void resetId() {
        for (int i = 0; i < task.size(); i++) {
            task.get(i).id = i + 1;
        }
    }
     */

    public static class Task implements Serializable {
        private String id;
        private String name;
        private int times;
        private int duration;
        private boolean audio;
        public boolean video;
        public List<Subtask> subtask;

        public Task(String id, String name, int times, int duration, boolean audio, boolean video) {
            this.id = id;
            this.name = name;
            this.times = times;
            this.duration = duration;
            this.audio = audio;
            this.video = video;
            this.subtask = new ArrayList<>();
        }

        public void addSubtask(Subtask newSubtask) {
            subtask.add(newSubtask);
        }

        /*
        public void resetId() {
            for(int i = 0; i < subtask.size(); i++) {
                subtask.get(i).id = i + 1;
            }
        }
         */

        public String[] getSubtaskName() {
            int size = getSubtask().size();
            String[] taskName = new String[size];
            for(int i = 0; i < size; i++) {
                Subtask t = getSubtask().get(i);
                taskName[i] = t.getId() + ". " + t.getName();
            }
            return taskName;
        }

        public static class Subtask implements Serializable {
            private String name;
            private String id;
            private int times;
            private int duration;
            private boolean audio;
            private boolean video;

            public Subtask(String id, String name, int times, int duration, boolean audio, boolean video) {
                this.id = id;
                this.name = name;
                this.times = times;
                this.duration = duration;
                this.audio = audio;
                this.video = video;
            }

            public void setTimes(int times) {
                this.times = times;
            }

            public void setVideo(boolean video) {
                this.video = video;
            }

            public void setName(String name) {
                this.name = name;
            }

            public void setId(String id) {
                this.id = id;
            }

            public void setDuration(int duration) {
                this.duration = duration;
            }

            public void setAudio(boolean audio) {
                this.audio = audio;
            }

            public String getName() {
                return name;
            }

            public int getTimes() {
                return times;
            }

            public String getId() {
                return id;
            }

            public long getDuration() {
                return duration;
            }

            public boolean isVideo() {
                return video;
            }

            public boolean isAudio() {
                return audio;
            }
        }

        public boolean isAudio() {
            return audio;
        }

        public boolean isVideo() {
            return video;
        }

        public int getDuration() {
            return duration;
        }

        public String getId() {
            return id;
        }

        public int getTimes() {
            return times;
        }

        public List<Subtask> getSubtask() {
            return subtask;
        }

        public String getName() {
            return name;
        }

        public void setAudio(boolean audio) {
            this.audio = audio;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setSubtask(List<Subtask> subtask) {
            this.subtask = subtask;
        }

        public void setTimes(int times) {
            this.times = times;
        }

        public void setVideo(boolean video) {
            this.video = video;
        }
    }

    public List<Task> getTask() {
        return task;
    }

    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTask(List<Task> task) {
        this.task = task;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTasks(List<Task> tasks) {
        this.task = tasks;
    }
}
