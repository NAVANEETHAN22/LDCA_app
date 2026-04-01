package com.example.ldca.results;

import android.content.Context;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class PredictionTable {

    public static TableLayout generate(Context ctx, int[] yTrue, int[] yPred){

        TableLayout table = new TableLayout(ctx);

        // Cap at 100 rows — rendering thousands of TableRows causes OOM
        int limit = Math.min(100, yTrue.length);

        // Header row
        TableRow header = new TableRow(ctx);
        TextView hActual = new TextView(ctx);
        hActual.setText("Actual");
        hActual.setPadding(16, 8, 16, 8);
        hActual.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView hPred = new TextView(ctx);
        hPred.setText("Predicted");
        hPred.setPadding(16, 8, 16, 8);
        hPred.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(hActual);
        header.addView(hPred);
        table.addView(header);

        for(int i = 0; i < limit; i++){

            TableRow row = new TableRow(ctx);

            TextView actual = new TextView(ctx);
            actual.setText("Actual: "+yTrue[i]);

            TextView pred = new TextView(ctx);
            pred.setText("Predicted: "+yPred[i]);

            row.addView(actual);
            row.addView(pred);

            table.addView(row);

        }

        return table;

    }

}
