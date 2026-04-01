package com.example.ldca.utils;

import com.example.ldca.ml.DataSet;
import java.util.*;

public class DatasetBuilder {

    public static DataSet build(List<String[]> raw) {
        // Safe Mode: Reduced from 5000 to 2000
        int rows = Math.min(raw.size() - 1, 2000);
        int totalCols = raw.get(0).length;
        // Safe Mode: Cap columns at 50
        int cols = Math.min(totalCols, 50);

        String[] header = raw.get(0);
        int labelIndex = LabelDetector.detectLabelColumn(header);
        
        // Ensure label index is within bounds of our limited columns, 
        // or handle it separately if it's outside. 
        // For simplicity, we assume label is usually the last column.
        // If label is outside the first 100 columns, we might need a better strategy,
        // but for security/bottleneck fixing, capping features is key.
        int featureCount = Math.min(totalCols - 1, 99); 

        double[][] X = new double[rows][featureCount];
        int[] y = new int[rows];

        Map<Integer, CategoricalEncoder> encoders = new HashMap<>();
        Map<String, Integer> labelMap = new HashMap<>();
        int labelCounter = 0;

        int xRow = 0;

        // Loop only up to the calculated rows limit
        for (int i = 1; i <= rows; i++) {
            String[] row = raw.get(i);
            int xCol = 0;

            for (int j = 0; j < cols; j++) {
                if (j == labelIndex) {
                    String labelValue = row[j];
                    if (!labelMap.containsKey(labelValue)) {
                        labelMap.put(labelValue, labelCounter++);
                    }
                    y[xRow] = labelMap.get(labelValue);
                } else {
                    X[xRow][xCol] = encodeValue(encoders, j, row[j]);
                    xCol++;
                }
            }
            xRow++;
        }

        return new DataSet(X, y);
    }

    static int encodeValue(Map<Integer, CategoricalEncoder> encoders,
                           int column,
                           String value) {
        try {
            return (int) Double.parseDouble(value);
        } catch (Exception e) {
            if (!encoders.containsKey(column))
                encoders.put(column, new CategoricalEncoder());

            return encoders.get(column).encode(value);
        }
    }
}