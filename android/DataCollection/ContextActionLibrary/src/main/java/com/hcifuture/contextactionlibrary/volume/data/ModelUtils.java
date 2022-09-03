package com.hcifuture.contextactionlibrary.volume.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializer;

public class ModelUtils {

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

}
