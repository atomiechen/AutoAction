package com.hcifuture.contextactionlibrary.volume.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DecisionTree {

    enum Algorithm {
        ID3, C4_5
    }

    class TreeNode {
        Map<Dataset.FeatureValue, TreeNode> branches;
        Dataset.Feature feature;
        int label;
    }

    TreeNode root = new TreeNode();
    int labelCount = 2;
    Algorithm algorithm = Algorithm.C4_5;
    Dataset dataset;

    public void train(Dataset dataset) {
        genTree(root, dataset);
        this.dataset = dataset;
    }

    private void genTree(TreeNode rootNode, Dataset dataset) {
        int label = checkSameLabel(dataset);
        if (label != -1) {
            // same label
            rootNode.label = label;
            return;
        }
        if (dataset.features.isEmpty()) {
            rootNode.label = getMajorityLabel(dataset);
            return;
        }

        Dataset.Feature feature = decideBestFeature(dataset);
        Set<Dataset.FeatureValue> values = getValues(dataset, feature);

        rootNode.branches = new HashMap<>();
        rootNode.feature = feature;
        for (Dataset.FeatureValue value : values) {
            Dataset newDataset = splitDataset(dataset, feature, value);
            TreeNode subNode = new TreeNode();
            rootNode.branches.put(value, subNode);
            genTree(subNode, newDataset);
        }
    }

    private int checkSameLabel(Dataset dataset) {
        boolean consistent = false;
        int testLabel;
        for (testLabel = 0; testLabel < labelCount; testLabel++) {
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
            return -1;
        }
    }

    private int getMajorityLabel(Dataset dataset) {
        int [] countBin = new int[labelCount];
        int maxCount = 0;
        int maxLabel = -1;
        for (Dataset.Sample sample : dataset.samples) {
            countBin[sample.label]++;
            if (countBin[sample.label] > maxCount) {
                maxCount = countBin[sample.label];
                maxLabel = sample.label;
            }
        }
        return maxLabel;
    }

    private Dataset.Feature decideBestFeature(Dataset dataset) {
        double maxGain = 0;
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
            }
            if (currentGain > maxGain) {
                maxGain = currentGain;
                bestFeature = feature;
            }
        }
        return bestFeature;
    }

    private double infoD(Dataset dataset) {
        int [] countBin = new int[labelCount];
        for (Dataset.Sample sample : dataset.samples) {
            countBin[sample.label]++;
        }
        double sum = 0;
        for (int label = 0; label < labelCount; label++) {
            double prob = countBin[label] / (double) dataset.samples.size();
            sum -= prob * Math.log(prob) / Math.log(2);
        }
        return sum;
    }

    private double infoA(Dataset dataset, Dataset.Feature feature) {
        double sum = 0;
        Set<Dataset.FeatureValue> values = getValues(dataset, feature);
        for (Dataset.FeatureValue value : values) {
            Dataset newDataset = splitDataset(dataset, feature, value);
            double prob = newDataset.samples.size() / (double) dataset.samples.size();
            sum += prob * infoD(newDataset);
        }
        return sum;
    }

    private double gain(Dataset dataset, Dataset.Feature feature) {
        return infoD(dataset) - infoA(dataset, feature);
    }

    private double splitInfo(Dataset dataset, Dataset.Feature feature) {
        double sum = 0;
        Set<Dataset.FeatureValue> values = getValues(dataset, feature);
        for (Dataset.FeatureValue value : values) {
            Dataset newDataset = splitDataset(dataset, feature, value);
            double prob = newDataset.samples.size() / (double) dataset.samples.size();
            sum -= prob * Math.log(prob) / Math.log(2);
        }
        return sum;
    }

    private double gainRatio(Dataset dataset, Dataset.Feature feature) {
        return gain(dataset, feature) / splitInfo(dataset, feature);
    }

    private Set<Dataset.FeatureValue> getValues(Dataset dataset, Dataset.Feature feature) {
        Set<Dataset.FeatureValue> values = new HashSet<>();
        for (Dataset.Sample sample : dataset.samples) {
            values.add(sample.getValue(feature));
        }
        return values;
    }

    private Dataset splitDataset(Dataset dataset, Dataset.Feature feature, Dataset.FeatureValue featureValue) {
        Dataset dataset1 = new Dataset();
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
        TreeNode currentNode = root;
        while (true) {
            TreeNode branchNode = currentNode.branches.get(sample.getValue(currentNode.feature));
            if (branchNode.branches == null) {
                // leaf node
                return branchNode.label;
            }
            currentNode = branchNode;
        }
    }

    public int predict(Object [] featureValues) {
        return predict(dataset.genSample(-1, featureValues));
    }
}
