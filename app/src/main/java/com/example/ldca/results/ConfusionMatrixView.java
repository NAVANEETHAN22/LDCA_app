package com.example.ldca.results;

import android.content.Context;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class ConfusionMatrixView {

    public static TableLayout generate(Context ctx, int[][] matrix){

        TableLayout table = new TableLayout(ctx);
        int limit = Math.min(20, matrix.length); // Prevent UI freeze on huge matrices

        for(int i = 0; i < limit; i++){

            TableRow row = new TableRow(ctx);

            for(int j = 0; j < limit; j++){

                TextView tv = new TextView(ctx);

                tv.setText(String.valueOf(matrix[i][j]));
                tv.setPadding(20, 20, 20, 20);

                row.addView(tv);

            }

            table.addView(row);

        }

        if(matrix.length > 20) {
            TextView note = new TextView(ctx);
            note.setText("Note: Confusion matrix is truncated to 20x20. Actual size is " + matrix.length + "x" + matrix.length);
            table.addView(note);
        }

        return table;
    }

}
