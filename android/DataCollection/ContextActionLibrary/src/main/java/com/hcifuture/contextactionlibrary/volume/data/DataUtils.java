package com.hcifuture.contextactionlibrary.volume.data;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.gson.reflect.TypeToken;
import com.hcifuture.contextactionlibrary.contextaction.context.ConfigContext;
import com.hcifuture.contextactionlibrary.utils.FileUtils;

import com.hcifuture.contextactionlibrary.volume.AppManager;
import com.hcifuture.contextactionlibrary.volume.CrowdManager;
import com.hcifuture.contextactionlibrary.volume.DeviceManager;
import com.hcifuture.contextactionlibrary.volume.NoiseManager;
import com.hcifuture.contextactionlibrary.volume.PositionManager;
import com.hcifuture.contextactionlibrary.volume.SoundManager;
import com.hcifuture.contextactionlibrary.volume.TimeManager;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiresApi(api = Build.VERSION_CODES.N)
public class DataUtils {
    public final String TAG = "DataUtils";
    private Context mContext;
    private List<Reason> reasonList;
    private Map<Reason, List<Dataset.Sample>> map;
    private Map<Reason, List<Bundle>> context_map;

    public DataUtils(Context context) {
        mContext = context;
        reasonList = getReasons();
        map = new HashMap<>();
        for (Reason reason: reasonList) {
            map.put(reason, getSamplesForReason(reason));
        }
        context_map = new HashMap<>();
        for (Reason reason: reasonList) {
            context_map.put(reason, getContextsForReason(reason));
        }
    }

    private static final String FILE_DIR = ConfigContext.VOLUME_SAVE_FOLDER + "data/";

    public List<Reason> getReasonList() {
        return reasonList;
    }

    public List<String> getReasonStringList(int num) {
        int list_length = Math.min(reasonList.size(), num);
        List<String> temp = reasonList.subList(reasonList.size() - list_length, reasonList.size()).stream().map(Reason::getName).collect(Collectors.toList());
        Collections.reverse(temp);
        return temp;
    }

    public void setReasonList(List<Reason> reasonList) {
        this.reasonList = reasonList;
        saveReasons(reasonList);
    }

    public void addReason(Reason reason) {
        if (reason == null)
            return;

        if (reasonList == null)
            reasonList = new ArrayList<>();
        reasonList.add(reason);
        saveReasons(reasonList);
    }

    public List<Dataset.Sample> getSamples(Reason reason) {
        return map.get(reason);
    }

    public void setSamples(Reason reason, List<Dataset.Sample> samples) {
        if (reason == null)
            return;

        map.replace(reason, samples);
        saveSamplesForReason(reason, samples);
    }

    public void addSample(Reason reason, Dataset.Sample sample) {
        if (reason == null)
            return;

        List<Dataset.Sample> list = map.get(reason);
        if (list == null)
            list = new ArrayList<>();
        list.add(sample);
        map.replace(reason, list);
        saveSamplesForReason(reason, list);
    }

    public static void saveReasons(List<Reason> reasons) {
        String result = Model.gson.toJson(reasons);
        FileUtils.writeStringToFile(result, new File(FILE_DIR + "reasons.json"));
    }

    public static List<Reason> getReasons() {
        Type type = new TypeToken<List<Reason>>(){}.getType();
        List<Reason> result = Model.gson.fromJson(
                FileUtils.getFileContent(FILE_DIR + "reasons.json"),
                type
        );
        if (result == null)
            result = new ArrayList<>();
        return result;
    }

    public static Reason getReasonByName(List<Reason> reasons, String name) {
        if (reasons != null) {
            for (Reason reason : reasons) {
                if (reason.getName().equals(name))
                    return reason;
            }
        }
        return null;
    }

    public void updateReasonList(String name) {
        if (name != null && reasonList != null) {
            for (int i = 0; i < reasonList.size(); i++) {
                if (reasonList.get(i).getName().equals(name)) {
                    Reason reason = new Reason(reasonList.get(i).getId(), name);
                    reasonList.remove(i);
                    addReason(reason);
                    break;
                }
            }
        }
    }

    public static List<Dataset.Sample> getSamplesForReason(Reason reason) {
        String id = reason.getId();

        Type type = new TypeToken<List<Dataset.Sample>>(){}.getType();
        List<Dataset.Sample> result = Model.gson.fromJson(
                FileUtils.getFileContent(FILE_DIR + id + ".json"),
                type
        );
        if (result == null)
            result = new ArrayList<>();
        return result;
    }

    public static void saveSamplesForReason(Reason reason, List<Dataset.Sample> samples) {
        String id = reason.getId();

        String result = Model.gson.toJson(samples);
        FileUtils.writeStringToFile(result, new File(FILE_DIR + id + ".json"));
    }

    public static List<Bundle> getContextsForReason(Reason reason) {
        String id = reason.getId();

        Type type = new TypeToken<List<Bundle>>(){}.getType();
        List<Bundle> result = Model.gson.fromJson(
                FileUtils.getFileContent(FILE_DIR + id + "_context.json"),
                type
        );
        if (result == null)
            result = new ArrayList<>();
        return result;
    }

    public static void saveContextsForReason(Reason reason, List<Bundle> contexts) {
        String id = reason.getId();

        String result = Model.gson.toJson(contexts);
        FileUtils.writeStringToFile(result, new File(FILE_DIR + id + "_context.json"));
    }

    public static DecisionTree getDTForReason(Reason reason) {
        String id = reason.getId();

        DecisionTree result = DecisionTree.fromJson(
                FileUtils.getFileContent(FILE_DIR + "DT/" + id + ".json")
        );
        // result might be null
        return result;
    }

    public static void saveDTForReason(Reason reason, DecisionTree tree) {
        String id = reason.getId();

        String result = DecisionTree.toJson(tree);
        FileUtils.writeStringToFile(result, new File(FILE_DIR + "DT/" + id + ".json"));
    }

    public static List<Dataset.Feature> allFeatures = new ArrayList<>(Arrays.asList(
            new Dataset.Feature("Noise"),
            new Dataset.Feature("Device"),
            new Dataset.Feature("Position"),
            new Dataset.Feature("App"),
            new Dataset.Feature("Bluetooth"),
            new Dataset.Feature("Audio"),
            new Dataset.Feature("Time"),
            new Dataset.Feature("Volume")
    ));

    public void addContextForReason(Reason reason, Bundle context) {
        List<Bundle> list = context_map.get(reason);
        if (list == null)
            list = new ArrayList<>();
        list.add(context);
        context_map.replace(reason, list);
        saveContextsForReason(reason, list);
    }

    public Object[] getLatestFeatureValues(List<Dataset.Feature> features) {
        List<Object> valueList = new ArrayList<>();
        for (Dataset.Feature feature: features) {
            switch (feature.name) {
                case "Noise":
                    valueList.add(NoiseManager.latest_noiseLevel);
                    break;
                case "Device":
                    valueList.add(DeviceManager.latest_device);
                    break;
                case "Position":
                    valueList.add(PositionManager.latest_position);
                    break;
                case "App":
                    valueList.add(AppManager.latest_id);
                    break;
                case "Bluetooth":
                    valueList.add(CrowdManager.latest_bleNumLevel);
                    break;
                case "Audio":
                    valueList.add(SoundManager.latest_audioLevel);
                    break;
                case "Time":
                    valueList.add(TimeManager.latest_formalizedTime);
                    break;
                case "Volume":
                    AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                    double volume = ((double) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    valueList.add(SoundManager.volume2level(volume));
                    break;
            }
        }
        return new Object[]{valueList};
    }
}

