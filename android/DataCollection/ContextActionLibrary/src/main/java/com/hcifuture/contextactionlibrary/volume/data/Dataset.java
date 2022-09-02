package com.hcifuture.contextactionlibrary.volume.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Dataset {

    // 标签数量，几分类
    int labelCount = 2;
    // 特征列表，表头
    List<Feature> features = new ArrayList<>();
    // 样本，内部存储的数据顺序与上述表头相同
    List<Sample> samples = new ArrayList<>();

    public Dataset(int labelCount) {
        this.labelCount = labelCount;
    }

    public void addFeature(String name) {
        features.add(new Feature(name));
    }

    public void addFeatures(String... names) {
        for (String name : names) {
            addFeature(name);
        }
    }

    public void addSample(int label, Object [] featureValues) {
        samples.add(new Sample(label, featureValues));
    }

    public Sample genSample(int label, Object [] featureValues) {
        return new Sample(label, featureValues);
    }

    static class Feature {
        String name;
        public Feature(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Feature feature = (Feature) o;
            return Objects.equals(name, feature.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    abstract public static class FeatureValue {
        // TODO: override equal() and hash()
    }

    public static class DiscreteFeatureValue extends FeatureValue {
        int value;

        public DiscreteFeatureValue(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DiscreteFeatureValue that = (DiscreteFeatureValue) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public static class NumericFeatureValue extends FeatureValue {
        double value;

        public NumericFeatureValue(double value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NumericFeatureValue that = (NumericFeatureValue) o;
            return Double.compare(that.value, value) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public class Sample {
        // 特征取值
        Map<Feature, FeatureValue> featureValues = new HashMap<>();
        // label从0开始取值
        int label;

        // 初始化时，feature value 顺序必须和表头一致
        public Sample(int label, Object [] featureValues) {
            this.label = label;
            for (int idx = 0; idx < featureValues.length; idx++) {
                Object v = featureValues[idx];
                if (v instanceof Integer) {
                    this.featureValues.put(features.get(idx), new DiscreteFeatureValue((int) v));
                } else if (v instanceof  Double) {
                    this.featureValues.put(features.get(idx), new NumericFeatureValue((double) v));
                }
            }
        }

        FeatureValue getValue(Feature feature) {
            return featureValues.get(feature);
        }
    }
}
