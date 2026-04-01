package com.example.ldca.results;

public class ResultBundle {

    // ── Original fields ──────────────────────────
    public double accuracy;
    public double precision;
    public double recall;
    public double f1;
    public int[][] confusion;

    // ── Cross-Validation ─────────────────────────
    public double[] cvFoldAccuracies;
    public double   cvMeanAccuracy;

    // ── ROC Curve ────────────────────────────────
    public float[] rocFpr;   // x-axis (False Positive Rate)
    public float[] rocTpr;   // y-axis (True Positive Rate)
    public double  aucScore;

    // ── Precision-Recall Curve ───────────────────
    public float[] prRecall;     // x-axis
    public float[] prPrecision;  // y-axis

    // ── Feature Importance ───────────────────────
    public double[] featureImportances;
    public String[] featureNames;

}