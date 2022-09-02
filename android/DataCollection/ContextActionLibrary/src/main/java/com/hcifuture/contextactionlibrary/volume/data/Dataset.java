package com.hcifuture.contextactionlibrary.volume.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Dataset {

    // 特征列表，表头
    List<Feature> features = new ArrayList<>();
    // 样本，内部存储的数据顺序与上述表头相同
    List<Sample> samples = new ArrayList<>();

    public void addFeature(String name) {
        features.add(new Feature(name));
    }

    public void addSample(int label, Object [] featureValues) {
        samples.add(new Sample(label, featureValues));
    }

    public Sample genSample(int label, Object [] featureValues) {
        return new Sample(label, featureValues);
    }

    class Feature {
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

    public class FeatureValue {
        // TODO: override equal() and hash()
    }

    public class DiscreteFeatureValue extends FeatureValue {
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

    public class NumericFeatureValue extends FeatureValue {
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
        FeatureValue [] fvalues;
        // label从0开始取值
        int label;

        public Sample(int label, Object [] featureValues) {
            FeatureValue [] values = new FeatureValue[features.size()];
            int index = 0;
            for (Object v : featureValues) {
                if (v instanceof Integer) {
                    values[index] = new DiscreteFeatureValue((int) v);
                } else if (v instanceof  Double) {
                    values[index] = new NumericFeatureValue((double) v);
                }
                index++;
            }
            this.fvalues = values;
            this.label = label;
        }

        FeatureValue getValue(Feature feature) {
//            return featureValues.get(feature);
            return fvalues[features.indexOf(feature)];
        }

        void setValues(FeatureValue [] values) {
            fvalues = values;
        }
    }
}
