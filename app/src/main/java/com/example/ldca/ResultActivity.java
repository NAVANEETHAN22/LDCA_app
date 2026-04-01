package com.example.ldca;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.example.ldca.results.ResultBundle;
import com.example.ldca.results.ResultEngine;
import com.example.ldca.results.ConfusionMatrixView;
import com.example.ldca.results.MetricChart;
import com.example.ldca.results.ClassDistributionChart;
import com.example.ldca.results.PredictionTable;
import com.example.ldca.results.PDFReportGenerator;
import com.example.ldca.results.ROCCurveView;
import com.example.ldca.results.PRCurveView;
import com.example.ldca.results.FeatureImportanceChart;
import com.example.ldca.results.CrossValidationView;

public class ResultActivity extends AppCompatActivity {

    TextView accuracyText, precisionText, recallText, f1Text, cvMeanText;
    TextView autoSelectReasonText;
    LinearLayout matrixContainer, predictionContainer;
    LinearLayout cvContainer, rocContainer, prContainer, fiContainer;

    BarChart metricChart;
    PieChart classChart;

    Button downloadBtn;

    ProgressBar computingBar;
    TextView    computingText;

    int[]    yTrue;
    int[]    yPred;
    double[] scores;
    double[] featureImportances;
    String[] featureNames;

    String datasetInfo  = "";
    String modelName    = "Unknown";
    String autoSelectReason = null;

    ResultBundle results;

    // Custom views captured for PDF
    ROCCurveView          rocView;
    PRCurveView           prView;
    FeatureImportanceChart fiView;
    CrossValidationView   cvView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        fiView  = null;
        cvView  = null;

        // ── Bind views ───────────────────────────────────────────────────
        accuracyText         = findViewById(R.id.accuracyText);
        precisionText        = findViewById(R.id.precisionText);
        recallText           = findViewById(R.id.recallText);
        f1Text               = findViewById(R.id.f1Text);
        cvMeanText           = findViewById(R.id.cvMeanText);
        autoSelectReasonText = findViewById(R.id.autoSelectReasonText);
        matrixContainer      = findViewById(R.id.matrixContainer);
        predictionContainer  = findViewById(R.id.predictionContainer);
        cvContainer          = findViewById(R.id.cvContainer);
        rocContainer         = findViewById(R.id.rocContainer);
        prContainer          = findViewById(R.id.prContainer);
        fiContainer          = findViewById(R.id.fiContainer);
        metricChart          = findViewById(R.id.metricChart);
        classChart           = findViewById(R.id.classChart);
        
        // Disable text for stability while loading
        if (metricChart != null) metricChart.setNoDataText("Preparing chart...");
        if (classChart != null) classChart.setNoDataText("Preparing chart...");

        downloadBtn          = findViewById(R.id.downloadReport);
        computingBar         = findViewById(R.id.computingProgressBar);
        computingText        = findViewById(R.id.computingText);

        // ── Receive data from Memory Cache (Bypass IPC constraints) ───────
        yTrue              = com.example.ldca.utils.DataCache.yTrue;
        yPred              = com.example.ldca.utils.DataCache.yPred;
        scores             = com.example.ldca.utils.DataCache.scores;
        featureImportances = com.example.ldca.utils.DataCache.featureImportances;
        featureNames       = com.example.ldca.utils.DataCache.featureNames;
        datasetInfo        = com.example.ldca.utils.DataCache.datasetInfo;
        modelName          = com.example.ldca.utils.DataCache.modelName;
        autoSelectReason   = com.example.ldca.utils.DataCache.autoSelectReason;
        if (modelName == null) modelName = "Naive Bayes";

        if (yTrue == null || yPred == null) {
            Toast.makeText(this, "No result data received", Toast.LENGTH_LONG).show();
            return;
        }

        // Show loading indication while computing
        showComputing(true);
        downloadBtn.setEnabled(false);

        // ── Run all heavy computation OFF the UI thread ───────────────────
        final int[]    finalYTrue  = yTrue;
        final int[]    finalYPred  = yPred;
        final double[] finalScores = scores;
        final String   finalModel  = modelName;
        final double[] finalImp    = featureImportances;
        final String[] finalNames  = featureNames;

        new Thread(() -> {
            // Safe Mode: Background priority
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            ResultBundle r;
            try {
                r = ResultEngine.evaluate(
                        finalYTrue, finalYPred,
                        finalScores,
                        null,          // DataSet: CV skipped gracefully via guard in CrossValidation
                        finalModel,
                        finalNames,
                        finalImp
                );
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    showComputing(false);
                    Toast.makeText(this,
                            "Error computing results: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
                return;
            }

            final ResultBundle bundle = r;
            // Radical Stability: add a small delay before populating UI to let memory settle
            runOnUiThread(() -> {
                getWindow().getDecorView().postDelayed(() -> populateResults(bundle), 200);
            });
        }).start();
    }

    /** Fills all UI views from the result bundle. Called on the UI thread. */
    private void populateResults(ResultBundle r) {
        this.results = r;
        showComputing(false);
        downloadBtn.setEnabled(true);

        // ── Base metrics ──────────────────────────────────────────────────
        accuracyText.setText(String.format("Accuracy:  %.4f", r.accuracy));
        precisionText.setText(String.format("Precision: %.4f", r.precision));
        recallText.setText(String.format("Recall:    %.4f", r.recall));
        f1Text.setText(String.format("F1 Score:  %.4f", r.f1));

        // ── Auto-select reason banner ─────────────────────────────────────
        if (autoSelectReason != null) {
            autoSelectReasonText.setVisibility(View.VISIBLE);
            autoSelectReasonText.setText("🤖 Auto-Selected: " + autoSelectReason);
        }

        // ── Confusion Matrix ──────────────────────────────────────────────
        if (r.confusion != null) {
            TableLayout matrix = ConfusionMatrixView.generate(this, r.confusion);
            matrixContainer.removeAllViews();
            matrixContainer.addView(matrix);
        }

        // ── Metric Bar Chart ──────────────────────────────────────────────
        MetricChart.showMetrics(metricChart, r);

        // ── Class Distribution ────────────────────────────────────────────
        ClassDistributionChart.show(classChart, yTrue);

        // ── Cross-Validation ──────────────────────────────────────────────
        if (r.cvFoldAccuracies != null) {
            cvMeanText.setText(String.format("CV Mean Accuracy: %.4f", r.cvMeanAccuracy));
            cvView = new CrossValidationView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            cvView.setLayoutParams(lp);
            cvView.setData(r.cvFoldAccuracies, r.cvMeanAccuracy);
            cvContainer.removeAllViews();
            cvContainer.addView(cvView);
        } else {
            cvMeanText.setText("(Cross-validation not available for this dataset)");
        }

        // ── ROC Curve ─────────────────────────────────────────────────────
        if (r.rocFpr != null && r.rocTpr != null) {
            rocView = new ROCCurveView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            rocView.setLayoutParams(lp);
            rocView.setData(r.rocFpr, r.rocTpr, r.aucScore);
            rocContainer.removeAllViews();
            rocContainer.addView(rocView);
        }

        // ── Precision-Recall Curve ────────────────────────────────────────
        if (r.prRecall != null && r.prPrecision != null) {
            prView = new PRCurveView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            prView.setLayoutParams(lp);
            prView.setData(r.prRecall, r.prPrecision);
            prContainer.removeAllViews();
            prContainer.addView(prView);
        }

        // ── Feature Importance ────────────────────────────────────────────
        if (r.featureImportances != null && r.featureImportances.length > 0) {
            fiView = new FeatureImportanceChart(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            fiView.setLayoutParams(lp);
            fiView.setData(r.featureImportances, r.featureNames);
            fiContainer.removeAllViews();
            fiContainer.addView(fiView);
        }

        // ── Prediction Table ──────────────────────────────────────────────
        TableLayout predTable = PredictionTable.generate(this, yTrue, yPred);
        predictionContainer.removeAllViews();
        predictionContainer.addView(predTable);
        if (yTrue.length > 100) {
            TextView note = new TextView(this);
            note.setText("Showing first 100 of " + yTrue.length + " predictions.");
            note.setPadding(8, 8, 8, 8);
            predictionContainer.addView(note);
        }

        // ── Download PDF ──────────────────────────────────────────────────
        downloadBtn.setOnClickListener(v -> generatePDF());
    }

    private void showComputing(boolean show) {
        if (computingBar != null)
            computingBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (computingText != null)
            computingText.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void generatePDF() {
        showComputing(true);
        downloadBtn.setEnabled(false);

        java.io.File downloadsDir = android.os.Environment
                .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
        String fileName = "LDCA_Report_" + System.currentTimeMillis() + ".pdf";
        final String path = new java.io.File(downloadsDir, fileName).getAbsolutePath();

        byte[] tmpMetricImg = null, tmpClassImg = null;
        byte[] tmpRocImg = null, tmpPrImg = null, tmpFiImg = null, tmpCvImg = null;

        try {
            android.graphics.Bitmap bmp;

            bmp = metricChart.getChartBitmap();
            tmpMetricImg = toBytes(bmp);

            bmp = classChart.getChartBitmap();
            tmpClassImg = toBytes(bmp);

            if (rocView != null) tmpRocImg = viewToBytes(rocView, rocView.getWidth(), rocView.getHeight());
            if (prView  != null) tmpPrImg  = viewToBytes(prView,  prView.getWidth(),  prView.getHeight());
            if (fiView  != null) {
                int h = fiView.getHeight();
                if (h <= 0) h = (int)(getResources().getDisplayMetrics().density * 600);
                tmpFiImg = viewToBytes(fiView, fiView.getWidth(), h);
            }
            if (cvView  != null) tmpCvImg  = viewToBytes(cvView,  cvView.getWidth(),  cvView.getHeight());

        } catch (Exception e) {
            e.printStackTrace();
            showComputing(false);
            downloadBtn.setEnabled(true);
            Toast.makeText(this, "Failed to capture charts for PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        final byte[] metricImg = tmpMetricImg;
        final byte[] classImg = tmpClassImg;
        final byte[] rocImg = tmpRocImg;
        final byte[] prImg = tmpPrImg;
        final byte[] fiImg = tmpFiImg;
        final byte[] cvImg = tmpCvImg;

        new Thread(() -> {
            // Safe Mode: Background priority
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            try {
                PDFReportGenerator.generate(
                        this, path, datasetInfo, modelName, results,
                        metricImg, classImg, rocImg, prImg, fiImg, cvImg
                );
                runOnUiThread(() -> {
                    showComputing(false);
                    downloadBtn.setEnabled(true);
                    Toast.makeText(this, "Report saved: " + path, Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    showComputing(false);
                    downloadBtn.setEnabled(true);
                    Toast.makeText(this, "Error generating PDF report", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear the data cache when result screen is closed to free up memory
        com.example.ldca.utils.DataCache.clear();
    }

    private byte[] toBytes(android.graphics.Bitmap bmp) {
        java.io.ByteArrayOutputStream s = new java.io.ByteArrayOutputStream();
        bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, s);
        return s.toByteArray();
    }

    private byte[] viewToBytes(android.view.View view, int w, int h) {
        if (w <= 0) w = 800;
        if (h <= 0) h = 400;
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                w, h, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas c = new android.graphics.Canvas(bmp);
        c.drawColor(android.graphics.Color.WHITE);
        view.draw(c);
        return toBytes(bmp);
    }
}