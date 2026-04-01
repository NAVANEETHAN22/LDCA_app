package com.example.ldca.results;

import com.example.ldca.ml.*;

public class ResultEngine {

    /** Original (backward-compatible) evaluate — no scores, no feature data. */
    public static ResultBundle evaluate(int[] yTrue, int[] yPred) {
        ResultBundle bundle = new ResultBundle();
        bundle.accuracy  = Metrics.accuracy(yTrue, yPred);
        bundle.confusion = Metrics.confusionMatrix(yTrue, yPred);
        bundle.precision = Metrics.precision(bundle.confusion);
        bundle.recall    = Metrics.recall(bundle.confusion);
        bundle.f1        = Metrics.f1(bundle.precision, bundle.recall);
        return bundle;
    }

    /**
     * Full evaluate — also runs cross-validation, computes ROC/PR curves,
     * and fills in feature importance.
     *
     * @param yTrue        ground-truth labels
     * @param yPred        hard predictions from the trained model
     * @param scores       continuous probability / confidence for class-1
     *                     (pass null to skip ROC/PR curves)
     * @param data         full dataset (for cross-validation)
     * @param modelName    chosen model name
     * @param featureNames CSV header names (may be null)
     * @param importances  per-feature import array from the model (may be null)
     */
    public static ResultBundle evaluate(int[]    yTrue,
                                        int[]    yPred,
                                        double[] scores,
                                        DataSet  data,
                                        String   modelName,
                                        String[] featureNames,
                                        double[] importances) {

        // ── Base metrics ─────────────────────────────────────────────────
        ResultBundle bundle = evaluate(yTrue, yPred);

        // ── Cross-validation (5-fold) ─────────────────────────────────────
        try {
            CrossValidation.CVResult cv = CrossValidation.run(data, modelName, 5);
            bundle.cvFoldAccuracies = cv.foldAccuracies;
            bundle.cvMeanAccuracy   = cv.meanAccuracy;
        } catch (Exception ignored) { }

        // ── ROC & PR curves ───────────────────────────────────────────────
        if (scores != null) {
            try {
                float[][] roc = Metrics.rocCurve(yTrue, scores);
                bundle.rocFpr  = roc[0];
                bundle.rocTpr  = roc[1];
                bundle.aucScore = Metrics.auc(bundle.rocFpr, bundle.rocTpr);

                float[][] pr = Metrics.prCurve(yTrue, scores);
                bundle.prRecall    = pr[0];
                bundle.prPrecision = pr[1];
            } catch (Exception ignored) { }
        }

        // ── Feature importance ────────────────────────────────────────────
        bundle.featureImportances = importances;
        bundle.featureNames       = featureNames;

        return bundle;
    }
}
