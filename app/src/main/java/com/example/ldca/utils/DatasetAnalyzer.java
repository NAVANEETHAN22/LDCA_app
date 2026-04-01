package com.example.ldca.utils;

import java.util.*;

public class DatasetAnalyzer {

    public static String analyze(List<String[]> data){

        int rows = data.size() - 1;
        int cols = data.get(0).length;

        String[] header = data.get(0);

        int label = LabelDetector.detectLabelColumn(header);

        return "Rows: " + rows +
                "\nColumns: " + cols +
                "\nLabel Column: " + header[label];

    }

}
