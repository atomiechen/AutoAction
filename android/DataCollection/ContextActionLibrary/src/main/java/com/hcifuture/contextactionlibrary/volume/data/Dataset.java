package com.hcifuture.contextactionlibrary.volume.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dataset {

    List<Feature> features = new ArrayList<>();
    List<Sample> samples = new ArrayList<>();

    class Feature {
        String name;
    }

    class FeatureValue {
        // TODO: override equal() and hash()
    }

    class Sample {
        Map<Feature, FeatureValue> featureValues = new HashMap<>();
        // label从0开始取值
        int label;

        FeatureValue getValue(Feature feature) {
            return featureValues.get(feature);
        }
    }
}
