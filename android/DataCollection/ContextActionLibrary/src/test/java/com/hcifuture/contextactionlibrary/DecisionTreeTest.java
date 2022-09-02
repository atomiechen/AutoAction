package com.hcifuture.contextactionlibrary;

import com.hcifuture.contextactionlibrary.volume.data.Dataset;
import com.hcifuture.contextactionlibrary.volume.data.DecisionTree;

import org.junit.Test;

public class DecisionTreeTest {

    @Test
    public void testDecisionTree() {
        System.out.println("start decision tree test");

        DecisionTree tree = new DecisionTree();
        Dataset dataset = new Dataset();
        dataset.addFeature("A");
        dataset.addFeature("B");
        dataset.addFeature("C");

        dataset.addSample(0, new Integer[]{1,2,3});
        dataset.addSample(1, new Integer[]{2,2,4});
        dataset.addSample(1, new Integer[]{3,3,5});
        dataset.addSample(0, new Integer[]{1,2,3});
        dataset.addSample(0, new Integer[]{1,2,3});

        tree.train(dataset);
        int prediction = tree.predict(new Integer[]{2,2,4});
        System.out.println("prediction: " + prediction);

        System.out.println("end decision tree test");
    }
}
