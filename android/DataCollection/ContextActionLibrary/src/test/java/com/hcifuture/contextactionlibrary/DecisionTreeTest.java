package com.hcifuture.contextactionlibrary;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hcifuture.contextactionlibrary.contextaction.context.ConfigContext;
import com.hcifuture.contextactionlibrary.sensor.collector.Collector;
import com.hcifuture.contextactionlibrary.volume.data.Dataset;
import com.hcifuture.contextactionlibrary.volume.data.DecisionTree;
import com.hcifuture.contextactionlibrary.volume.data.ModelUtils;

import org.junit.Test;

public class DecisionTreeTest {

    @Test
    public void testDecisionTree() {
        System.out.println("start decision tree test");

        DecisionTree tree = new DecisionTree();
        Dataset dataset = new Dataset(2);
        dataset.addFeatures("A", "B", "C");

        // sample里面的数据顺序必须和feature顺序一致
        dataset.addSample(0, new Object[]{1,2,3});
        dataset.addSample(1, new Object[]{2,2,4});
        dataset.addSample(0, new Object[]{3,3,5});
        dataset.addSample(0, new Object[]{1,2,3});
        dataset.addSample(1, new Object[]{1,2,3});

        tree.train(dataset);
        int prediction = tree.predict(new Object[]{3,3,5});

        // print dataset
        System.out.println(ModelUtils.gson.toJson(dataset));

        // print prediction
        System.out.println("prediction: " + prediction);

        // print tree
        String strRepresentation = DecisionTree.toJson(tree);
        System.out.println(strRepresentation);

        // load from tree's string representation
        DecisionTree newTree = DecisionTree.fromJson(strRepresentation);
        System.out.println(DecisionTree.toJson(newTree));

        System.out.println("end decision tree test");
    }
}
