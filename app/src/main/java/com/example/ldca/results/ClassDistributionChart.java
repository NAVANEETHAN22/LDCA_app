package com.example.ldca.results;

import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.charts.PieChart;

import java.util.*;

public class ClassDistributionChart {

    public static void show(PieChart chart, int[] labels){

        Map<Integer,Integer> count = new HashMap<>();

        for(int l:labels){

            count.put(l,count.getOrDefault(l,0)+1);

        }

        List<Map.Entry<Integer, Integer>> entryList = new ArrayList<>(count.entrySet());
        entryList.sort((a, b) -> b.getValue().compareTo(a.getValue())); // Sort descending by count

        List<PieEntry> entries = new ArrayList<>();
        int limit = Math.min(20, entryList.size());
        
        for(int i = 0; i < limit; i++){
            Map.Entry<Integer, Integer> e = entryList.get(i);
            entries.add(new PieEntry(e.getValue(), "Class " + e.getKey()));
        }

        if(entryList.size() > 20) {
            int otherCount = 0;
            for(int i = 20; i < entryList.size(); i++) {
                otherCount += entryList.get(i).getValue();
            }
            entries.add(new PieEntry(otherCount, "Other (" + (entryList.size() - 20) + ")"));
        }

        PieDataSet set = new PieDataSet(entries,"Distribution");

        PieData data = new PieData(set);

        chart.setData(data);
        chart.invalidate();

    }

}