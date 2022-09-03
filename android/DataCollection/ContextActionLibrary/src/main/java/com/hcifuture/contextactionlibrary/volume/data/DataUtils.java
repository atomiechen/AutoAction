package com.hcifuture.contextactionlibrary.volume.data;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.google.gson.reflect.TypeToken;
import com.hcifuture.contextactionlibrary.contextaction.context.ConfigContext;
import com.hcifuture.contextactionlibrary.utils.FileUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.N)
public class DataUtils {
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
}

