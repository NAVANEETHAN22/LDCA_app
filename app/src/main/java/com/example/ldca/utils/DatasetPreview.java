package com.example.ldca.utils;

import java.util.*;

public class DatasetPreview {

    public static List<String[]> preview(List<String[]> data){

        List<String[]> preview = new ArrayList<>();

        int limit = Math.min(10, data.size());

        for(int i=0;i<limit;i++){
            preview.add(data.get(i));
        }

        return preview;

    }

}
