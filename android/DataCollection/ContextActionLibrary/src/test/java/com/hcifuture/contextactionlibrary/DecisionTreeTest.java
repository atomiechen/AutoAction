package com.hcifuture.contextactionlibrary;

import com.hcifuture.contextactionlibrary.volume.data.Dataset;
import com.hcifuture.contextactionlibrary.volume.data.DecisionTree;
import com.hcifuture.contextactionlibrary.volume.data.Model;

import org.junit.Test;

import java.util.List;

public class DecisionTreeTest {

    @Test
    public void testDecisionTree() {
        System.out.println("start decision tree test");

        DecisionTree tree = new DecisionTree().setAlgorithm(DecisionTree.Algorithm.CART);
        Dataset dataset = new Dataset(2);
        dataset.addFeatures("A", "B", "C");

        // sample里面的数据顺序必须和feature顺序一致
        dataset.addSample(0, new Object[]{1,2,3});
        // duplicate sample with different label
        dataset.addSample(1, new Object[]{1,2,3});
        dataset.addSample(1, new Object[]{1,2,3});
        dataset.addSample(1, new Object[]{2,2,4});
        dataset.addSample(0, new Object[]{3,3,5});
        dataset.addSample(1, new Object[]{1,4,3});

        tree.train(dataset);
        int prediction = tree.predict(new Object[]{1,3,5});

        // print dataset
        System.out.println(Model.gson.toJson(dataset));

        // print prediction
        System.out.println("prediction: " + prediction);

        // print statistics
        List<Integer> results = tree.predict(dataset);
//        System.out.println("accuracy on train set: " + Model.getAccuracy(dataset, results));
        // 0 for positive label
        double[] statistics = Model.getStatistics(dataset, results, 0);
        double accuracy = statistics[0], precision = statistics[1], recall = statistics[2], F1 = statistics[3];
        System.out.println(String.format("statistics on train set: \n" +
                "  accuracy = %f\n" +
                "  precision = %f\n" +
                "  recall = %f\n" +
                "  F1 = %f",
                accuracy, precision, recall, F1));

        // print tree
        String strRepresentation = DecisionTree.toJson(tree);
        System.out.println(strRepresentation);

        // load from tree's string representation
        DecisionTree newTree = DecisionTree.fromJson(strRepresentation);
        System.out.println(DecisionTree.toJson(newTree));

        System.out.println("end decision tree test");
    }
}
