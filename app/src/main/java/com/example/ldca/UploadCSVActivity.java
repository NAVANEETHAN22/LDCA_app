package com.example.ldca;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ldca.ml.*;
import com.example.ldca.utils.*;
import com.example.ldca.results.*;

import java.io.*;
import java.util.*;

public class UploadCSVActivity extends AppCompatActivity {

    Button chooseFileBtn, runAnalysisBtn;
    TextView datasetInfo, loadingText;
    ProgressBar loadingBar;
    Spinner modelSpinner;
    TableLayout previewTable;

    Uri csvUri;
    List<String[]> rawData;
    DataSet dataset;
    boolean isAnalysisRunning = false;

    // Must match the limit in DatasetBuilder
    // Safe Mode: Reduced from 5000 to 2000 for maximum reliability
    static final int MAX_ROWS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_csv);

        chooseFileBtn  = findViewById(R.id.btnChooseFile);
        runAnalysisBtn = findViewById(R.id.btnRun);
        datasetInfo    = findViewById(R.id.datasetInfo);
        loadingBar     = findViewById(R.id.loadingBar);
        loadingText    = findViewById(R.id.loadingText);
        modelSpinner   = findViewById(R.id.modelSpinner);
        previewTable   = findViewById(R.id.previewTable);

        chooseFileBtn.setOnClickListener(v -> openFilePicker());
        runAnalysisBtn.setOnClickListener(v -> runMLPipeline());

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

    /*
    =========================
    File Picker
    =========================
     */
    ActivityResultLauncher<String[]> filePicker =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri != null) {
                            csvUri = uri;
                            readCSVFile();
                        }
                    });

    void openFilePicker() {
        filePicker.launch(new String[]{"text/*"});
    }

    /*
    =========================
    Read CSV File
    =========================
     */
    void readCSVFile() {
        loadingBar.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        loadingText.setText("Loading CSV... 0%");
        new Thread(() -> {
            try {
                long estimatedSize = 0;
                android.database.Cursor cursor = getContentResolver().query(csvUri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    if (!cursor.isNull(sizeIndex)) {
                        estimatedSize = cursor.getLong(sizeIndex);
                    }
                    cursor.close();
                }

                InputStream inputStream = getContentResolver().openInputStream(csvUri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                List<String[]> tempList = new ArrayList<>();
                String line;
                long charsRead = 0;
                int lastPercent = -1;

                while ((line = reader.readLine()) != null) {
                    if (tempList.size() > MAX_ROWS) {
                        break;
                    }
                    String[] row = line.split(",");
                    // Radical Pruning: cap at 50 columns to save string reference memory
                    if (row.length > 50) {
                        String[] pruned = new String[50];
                        System.arraycopy(row, 0, pruned, 0, 50);
                        row = pruned;
                    }
                    tempList.add(row);

                    if (estimatedSize > 0) {
                        charsRead += line.length() + 1;
                        int percent = (int) ((charsRead * 100) / estimatedSize);
                        if (percent > lastPercent && percent <= 100) {
                            lastPercent = percent;
                            int finalPercent = percent;
                            if (finalPercent % 2 == 0) {
                                runOnUiThread(() -> loadingText.setText("Parsing CSV... " + finalPercent + "%"));
                            }
                        }
                    }
                }
                reader.close();

                runOnUiThread(() -> {
                    rawData = tempList;
                    datasetInfo.setText(
                            "Rows: " + (rawData.size() - 1) +
                                    "\nColumns: " + rawData.get(0).length
                    );
                    showPreview(rawData);
                    loadingBar.setVisibility(View.GONE);
                    loadingText.setVisibility(View.GONE);
                    Toast.makeText(UploadCSVActivity.this, "CSV Loaded", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    loadingBar.setVisibility(View.GONE);
                    loadingText.setVisibility(View.GONE);
                    Toast.makeText(UploadCSVActivity.this, "Error reading file", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /*
    =========================
    Show First 10 Rows
    =========================
     */
    void showPreview(List<String[]> rawData) {
        previewTable.removeAllViews();
        int rows = Math.min(10, rawData.size());
        for (int i = 0; i < rows; i++) {
            TableRow tableRow = new TableRow(this);
            String[] row = rawData.get(i);
            for (String cell : row) {
                TextView tv = new TextView(this);
                tv.setText(cell);
                tv.setPadding(20, 10, 20, 10);
                tv.setBackgroundResource(android.R.drawable.editbox_background);
                tableRow.addView(tv);
            }
            previewTable.addView(tableRow);
        }
    }

    /*
    =========================
    Run ML Pipeline (Threaded)
    =========================
     */
    void runMLPipeline() {
        if (rawData == null) {
            Toast.makeText(this, "Upload CSV first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isAnalysisRunning) return;
        isAnalysisRunning = true;

        runAnalysisBtn.setEnabled(false);
        chooseFileBtn.setEnabled(false);
        loadingBar.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        loadingText.setText("Preparing Analysis...");

        new Thread(() -> {
            // Safe Mode: Run with background priority to prevent System UI starvation
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            try {
                runOnUiThread(() -> loadingText.setText("Building Dataset Matrix..."));
                dataset = DatasetBuilder.build(rawData);
                
                // Extract feature names from CSV header row
                String[] featureNames = null;
                if (rawData != null && rawData.size() > 0) {
                    String[] header = rawData.get(0);
                    // Last column is label — take all but last as feature names
                    featureNames = new String[header.length - 1];
                    System.arraycopy(header, 0, featureNames, 0, header.length - 1);
                }

                // Explicitly nullify rawData as it's no longer needed and can be huge
                rawData = null;
                System.gc(); // Hint to GC to reclaim tempList/rawData if possible

                int rows = dataset.rows;
                int[] predictions = new int[rows];
                double[] scores = new double[rows];  // probability for class 1

                String selectedModelTemp = modelSpinner.getSelectedItem().toString();
                String autoSelectReason  = null;
                if ("Auto Select".equals(selectedModelTemp)) {
                    selectedModelTemp = AutoModelSelector.chooseModel(dataset);
                    autoSelectReason  = AutoModelSelector.getReason(dataset);
                }
                final String finalModelName    = selectedModelTemp;
                final String finalAutoReason   = autoSelectReason;
                final String[] finalFeatureNames = featureNames;

                runOnUiThread(() -> loadingText.setText("Training " + finalModelName + "..."));

                double[] featureImportances = null;

                if ("Logistic Regression".equals(finalModelName)) {
                    LogisticRegression model = new LogisticRegression();
                    model.train(dataset);
                    runOnUiThread(() -> loadingText.setText("Generating Predictions (0%)..."));
                    for (int i = 0; i < rows; i++) {
                        predictions[i] = model.predict(dataset.X[i]);
                        // Use sigmoid output as score (re-compute)
                        double dot = 0;
                        for (int j = 0; j < model.weights.length; j++) dot += dataset.X[i][j] * model.weights[j];
                        scores[i] = 1.0 / (1.0 + Math.exp(-dot));
                        if (i % 2000 == 0) {
                            int finalI = i;
                            runOnUiThread(() -> loadingText.setText("Generating Predictions (" + (finalI * 100 / rows) + "%)..."));
                        }
                    }
                    // Uniform importance for logistic regression (use |weight| as proxy)
                    featureImportances = new double[dataset.cols];
                    double maxW = 0;
                    for (double w : model.weights) maxW = Math.max(maxW, Math.abs(w));
                    if (maxW == 0) maxW = 1;
                    for (int j = 0; j < dataset.cols; j++) featureImportances[j] = Math.abs(model.weights[j]) / maxW;

                } else if ("Decision Tree".equals(finalModelName)) {
                    DecisionTree model = new DecisionTree();
                    model.train(dataset);
                    runOnUiThread(() -> loadingText.setText("Generating Predictions (0%)..."));
                    for (int i = 0; i < rows; i++) {
                        predictions[i] = model.predict(dataset.X[i]);
                        // DT score: just use the feature value directly normalized over range
                        // A rough proxy to get a smooth ROC curve for a stump
                        double v = dataset.X[i][model.featureIndex];
                        scores[i] = v; 
                        if (i % 2000 == 0) {
                            int finalI = i;
                            runOnUiThread(() -> loadingText.setText("Generating Predictions (" + (finalI * 100 / rows) + "%)..."));
                        }
                    }
                    featureImportances = new double[dataset.cols];
                    featureImportances[model.featureIndex] = 1.0;

                } else if ("Random Forest".equals(finalModelName)) {
                    // Reduce complexity: 5 trees instead of 10
                    RandomForest model = new RandomForest(5);
                    model.train(dataset);
                    runOnUiThread(() -> loadingText.setText("Generating Predictions (0%)..."));
                    for (int i = 0; i < rows; i++) {
                        predictions[i] = model.predict(dataset.X[i]);
                        scores[i]      = model.score(dataset.X[i]);
                        if (i % 2000 == 0) {
                            int finalI = i;
                            runOnUiThread(() -> loadingText.setText("Generating Predictions (" + (finalI * 100 / rows) + "%)..."));
                        }
                    }
                    featureImportances = model.featureImportances();

                } else { // Naive Bayes
                    NaiveBayes model = new NaiveBayes();
                    model.train(dataset);
                    runOnUiThread(() -> loadingText.setText("Generating Predictions (0%)..."));
                    for (int i = 0; i < rows; i++) {
                        predictions[i] = model.predict(dataset.X[i]);
                        // Retrieve score (posterior prob) from NB model
                        double[] probs = model.predictProba(dataset.X[i]);
                        scores[i] = probs[1];
                        if (i % 2000 == 0) {
                            int finalI = i;
                            runOnUiThread(() -> loadingText.setText("Generating Predictions (" + (finalI * 100 / rows) + "%)..."));
                        }
                    }
                    // Uniform importance for NB
                    featureImportances = new double[dataset.cols];
                    java.util.Arrays.fill(featureImportances, 1.0 / dataset.cols);
                }

                int[] actual = dataset.y;
                final double[] finalScores = scores;
                final double[] finalImportances = featureImportances;

                runOnUiThread(() -> {
                    loadingBar.setVisibility(View.GONE);
                    loadingText.setVisibility(View.GONE);
                    isAnalysisRunning = false;

                    Intent intent = new Intent(this, ResultActivity.class);
                    com.example.ldca.utils.DataCache.yTrue = actual;
                    com.example.ldca.utils.DataCache.yPred = predictions;
                    com.example.ldca.utils.DataCache.scores = finalScores;
                    com.example.ldca.utils.DataCache.modelName = finalModelName;
                    com.example.ldca.utils.DataCache.autoSelectReason = finalAutoReason;
                    com.example.ldca.utils.DataCache.featureNames = finalFeatureNames;
                    com.example.ldca.utils.DataCache.featureImportances = finalImportances;
                    com.example.ldca.utils.DataCache.datasetInfo = "Rows: " + rows + "\nFeatures: " + dataset.cols;

                    startActivity(intent);
                    runAnalysisBtn.setEnabled(true);
                    chooseFileBtn.setEnabled(true);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    loadingBar.setVisibility(View.GONE);
                    loadingText.setVisibility(View.GONE);
                    isAnalysisRunning = false;
                    runAnalysisBtn.setEnabled(true);
                    chooseFileBtn.setEnabled(true);
                    Toast.makeText(this, "Error during analysis", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /* ──────────────────────────────────────────────────────────
       Model Info Dialog
    ────────────────────────────────────────────────────────── */
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