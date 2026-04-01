package com.example.ldca.utils;

public class LabelDetector {

    public static int detectLabelColumn(String[] header){

        for(int i=0;i<header.length;i++){

            String col = header[i].toLowerCase();

            if(col.contains("label") ||
                    col.contains("class") ||
                    col.contains("target") ||
                    col.contains("result"))
                return i;

        }

        return header.length-1; // fallback = last column

    }

}
