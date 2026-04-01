package com.example.ldca.utils;

public class DataCache {
    public static int[] yTrue;
    public static int[] yPred;
    public static double[] scores;
    public static double[] featureImportances;
    public static String[] featureNames;
    public static String datasetInfo;
    public static String modelName;
    public static String autoSelectReason;

    public static void clear() {
        yTrue = null;
        yPred = null;
        scores = null;
        featureImportances = null;
        featureNames = null;
        datasetInfo = null;
        modelName = null;
        autoSelectReason = null;
    }
}
