package com.example.ldca.ml;

public class CrossValidation {

    /**
     * Run k-fold cross-validation for the given model name.
     *
     * @param data      full dataset
     * @param modelName one of "Naive Bayes", "Logistic Regression",
     *                  "Decision Tree", "Random Forest"
     * @param k         number of folds (typically 5)
     * @return CVResult containing per-fold accuracies and the mean accuracy
     */
    public static CVResult run(DataSet data, String modelName, int k) {
        // Guard: skip CV gracefully if data is null or too small
        if (data == null || data.rows < k || data.cols == 0) {
            throw new IllegalArgumentException(
                    "Dataset is null or too small for " + k + "-fold CV");
        }

        int n = data.rows;
        int foldSize = n / k;

        double[] foldAccuracies = new double[k];

        for (int fold = 0; fold < k; fold++) {

            int valStart = fold * foldSize;
            int valEnd   = (fold == k - 1) ? n : valStart + foldSize;
            int valSize  = valEnd - valStart;
            int trainSize = n - valSize;

            // Build train / val splits
            double[][] trainX = new double[trainSize][data.cols];
            int[]      trainY = new int[trainSize];
            double[][] valX   = new double[valSize][data.cols];
            int[]      valY   = new int[valSize];

            int ti = 0;
            for (int i = 0; i < n; i++) {
                if (i >= valStart && i < valEnd) {
                    valX[i - valStart] = data.X[i];
                    valY[i - valStart] = data.y[i];
                } else {
                    trainX[ti] = data.X[i];
                    trainY[ti] = data.y[i];
                    ti++;
                }
            }

            DataSet trainSet = new DataSet(trainX, trainY);

            int[] predictions = new int[valSize];

            switch (modelName) {
                case "Logistic Regression": {
                    LogisticRegression m = new LogisticRegression();
                    m.train(trainSet);
                    for (int i = 0; i < valSize; i++) predictions[i] = m.predict(valX[i]);
                    break;
                }
                case "Decision Tree": {
                    DecisionTree m = new DecisionTree();
                    m.train(trainSet);
                    for (int i = 0; i < valSize; i++) predictions[i] = m.predict(valX[i]);
                    break;
                }
                case "Random Forest": {
                    // Reduce complexity: 5 trees instead of 10
                    RandomForest m = new RandomForest(5);
                    m.train(trainSet);
                    for (int i = 0; i < valSize; i++) predictions[i] = m.predict(valX[i]);
                    break;
                }
                default: { // Naive Bayes
                    NaiveBayes m = new NaiveBayes();
                    m.train(trainSet);
                    for (int i = 0; i < valSize; i++) predictions[i] = m.predict(valX[i]);
                    break;
                }
            }

            foldAccuracies[fold] = Metrics.accuracy(valY, predictions);
        }

        double mean = 0;
        for (double acc : foldAccuracies) mean += acc;
        mean /= k;

        return new CVResult(foldAccuracies, mean);
    }

    // ──────────────────────────────────────────────
    public static class CVResult {
        public final double[] foldAccuracies;
        public final double   meanAccuracy;

        public CVResult(double[] foldAccuracies, double meanAccuracy) {
            this.foldAccuracies = foldAccuracies;
            this.meanAccuracy   = meanAccuracy;
        }
    }
}
