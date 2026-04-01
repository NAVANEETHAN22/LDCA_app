package com.example.ldca;

import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ldca.ml.*;
import com.example.ldca.utils.*;
import com.example.ldca.results.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class URLCSVActivity extends AppCompatActivity {

    EditText urlInput;
    Button loadBtn, runBtn;
    TextView statusText;
    ProgressBar loadingBar;
    Spinner modelSpinner;

    List<String[]> rawData;
    boolean isAnalysisRunning = false;

    // Must match the limit in other activities
    // Safe Mode: Reduced from 5000 to 2000 for maximum reliability
    static final int MAX_ROWS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_url_csv);

        urlInput     = findViewById(R.id.urlInput);
        loadBtn      = findViewById(R.id.btnLoadURL);
        runBtn       = findViewById(R.id.btnRunURL);
        statusText   = findViewById(R.id.urlStatus);
        loadingBar   = findViewById(R.id.urlLoadingBar);
        modelSpinner = findViewById(R.id.modelSpinner);

        loadBtn.setOnClickListener(v -> loadCSVFromURL());
        runBtn.setOnClickListener(v -> runMLPipeline());

        // Info icon — explains models and auto selection logic
        ImageButton btnModelInfo = findViewById(R.id.btnModelInfo);
        btnModelInfo.setOnClickListener(v -> showModelInfoDialog());

        modelSpinner.setOnTouchListener((v, event) -> {
            if (isAnalysisRunning) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    Toast.makeText(this, "Analysis in progress. Please wait.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
    }

    /* ─── Load CSV from URL ─── */
    void loadCSVFromURL() {
        String url = urlInput.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Enter a URL first", Toast.LENGTH_SHORT).show();
            return;
        }
        loadingBar.setVisibility(View.VISIBLE);
        statusText.setText("Downloading…");

        new Thread(() -> {
            try {
                HttpURLConnection conn =
                        (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(15_000);

                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(conn.getInputStream()));
                List<String[]> data = new ArrayList<>();
                String line;
                int rowCount = 0;
                while ((line = reader.readLine()) != null) {
                    if (rowCount > MAX_ROWS) {
                        break;
                    }
                    String[] row = line.split(",");
                    if (row.length > 50) {
                        String[] pruned = new String[50];
                        System.arraycopy(row, 0, pruned, 0, 50);
                        row = pruned;
                    }
                    data.add(row);
                    rowCount++;
                }
                reader.close();

                rawData = data;
                runOnUiThread(() -> {
                    loadingBar.setVisibility(View.GONE);
                    statusText.setText("Loaded " + (rawData.size() - 1) +
                            " rows, " + rawData.get(0).length + " columns");
                    Toast.makeText(this, "CSV loaded", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    loadingBar.setVisibility(View.GONE);
                    statusText.setText("Error: " + e.getMessage());
                    Toast.makeText(this, "Failed to load CSV", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /* ─── Run ML Pipeline ─── */
    void runMLPipeline() {
        if (rawData == null) {
            Toast.makeText(this, "Load a CSV first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isAnalysisRunning) return;
        isAnalysisRunning = true;
        runBtn.setEnabled(false);
        loadBtn.setEnabled(false);
        loadingBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            // Safe Mode: Run with background priority to prevent System UI starvation
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            try {
                DataSet dataset = DatasetBuilder.build(rawData);
                
                // Extract feature names from CSV header row
                String[] featureNames = null;
                if (rawData != null && rawData.size() > 0) {
                    String[] header = rawData.get(0);
                    featureNames = new String[header.length - 1];
                    System.arraycopy(header, 0, featureNames, 0, header.length - 1);
                }

                // Explicitly nullify rawData to reclaim memory
                rawData = null;
                System.gc(); // Hint GC to clean up immediately

                int rows = dataset.rows;
                int[] predictions = new int[rows];
                double[] scores = new double[rows];

                String selectedModel = modelSpinner.getSelectedItem().toString();
                String autoSelectReason = null;
                if ("Auto Select".equals(selectedModel)) {
                    selectedModel   = AutoModelSelector.chooseModel(dataset);
                    autoSelectReason = AutoModelSelector.getReason(dataset);
                }
                final String finalModelName  = selectedModel;
                final String finalAutoReason = autoSelectReason;
                final String[] finalFeatureNames = featureNames;

                runOnUiThread(() -> statusText.setText("Training " + finalModelName + "..."));

                double[] featureImportances = null;

                if ("Logistic Regression".equals(finalModelName)) {
                    LogisticRegression model = new LogisticRegression();
                    model.train(dataset);
                    for (int i = 0; i < rows; i++) {
                        predictions[i] = model.predict(dataset.X[i]);
                        double dot = 0;
                        for (int j = 0; j < model.weights.length; j++) dot += dataset.X[i][j] * model.weights[j];
                        scores[i] = 1.0 / (1.0 + Math.exp(-dot));
                    }
                    featureImportances = new double[dataset.cols];
                    double maxW = 0;
                    for (double w : model.weights) maxW = Math.max(maxW, Math.abs(w));
                    if (maxW == 0) maxW = 1;
                    for (int j = 0; j < dataset.cols; j++) featureImportances[j] = Math.abs(model.weights[j]) / maxW;

                } else if ("Decision Tree".equals(finalModelName)) {
                    DecisionTree model = new DecisionTree();
                    model.train(dataset);
                    for (int i = 0; i < rows; i++) {
                        predictions[i] = model.predict(dataset.X[i]);
                        double v = dataset.X[i][model.featureIndex];
                        scores[i] = v;
                    }
                    featureImportances = new double[dataset.cols];
                    featureImportances[model.featureIndex] = 1.0;

                } else if ("Random Forest".equals(finalModelName)) {
                    // Reduce complexity: 5 trees instead of 10
                    RandomForest model = new RandomForest(5);
                    model.train(dataset);
                    for (int i = 0; i < rows; i++) {
                        predictions[i] = model.predict(dataset.X[i]);
                        scores[i]      = model.score(dataset.X[i]);
                    }
                    featureImportances = model.featureImportances();

                } else { // Naive Bayes
                    NaiveBayes model = new NaiveBayes();
                    model.train(dataset);
                    for (int i = 0; i < rows; i++) {
                        predictions[i] = model.predict(dataset.X[i]);
                        double[] probs = model.predictProba(dataset.X[i]);
                        scores[i] = probs[1];
                    }
                    featureImportances = new double[dataset.cols];
                    Arrays.fill(featureImportances, 1.0 / dataset.cols);
                }

                int[] actual = dataset.y;
                final double[] finalScores = scores;
                final double[] finalImportances = featureImportances;

                runOnUiThread(() -> {
                    loadingBar.setVisibility(View.GONE);
                    isAnalysisRunning = false;

                    com.example.ldca.utils.DataCache.yTrue = actual;
                    com.example.ldca.utils.DataCache.yPred = predictions;
                    com.example.ldca.utils.DataCache.scores = finalScores;
                    com.example.ldca.utils.DataCache.modelName = finalModelName;
                    com.example.ldca.utils.DataCache.autoSelectReason = finalAutoReason;
                    com.example.ldca.utils.DataCache.featureNames = finalFeatureNames;
                    com.example.ldca.utils.DataCache.featureImportances = finalImportances;
                    com.example.ldca.utils.DataCache.datasetInfo = "Rows: " + rows + "\nFeatures: " + dataset.cols;

                    Intent intent = new Intent(this, ResultActivity.class);
                    startActivity(intent);
                    runBtn.setEnabled(true);
                    loadBtn.setEnabled(true);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    loadingBar.setVisibility(View.GONE);
                    isAnalysisRunning = false;
                    runBtn.setEnabled(true);
                    loadBtn.setEnabled(true);
                    Toast.makeText(this, "Error during analysis", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /* ── Model Info Dialog ── */
    void showModelInfoDialog() {
        String message =
            "📊 MODEL GUIDE\n\n" +
            "🔹 Naive Bayes\n" +
            "  Best for small datasets (< 200 rows).\n" +
            "  Fast, probabilistic, works well with limited samples.\n\n" +
            "🔹 Logistic Regression\n" +
            "  Best for medium datasets with few features.\n" +
            "  Assumes a roughly linear decision boundary.\n\n" +
            "🔹 Decision Tree\n" +
            "  Best for high-dimensional data (> 10 features).\n" +
            "  Handles complex feature splits naturally.\n\n" +
            "🔹 Random Forest\n" +
            "  Best for large datasets (≥ 500 rows, ≤ 10 features).\n" +
            "  Ensemble of trees — more accurate and robust.\n\n" +
            "─────────────────────────────\n" +
            "🤖 AUTO SELECT LOGIC\n\n" +
            "  rows < 200          → Naive Bayes\n" +
            "  cols > 10           → Decision Tree\n" +
            "  rows ≥ 500          → Random Forest\n" +
            "  Otherwise          → Logistic Regression";

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("ℹ Model Information")
            .setMessage(message)
            .setPositiveButton("Got it", null)
            .show();
    }
}
