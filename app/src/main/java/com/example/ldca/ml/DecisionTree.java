package com.example.ldca.ml;

/**
 * Decision Stump (depth-1 Decision Tree).
 * Finds the single best (feature, threshold) split by exhaustive search.
 * Fixed: was previously hardcoded to feature 0 / first-row threshold only.
 */
public class DecisionTree {

    public int    featureIndex = 0;
    double        threshold    = 0;
    int           leftClass    = 0;
    int           rightClass   = 1;

    public void train(DataSet data) {
        if (data == null || data.rows == 0 || data.cols == 0) return;

        int n        = data.rows;
        int features = data.cols;

        double bestAcc = -1;

        for (int f = 0; f < features; f++) {
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (data.X[i][f] < min) min = data.X[i][f];
                if (data.X[i][f] > max) max = data.X[i][f];
            }
            if (min == max) continue; // Skip constant features

            // Reduce search space for stability: Check 5 thresholds instead of 10
            int numSteps = 5;
            double step = (max - min) / numSteps;

            for (int t = 1; t <= numSteps; t++) {
                double thresh = min + t * step;

                int l0 = 0, l1 = 0, r0 = 0, r1 = 0;
                for (int i = 0; i < n; i++) {
                    if (data.X[i][f] <= thresh) {
                        if (data.y[i] == 0) l0++; else l1++;
                    } else {
                        if (data.y[i] == 0) r0++; else r1++;
                    }
                }

                int lc = l1 > l0 ? 1 : 0;
                int rc = r1 > r0 ? 1 : 0;

                // Count correct predictions for this split
                int correct = 0;
                for (int i = 0; i < n; i++) {
                    int pred = (data.X[i][f] <= thresh) ? lc : rc;
                    if (pred == data.y[i]) correct++;
                }

                double acc = (double) correct / n;
                if (acc > bestAcc) {
                    bestAcc      = acc;
                    featureIndex = f;
                    threshold    = thresh;
                    leftClass    = lc;
                    rightClass   = rc;
                }
            }
        }
    }

    public int predict(double[] x) {
        if (x == null || x.length == 0) return 0;
        return x[featureIndex] <= threshold ? leftClass : rightClass;
    }
}
