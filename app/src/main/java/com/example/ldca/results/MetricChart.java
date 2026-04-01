package com.example.ldca.results;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.charts.BarChart;

import java.util.ArrayList;

public class MetricChart {

    public static void showMetrics(BarChart chart, ResultBundle r){

        ArrayList<BarEntry> entries = new ArrayList<>();

        entries.add(new BarEntry(0,(float)r.accuracy));
        entries.add(new BarEntry(1,(float)r.precision));
        entries.add(new BarEntry(2,(float)r.recall));
        entries.add(new BarEntry(3,(float)r.f1));

        BarDataSet set = new BarDataSet(entries,"Metrics");

        BarData data = new BarData(set);

        chart.setData(data);
        chart.invalidate();

    }

}
