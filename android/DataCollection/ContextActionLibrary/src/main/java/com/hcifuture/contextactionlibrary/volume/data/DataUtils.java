package com.hcifuture.contextactionlibrary.volume.data;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

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
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.N)
public class DataUtils {
    private Context mContext;

    public DataUtils(Context context) {
        mContext = context;
    }

    private static final String FILE_DIR = ConfigContext.VOLUME_SAVE_FOLDER + "data/";

    public static void saveReasons(List<Reason> reasons) {
        String result = ModelUtils.gson.toJson(reasons);
        FileUtils.writeStringToFile(result, new File(FILE_DIR + "reasons.json"));
    }

    public static List<Reason> getReasons() {
        Type type = new TypeToken<List<Reason>>(){}.getType();
        List<Reason> result = ModelUtils.gson.fromJson(
                FileUtils.getFileContent(FILE_DIR + "reasons.json"),
                type
        );
        if (result == null)
            result = new ArrayList<>();
        return result;
    }

    public static Reason getReasonByName(List<Reason> reasons, String name) {
        for (Reason reason: reasons) {
            if (reason.getName().equals(name))
                return reason;
        }
        return null;
    }

    public static List<Dataset.Sample> getSamplesForReason(Reason reason) {
        int id = reason.getId();

        Type type = new TypeToken<List<Dataset.Sample>>(){}.getType();
        List<Dataset.Sample> result = ModelUtils.gson.fromJson(
                FileUtils.getFileContent(FILE_DIR + id + ".json"),
                type
        );
        if (result == null)
            result = new ArrayList<>();
        return result;
    }

    public static void saveSamplesForReason(Reason reason, List<Dataset.Sample> samples) {
        int id = reason.getId();

        String result = ModelUtils.gson.toJson(samples);
        FileUtils.writeStringToFile(result, new File(FILE_DIR + id + ".json"));
    }

    public static DecisionTree getDTForReason(Reason reason) {
        int id = reason.getId();

        DecisionTree result = DecisionTree.fromJson(
                FileUtils.getFileContent(FILE_DIR + "DT/" + id + ".json")
        );
        // result might be null
        return result;
    }

    public static void saveDTForReason(Reason reason, DecisionTree tree) {
        int id = reason.getId();

        String result = DecisionTree.toJson(tree);
        FileUtils.writeStringToFile(result, new File(FILE_DIR + "DT/" + id + ".json"));
    }

    public Object[] getLatestFeatureValues(List<Dataset.Feature> features) {
        Integer int_val = -1;
        Double double_val = -1.0;
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

