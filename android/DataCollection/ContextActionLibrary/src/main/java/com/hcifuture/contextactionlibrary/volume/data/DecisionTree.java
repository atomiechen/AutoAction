package com.hcifuture.contextactionlibrary.volume.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DecisionTree {

    public enum Algorithm {
        ID3, C4_5, CART
    }

    static class TreeNode {
        Map<Dataset.FeatureValue, TreeNode> branches;
        Dataset.Feature feature;
        Integer label = null;
    }

    TreeNode root = new TreeNode();
    Algorithm algorithm = Algorithm.C4_5;
    List<Dataset.Feature> features;
    boolean trained = false;

    public DecisionTree setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    public void train(Dataset dataset) {
        if (dataset.samples.isEmpty()) {
            return;
        } else {
            this.features = dataset.features;
            genTree(root, dataset);
            trained = true;
        }
    }

    private void genTree(TreeNode rootNode, Dataset dataset) {
        int label = checkSameLabel(dataset);
        if (label != Dataset.LABEL_INVALID) {
            // same label
            rootNode.label = label;
            return;
        }
        if (dataset.features.isEmpty()) {
            // no feature, choose the majority
            rootNode.label = getMajorityLabel(dataset);
            return;
        }

        Dataset.Feature feature = decideBestFeature(dataset, algorithm);
        Set<Dataset.FeatureValue> values = getValues(dataset, feature);
        if (!values.isEmpty()) {
            rootNode.branches = new HashMap<>();
            rootNode.feature = feature;
            for (Dataset.FeatureValue value : values) {
                Dataset newDataset = splitDataset(dataset, feature, value);
                TreeNode subNode = new TreeNode();
                rootNode.branches.put(value, subNode);
                genTree(subNode, newDataset);
            }
        } else {
            // Should not happen
            rootNode.label = Dataset.LABEL_INVALID;
        }
    }

    private static int checkSameLabel(Dataset dataset) {
        boolean consistent = false;
        int testLabel;
        for (testLabel = 0; testLabel < dataset.labelCount; testLabel++) {
            boolean same = true;
            for (Dataset.Sample sample : dataset.samples) {
                if (sample.label != testLabel) {
                    same = false;
                    break;
                }
            }
            if (same) {
                consistent = true;
                break;
            }
        }
        if (consistent) {
            return testLabel;
        } else {
            return Dataset.LABEL_INVALID;
        }
    }

    private static int[] countLabels(Dataset dataset) {
        int [] countBin = new int[dataset.labelCount];
        for (Dataset.Sample sample : dataset.samples) {
            countBin[sample.label]++;
        }
        return countBin;
    }

    private static int getMajorityLabel(Dataset dataset) {
        int [] countBin = countLabels(dataset);;
        int maxCount = 0;
        int maxLabel = Dataset.LABEL_INVALID;
        for (int label = 0; label < dataset.labelCount; label++) {
            if (countBin[label] > maxCount) {
                maxCount = countBin[label];
                maxLabel = label;
            }
        }
        return maxLabel;
    }

    private static Dataset.Feature decideBestFeature(Dataset dataset, Algorithm algorithm) {
        double maxGain = -1.1; // max gini index is 1; using the negative initial value
        Dataset.Feature bestFeature = dataset.features.get(0);
        for (Dataset.Feature feature : dataset.features) {
            double currentGain = 0;
            switch (algorithm) {
                case ID3:
                    currentGain = gain(dataset, feature);
                    break;
                case C4_5:
                    currentGain = gainRatio(dataset, feature);
                    break;
                case CART:
                    // smaller is better
                    currentGain = -giniIndex(dataset, feature);
                    break;
            }
            if (currentGain > maxGain) {
                maxGain = currentGain;
                bestFeature = feature;
            }
        }
        return bestFeature;
    }

    private static double infoD(Dataset dataset) {
        int [] countBin = countLabels(dataset);
        double sum = 0;
        for (int label = 0; label < dataset.labelCount; label++) {
            double prob = countBin[label] / (double) dataset.samples.size();
            sum -= prob * Math.log(prob) / Math.log(2);
        }
        return sum;
    }

    private static double infoA(Dataset dataset, Dataset.Feature feature) {
        double sum = 0;
        Set<Dataset.FeatureValue> values = getValues(dataset, feature);
        for (Dataset.FeatureValue value : values) {
            Dataset newDataset = splitDataset(dataset, feature, value);
            double prob = newDataset.samples.size() / (double) dataset.samples.size();
            sum += prob * infoD(newDataset);
        }
        return sum;
    }

    private static double gain(Dataset dataset, Dataset.Feature feature) {
        return infoD(dataset) - infoA(dataset, feature);
    }

    private static double splitInfo(Dataset dataset, Dataset.Feature feature) {
        double sum = 0;
        Set<Dataset.FeatureValue> values = getValues(dataset, feature);
        for (Dataset.FeatureValue value : values) {
            Dataset newDataset = splitDataset(dataset, feature, value);
            double prob = newDataset.samples.size() / (double) dataset.samples.size();
            sum -= prob * Math.log(prob) / Math.log(2);
        }
        return sum;
    }

    private static double gainRatio(Dataset dataset, Dataset.Feature feature) {
        return gain(dataset, feature) / splitInfo(dataset, feature);
    }

    private static double gini(Dataset dataset) {
        int [] countBin = countLabels(dataset);
        double sum = 1.0;
        for (int label = 0; label < dataset.labelCount; label++) {
            double prob = countBin[label] / (double) dataset.samples.size();
            sum -= prob * prob;
        }
        return sum;
    }

    private static double giniIndex(Dataset dataset, Dataset.Feature feature) {
        double sum = 0;
        Set<Dataset.FeatureValue> values = getValues(dataset, feature);
        for (Dataset.FeatureValue value : values) {
            Dataset newDataset = splitDataset(dataset, feature, value);
            sum += gini(newDataset) * newDataset.samples.size() / dataset.samples.size();
        }
        return sum;
    }

    private static Set<Dataset.FeatureValue> getValues(Dataset dataset, Dataset.Feature feature) {
        Set<Dataset.FeatureValue> values = new HashSet<>();
        for (Dataset.Sample sample : dataset.samples) {
            values.add(sample.getValue(feature));
        }
        return values;
    }

    private static Dataset splitDataset(Dataset dataset, Dataset.Feature feature, Dataset.FeatureValue featureValue) {
        Dataset dataset1 = new Dataset(dataset.labelCount);
        for (Dataset.Sample sample : dataset.samples) {
            if (sample.getValue(feature).equals(featureValue)) {
                dataset1.samples.add(sample);
            }
        }
        dataset1.features.addAll(dataset.features);
        dataset1.features.remove(feature);
        return dataset1;
    }

    public int predict(Dataset.Sample sample) {
        if (trained) {
            TreeNode currentNode = root;
            while (true) {
                if (currentNode.branches == null) {
                    // leaf node
                    return currentNode.label;
                }
                // go to child node
                currentNode = currentNode.branches.get(sample.getValue(currentNode.feature));
            }
        } else {
            return Dataset.LABEL_INVALID;
        }
    }

    public int predict(Object [] featureValues) {
        if (trained) {
            return predict(new Dataset.Sample(Dataset.LABEL_INVALID, featureValues, features));
        } else {
            return Dataset.LABEL_INVALID;
        }
    }

    public static String toJson(DecisionTree tree) {
        return ModelUtils.gson.toJson(tree, DecisionTree.class);
    }

    public static DecisionTree fromJson(String jsonStr) {
        return ModelUtils.gson.fromJson(jsonStr, DecisionTree.class);
    }
}
