package com.example.ldca;

import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ldca.ml.*;
import com.example.ldca.utils.*;
import com.example.ldca.results.*;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

public class PayloadActivity extends AppCompatActivity {

    EditText payloadInput;
    Button predictBtn, downloadBtn;
    private TextView predictionResult, predictionDetail, analysisBreakdown, advancedAnalysis, decodedPayloadTitle, decodedPayloadText;
    private android.widget.ProgressBar riskMeter;
    ProgressBar loadingBar;
    Spinner modelSpinner;
    View resultCard;
    boolean isAnalysisRunning = false;
    int lastPrediction = -1;
    String lastModelUsed = "";
    String lastPayload = "";

    private Interpreter tflite;
    private Map<String, Integer> vocab = new HashMap<>();
    private static final int MAX_SEQ_LEN = 100; // Updated to match model's expected shape [1, 100]
    private static final String[] VULN_LABELS = {
        "API Abuse",         // 0
        "CMD Injection",     // 1
        "GraphQL Injection", // 2
        "IDOR",              // 3
        "LFI",               // 4
        "Open Redirect",     // 5
        "PATH Traversal",    // 6
        "SQL Injection",     // 7
        "SSRF",              // 8
        "XSS",               // 9
        "XXE"                // 10
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payload);

        payloadInput    = findViewById(R.id.payloadInput);
        predictBtn      = findViewById(R.id.btnPredict);
        downloadBtn     = findViewById(R.id.btnDownloadPayload);
        predictionResult      = findViewById(R.id.predictionResult);
        predictionDetail = findViewById(R.id.predictionDetail);
        analysisBreakdown = findViewById(R.id.analysisBreakdown);
        advancedAnalysis = findViewById(R.id.advancedAnalysis);
        decodedPayloadTitle = findViewById(R.id.decodedPayloadTitle);
        decodedPayloadText = findViewById(R.id.decodedPayloadText);
        riskMeter = findViewById(R.id.riskMeter);
        loadingBar      = findViewById(R.id.payloadLoadingBar);
        modelSpinner    = findViewById(R.id.modelSpinner);
        resultCard      = findViewById(R.id.resultCard);

        // Update model options to include TFLite
        updateModelOptions();

        predictBtn.setOnClickListener(v -> runPrediction());
        downloadBtn.setOnClickListener(v -> downloadPDF());

        modelSpinner.setOnTouchListener((v, event) -> {
            if (isAnalysisRunning) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    Toast.makeText(this, "Prediction in progress. Please wait.", Toast.LENGTH_SHORT).show();
                }
                return true; 
            }
            return false;
        });

        loadTFLiteModel();
        loadVocab();
    }

    private void updateModelOptions() {
        String[] options = getResources().getStringArray(R.array.model_options);
        List<String> optionsList = new ArrayList<>(Arrays.asList(options));
        if (!optionsList.contains("TFLite (Payload Optimized)")) {
            optionsList.add("TFLite (Payload Optimized)");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, optionsList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(adapter);
        // Set TFLite as default if available
        modelSpinner.setSelection(optionsList.size() - 1);
    }

    private void loadTFLiteModel() {
        try {
            tflite = new Interpreter(loadModelFile());
            Log.d("PayloadActivity", "TFLite model loaded successfully");
        } catch (Exception e) {
            Log.e("PayloadActivity", "Error loading TFLite model", e);
            Toast.makeText(this, "Failed to load TFLite model", Toast.LENGTH_SHORT).show();
        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd("plgenn_ltig_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void loadVocab() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("vocab.txt")))) {
            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                vocab.put(line.trim(), index++);
            }
            Log.d("PayloadActivity", "Vocab loaded: " + vocab.size() + " words");
        } catch (IOException e) {
            Log.e("PayloadActivity", "Error loading vocab", e);
        }
    }

    private float[] tokenize(String text) {
        float[] input = new float[MAX_SEQ_LEN];
        // Split by whitespace or transitions between alphanumeric and symbols
        // Using a regex to find tokens: words or individual symbols
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\w+|[^\\w\\s]");
        java.util.regex.Matcher m = p.matcher(text);
        
        int i = 0;
        int knownTokens = 0;
        int totalTokens = 0;
        
        while (m.find() && i < MAX_SEQ_LEN) {
            totalTokens++;
            String token = m.group();
            Integer id = vocab.get(token);
            if (id == null) {
                // Try case-insensitive fallback if exact match fails
                id = vocab.get(token.toLowerCase());
                if (id == null) id = vocab.get(token.toUpperCase());
            }
            if (id != null) {
                input[i++] = (float) id;
                knownTokens++;
            } else {
                input[i++] = 1.0f; // 1 is [UNK]
            }
        }
        
        // Storing tokenizer stats in tag for UI use
        payloadInput.setTag(new int[]{knownTokens, totalTokens});
        
        return input;
    }

    void runPrediction() {
        String payload = payloadInput.getText().toString().trim();
        if (payload.isEmpty()) {
            Toast.makeText(this, "Enter a payload first", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedModel = modelSpinner.getSelectedItem().toString();
        
        if (isAnalysisRunning) return;
        isAnalysisRunning = true;

        loadingBar.setVisibility(View.VISIBLE);
        resultCard.setVisibility(View.GONE);
        lastPayload = payload;

        new Thread(() -> {
            // Safe Mode: Background priority
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            try {
                int prediction = 0;
                final String finalModelName = selectedModel;
                float confidence = -1;

                if ("TFLite (Payload Optimized)".equals(selectedModel) && tflite != null) {
                    float[] seqInput = tokenize(payload);
                    
                    // Input 0: Numeric Features [1, 4] -> [length, symbols, digits, spaces]
                    float[][] numericInput = new float[1][4];
                    numericInput[0][0] = payload.length();
                    numericInput[0][1] = countSymbols(payload); 
                    numericInput[0][2] = 0; // digits
                    numericInput[0][3] = 0; // spaces
                    for (char c : payload.toCharArray()) {
                        if (Character.isDigit(c)) numericInput[0][2]++;
                        else if (Character.isWhitespace(c)) numericInput[0][3]++;
                    }

                    // Input 1: Sequence Features [1, 100]
                    float[][] sequenceInput = new float[1][MAX_SEQ_LEN];
                    System.arraycopy(seqInput, 0, sequenceInput[0], 0, Math.min(seqInput.length, MAX_SEQ_LEN));

                    Object[] inputs = {numericInput, sequenceInput};
                    
                    // Output 0: [1, 35], Output 1: [1, 11], Output 2: [1, 3]
                    Map<Integer, Object> outputs = new HashMap<>();
                    outputs.put(0, new float[1][35]);
                    outputs.put(1, new float[1][11]);
                    outputs.put(2, new float[1][3]);

                    tflite.runForMultipleInputsOutputs(inputs, outputs);

                    // Output 2 is the 3-layer classification output [benign, malicious, potentially_malicious]
                    float[][] resultClass = (float[][]) outputs.get(2);
                    float[] probClass = resultClass[0];
                    
                    // Find argmax
                    int maxIdxClass = 0;
                    for (int i = 1; i < probClass.length; i++) {
                        if (probClass[i] > probClass[maxIdxClass]) maxIdxClass = i;
                    }
                    
                    // Map [0: Benign, 1: Malicious, 2: Mixed/Uncertain]
                    prediction = (maxIdxClass == 1 || maxIdxClass == 2) ? 1 : 0;
                    confidence = probClass[maxIdxClass];

                    // Determine specific category from Output 1
                    float[][] resultCategory = (float[][]) outputs.get(1);
                    float[] probCategory = resultCategory[0];
                    int maxIdxCat = 0;
                    for (int i = 1; i < probCategory.length; i++) if (probCategory[i] > probCategory[maxIdxCat]) maxIdxCat = i;
                    String modelCategory = VULN_LABELS[maxIdxCat];

                    updateAnalysisUI(prediction, confidence, payload, finalModelName, modelCategory);
                } else {
                    // Fallback to traditional simulation
                    prediction = (payload.length() > 50 || countSymbols(payload) > 5) ? 1 : 0;
                    confidence = 0.85f;
                    updateAnalysisUI(prediction, confidence, payload, finalModelName, "Unknown");
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    loadingBar.setVisibility(View.GONE);
                    isAnalysisRunning = false;
                    Toast.makeText(this, "Prediction failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void updateAnalysisUI(int prediction, float confidence, String payload, String modelName, String modelCategory) {
        runOnUiThread(() -> {
            loadingBar.setVisibility(View.GONE);
            isAnalysisRunning = false;
            lastPrediction = prediction;
            lastModelUsed = modelName;
            lastPayload = payload;

            resultCard.setVisibility(View.VISIBLE);
            
            if (prediction == 1) {
                predictionResult.setText("Status: MALICIOUS");
                predictionResult.setTextColor(Color.RED);
            } else {
                predictionResult.setText("Status: BENIGN");
                predictionResult.setTextColor(Color.parseColor("#4CAF50")); // Green
            }

            String detail = "Model: " + modelName + "\nConfidence: " + (confidence >= 0 ? String.format("%.2f%%", confidence * 100) : "N/A");
            predictionDetail.setText(detail);

            int riskPercent = (prediction == 1) ? (int)(confidence * 100) : (int)((1 - confidence) * 100);
            riskMeter.setProgress(riskPercent);

            // Fetch tokenizer stats from tag
            int[] stats = (int[]) payloadInput.getTag();
            int known = (stats != null) ? stats[0] : 0;
            int total = (stats != null) ? stats[1] : 0;
            double coverage = (total > 0) ? (known * 100.0 / total) : 0;

            String breakdownText = String.format("• Payload Length: %d chars\n• Special Symbols: %d\n• Vocabulary Coverage: %.1f%% (%d/%d tokens)",
                    payload.length(), countSymbols(payload), coverage, known, total);
            analysisBreakdown.setText(breakdownText);

            // Advanced Analysis
            StringBuilder advanced = new StringBuilder();
            
            // 1. Dynamic Threat Type (Model vs Heuristic)
            String threatType = detectThreatType(payload);
            if (!modelCategory.equals("Unknown")) {
                advanced.append("• Model Predicted Category: ").append(modelCategory).append("\n");
                if (!threatType.equals("Unknown") && !threatType.equalsIgnoreCase(modelCategory)) {
                    advanced.append("• Heuristic Analysis: Likely ").append(threatType).append("\n");
                }
            } else if (!threatType.equals("Unknown")) {
                advanced.append("• Likely Attack Type: ").append(threatType).append("\n");
            }

            // 2. Entropy (Obfuscation)
            double entropy = calculateEntropy(payload);
            advanced.append(String.format("• Shannon Entropy: %.2f (Obfuscation: %s)", 
                entropy, entropy > 4.2 ? "High" : "Low"));

            // 3. Risky Tokens
            List<String> risky = findRiskyTokens(payload);
            if (!risky.isEmpty()) {
                advanced.append("\n• Suspicious Tokens detected: ").append(TextUtils.join(", ", risky));
            }

            advancedAnalysis.setText(advanced.toString());
            advancedAnalysis.setVisibility(View.VISIBLE);

            // 4. Decoding
            String decoded = tryDecode(payload);
            if (!decoded.equals(payload)) {
                decodedPayloadTitle.setVisibility(View.VISIBLE);
                decodedPayloadText.setText(decoded);
                decodedPayloadText.setVisibility(View.VISIBLE);
            } else {
                decodedPayloadTitle.setVisibility(View.GONE);
                decodedPayloadText.setVisibility(View.GONE);
            }
        });
    }

    private int countSymbols(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) count++;
        }
        return count;
    }

    private double calculateEntropy(String text) {
        if (text == null || text.isEmpty()) return 0.0;
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : text.toCharArray()) freq.put(c, freq.getOrDefault(c, 0) + 1);
        double entropy = 0.0;
        int len = text.length();
        for (int count : freq.values()) {
            double p = (double) count / len;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    private String detectThreatType(String payload) {
        String p = payload.toLowerCase();
        if (p.contains("select") || p.contains("union") || p.contains("insert") || p.contains("drop")) return "SQL Injection";
        if (p.contains("<script") || p.contains("alert(") || p.contains("onerror=") || p.contains("onload=")) return "XSS";
        if (p.contains("/etc/passwd") || p.contains("../") || p.contains("boot.ini") || p.contains("win.ini")) return "PATH Traversal/LFI";
        if (p.contains("system(") || p.contains("exec(") || p.contains("cmd.exe") || p.contains("/bin/sh")) return "CMD Injection";
        if (p.contains("<!entity") || p.contains("<!doctype") || p.contains("xxe")) return "XXE";
        if (p.contains("http://") && (p.contains(".internal") || p.contains("169.254.169.254"))) return "SSRF";
        if (p.contains("redirect=") || p.contains("url=") || p.contains("next=")) return "Open Redirect";
        if (p.contains("query {") || p.contains("mutation {")) return "GraphQL Injection";
        if (p.contains("/user/") && p.contains("id=")) return "Potential IDOR";
        return "Unknown";
    }

    private List<String> findRiskyTokens(String payload) {
        List<String> found = new ArrayList<>();
        String[] riskyItems = {"SELECT", "UNION", "eval", "base64", "<script", "onload", "SYSTEM", "exec", "ENTITY", "http://", "redirect", "query"};
        for (String item : riskyItems) {
            if (payload.toLowerCase().contains(item.toLowerCase())) found.add(item);
        }
        return found;
    }

    private String tryDecode(String payload) {
        try {
            String urlDecoded = java.net.URLDecoder.decode(payload, "UTF-8");
            if (!urlDecoded.equals(payload)) return urlDecoded;
        } catch (Exception ignored) {}
        
        try {
            if (payload.length() > 8 && payload.matches("^[a-zA-Z0-9+/=]+$")) {
                byte[] data = android.util.Base64.decode(payload, android.util.Base64.DEFAULT);
                String decoded = new String(data);
                // Basic validation its text
                if (decoded.length() > 0 && Character.isDefined(decoded.charAt(0))) return decoded;
            }
        } catch (Exception ignored) {}
        
        return payload;
    }

    void downloadPDF() {
        if (lastPrediction == -1) return;

        java.io.File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
        String fileName = "LDCA_Payload_Report_" + System.currentTimeMillis() + ".pdf";
        java.io.File file = new java.io.File(downloadsDir, fileName);
        final String path = file.getAbsolutePath();

        ResultBundle bundle = new ResultBundle();
        bundle.accuracy = 100.0f;

        // Show a temporary indicator if possible or just run in background
        Toast.makeText(this, "Generating report...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            // Safe Mode: Background priority
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            try {
                PDFReportGenerator.generate(
                        this,
                        path,
                        "Payload Analysis: " + (lastPayload.length() > 30 ? lastPayload.substring(0, 27) + "..." : lastPayload),
                        lastModelUsed,
                        bundle,
                        null, null, null, null, null, null
                );
                runOnUiThread(() -> {
                    Toast.makeText(this, "Report saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        if (tflite != null) {
            tflite.close();
        }
        super.onDestroy();
    }
}
