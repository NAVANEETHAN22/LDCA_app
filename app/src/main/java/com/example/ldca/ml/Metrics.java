package com.example.ldca.ml;

import java.util.Arrays;

public class Metrics {

    public static double accuracy(int[] yTrue, int[] yPred) {
        int correct = 0;
        for (int i = 0; i < yTrue.length; i++)
            if (yTrue[i] == yPred[i]) correct++;
        return (double) correct / yTrue.length;
    }

    public static int[][] confusionMatrix(int[] yTrue, int[] yPred) {
        int maxClass = 0;
        for (int v : yTrue) if (v > maxClass) maxClass = v;
        for (int v : yPred) if (v > maxClass) maxClass = v;

        int size = maxClass + 1;
        int[][] matrix = new int[size][size];
        for (int i = 0; i < yTrue.length; i++) {
            int actual    = yTrue[i];
            int predicted = yPred[i];
            if (actual < size && predicted < size)
                matrix[actual][predicted]++;
        }
        return matrix;
    }

    public static double precision(int[][] m) {
        return (double) m[1][1] / (m[1][1] + m[0][1] + 1e-9);
    }

    public static double recall(int[][] m) {
        return (double) m[1][1] / (m[1][1] + m[1][0] + 1e-9);
    }

    public static double f1(double p, double r) {
        return 2 * p * r / (p + r + 1e-9);
    }

    // ──────────────────────────────────────────────
    // ROC Curve
    // ──────────────────────────────────────────────
    /**
     * Computes the ROC curve data points.
     *
     * @param yTrue  ground truth binary labels (0 or 1)
     * @param scores continuous probability/confidence scores for class 1
     * @return float[2][] where [0] = FPR array, [1] = TPR array
     */
    public static float[][] rocCurve(int[] yTrue, double[] scores) {
        int n = yTrue.length;

        // Sort indices by descending score
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(scores[b], scores[a]));

        int totalPos = 0, totalNeg = 0;
        for (int y : yTrue) {
            if (y == 1) totalPos++;
            else totalNeg++;
        }
        if (totalPos == 0 || totalNeg == 0)
            return new float[][]{{0f, 1f}, {0f, 1f}};

        float[] fprArr = new float[n + 2];
        float[] tprArr = new float[n + 2];
        int tp = 0, fp = 0;

        fprArr[0] = 0f;
        tprArr[0] = 0f;

        for (int i = 0; i < n; i++) {
            if (yTrue[idx[i]] == 1) tp++;
            else fp++;
            fprArr[i + 1] = (float) fp / totalNeg;
            tprArr[i + 1] = (float) tp / totalPos;
        }
        fprArr[n + 1] = 1f;
        tprArr[n + 1] = 1f;

        return downsampleCurve(new float[][]{fprArr, tprArr}, 500);
    }

    /** Area Under ROC Curve (trapezoidal rule). */
    public static double auc(float[] fpr, float[] tpr) {
        double area = 0;
        for (int i = 1; i < fpr.length; i++) {
            area += (fpr[i] - fpr[i - 1]) * (tpr[i] + tpr[i - 1]) / 2.0;
        }
        return Math.abs(area);
    }

    // ──────────────────────────────────────────────
    // Precision-Recall Curve
    // ──────────────────────────────────────────────
    /**
     * Computes the Precision-Recall curve data points.
     *
     * @param yTrue  ground truth binary labels
     * @param scores continuous probability/confidence scores for class 1
     * @return float[2][] where [0] = Recall array, [1] = Precision array
     */
    public static float[][] prCurve(int[] yTrue, double[] scores) {
        int n = yTrue.length;

        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(scores[b], scores[a]));

        float[] recArr  = new float[n + 1];
        float[] precArr = new float[n + 1];

        int tp = 0;
        int totalPos = 0;
        for (int y : yTrue) if (y == 1) totalPos++;
        if (totalPos == 0) return new float[][]{{0f, 1f}, {1f, 0f}};

        for (int i = 0; i < n; i++) {
            if (yTrue[idx[i]] == 1) tp++;
            recArr[i]  = (float) tp / totalPos;
            precArr[i] = (float) tp / (i + 1);
        }
        recArr[n]  = 1f;
        precArr[n] = (float) totalPos / n;

        return downsampleCurve(new float[][]{recArr, precArr}, 500);
    }

    private static float[][] downsampleCurve(float[][] curve, int maxPoints) {
        int n = curve[0].length;
        if (n <= maxPoints) return curve;

        float[] x = curve[0];
        float[] y = curve[1];
        float[] newX = new float[maxPoints];
        float[] newY = new float[maxPoints];

        for (int i = 0; i < maxPoints; i++) {
            int idx = (int) ((long) i * (n - 1) / (maxPoints - 1));
            newX[i] = x[idx];
            newY[i] = y[idx];
        }
        return new float[][]{newX, newY};
    }
}
