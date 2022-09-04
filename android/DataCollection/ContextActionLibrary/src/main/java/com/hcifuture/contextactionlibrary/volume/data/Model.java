package com.hcifuture.contextactionlibrary.volume.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

abstract public class Model {

    // GSON part
    public static final JsonSerializer<Dataset.FeatureValue> featureValueSerializer = (src, typeOfSrc, context) -> {
        if (src == null) {
            return JsonNull.INSTANCE;
        } else {
            return context.serialize(src.toString());
        }
    };

    public static final JsonDeserializer<Dataset.FeatureValue> featureValueDeserializer = (json, typeOfT, context) -> {
        if (json.isJsonPrimitive()) {
            String str = json.getAsString();
            Object v = null;
            try {
                v = Integer.valueOf(str);
            } catch (NumberFormatException e) {
                try {
                    v = Double.valueOf(str);
                } catch (NumberFormatException e1) {
                }
            }
            return Dataset.FeatureValue.createFeatureValue(v);
        } else {
            return null;
        }
    };

    public static final JsonSerializer<Dataset.Feature> featureSerializer = (src, typeOfSrc, context) -> {
        if (src == null) {
            return JsonNull.INSTANCE;
        } else {
            return context.serialize(src.toString());
        }
    };

    public static final JsonDeserializer<Dataset.Feature> featureDeserializer = (json, typeOfT, context) -> {
        if (json.isJsonPrimitive()) {
            String str = json.getAsString();
            return new Dataset.Feature(str);
        } else {
            return null;
        }
    };

    public static final Gson gson = new GsonBuilder().disableHtmlEscaping()
            .registerTypeAdapter(Dataset.FeatureValue.class, featureValueSerializer)
            .registerTypeAdapter(Dataset.FeatureValue.class, featureValueDeserializer)
            .registerTypeAdapter(Dataset.Feature.class, featureSerializer)
            .registerTypeAdapter(Dataset.Feature.class, featureDeserializer)
            .create();

    // class methods
    abstract public int predict(Dataset.Sample sample);
    abstract public void train(Dataset dataset);

    public List<Integer> predict(Dataset dataset) {
        List<Integer> results = new ArrayList<>(dataset.samples.size());
        for (Dataset.Sample sample : dataset.samples) {
            results.add(predict(sample));
        }
        return results;
    }

    public static double getAccuracy(Dataset dataset, List<Integer> results) {
        int correct = 0;
        Iterator<Integer> iterator = results.iterator();
        for (Dataset.Sample sample : dataset.samples) {
            if (sample.label == iterator.next()) {
                correct += 1;
            }
        }
        return correct / (double) dataset.samples.size();
    }

    public static double[] getStatistics(Dataset dataset, List<Integer> results, int targetLabel) {
        int[] fourCount = getFourCount(dataset, results, targetLabel);
        int TP = fourCount[0], TN = fourCount[1], FP = fourCount[2], FN = fourCount[3];
        double accuracy = (TP + TN) / (double) dataset.samples.size();
        double precision = TP / (double) (TP + FP);
        double recall = TP / (double) (TP + FN);
        double F1 = 2 * precision * recall / (precision + recall);
        return new double[]{accuracy, precision, recall, F1};
    }

    public static int[] getFourCount(Dataset dataset, List<Integer> results, int targetLabel) {
        int TP = 0, TN = 0, FP = 0, FN = 0;
        Iterator<Integer> iterator = results.iterator();
        for (Dataset.Sample sample : dataset.samples) {
            int predictedLabel = iterator.next();
            if (sample.label == predictedLabel) {
                // 预测正确
                if (predictedLabel == targetLabel) {
                    // 正例
                    TP++;
                } else {
                    // 负例
                    TN++;
                }
            } else {
                // 预测错误
                if (predictedLabel == targetLabel) {
                    // 预测为正例
                    FP++;
                } else {
                    // 预测为负例
                    FN++;
                }
            }
        }
        return new int[]{TP, TN, FP, FN};
    }
}
