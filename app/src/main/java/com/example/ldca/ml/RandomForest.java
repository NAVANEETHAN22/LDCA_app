package com.example.ldca.ml;

import java.util.Random;

public class RandomForest {

    private final int nTrees;
    private final DecisionTree[] trees;
    private double[] featureImportances;

    public RandomForest(int nTrees) {
        this.nTrees = nTrees;
        this.trees = new DecisionTree[nTrees];
    }

    public void train(DataSet data) {
        Random rng = new Random(42);
        int n = data.rows;
        int features = data.cols;
        featureImportances = new double[features];

        for (int t = 0; t < nTrees; t++) {

            // Bootstrap sample (sample with replacement)
            double[][] bootX = new double[n][features];
            int[] bootY = new int[n];
            for (int i = 0; i < n; i++) {
                int idx = rng.nextInt(n);
                bootX[i] = data.X[idx];
                bootY[i] = data.y[idx];
            }

            DataSet bootData = new DataSet(bootX, bootY);
            DecisionTree tree = new DecisionTree();
            tree.train(bootData);
            trees[t] = tree;

            // Accumulate feature importance (which feature was used to split)
            featureImportances[tree.featureIndex]++;
        }

        // Normalise
        for (int j = 0; j < features; j++) {
            featureImportances[j] /= nTrees;
        }
    }

    public int predict(double[] x) {
        int votes0 = 0, votes1 = 0;
        for (DecisionTree tree : trees) {
            if (tree.predict(x) == 0) votes0++;
            else votes1++;
        }
        return votes1 > votes0 ? 1 : 0;
    }

    /**
     * Returns a probability score for class 1 (fraction of trees that voted 1).
     * Used for ROC / PR curve computation.
     */
    public double score(double[] x) {
        int votes1 = 0;
        for (DecisionTree tree : trees) {
            if (tree.predict(x) == 1) votes1++;
        }
        return (double) votes1 / nTrees;
    }

    /** Per-feature importance in [0,1], sums ≈ 1. */
    public double[] featureImportances() {
        return featureImportances;
    }

}
