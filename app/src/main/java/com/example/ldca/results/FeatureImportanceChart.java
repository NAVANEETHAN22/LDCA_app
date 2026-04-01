package com.example.ldca.results;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom view that draws a horizontal bar chart for feature importances.
 */
public class FeatureImportanceChart extends View {

    private double[] importances;
    private String[] names;

    private final Paint barPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int[] BAR_COLORS = {
            Color.parseColor("#1E88E5"),
            Color.parseColor("#43A047"),
            Color.parseColor("#E53935"),
            Color.parseColor("#FB8C00"),
            Color.parseColor("#8E24AA"),
            Color.parseColor("#00ACC1"),
            Color.parseColor("#F4511E"),
            Color.parseColor("#3949AB"),
    };

    public FeatureImportanceChart(Context ctx) { super(ctx); init(); }
    public FeatureImportanceChart(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }

    private void init() {
        barPaint.setStyle(Paint.Style.FILL);
        axisPaint.setColor(Color.BLACK);
        axisPaint.setStrokeWidth(2f);
        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(26f);
    }

    public void setData(double[] importances, String[] names) {
        this.importances = importances;
        this.names       = names;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int bars = (importances != null) ? Math.min(importances.length, 15) : 1;
        int needed = bars * 52 + 80;
        setMeasuredDimension(
                MeasureSpec.getSize(widthSpec),
                Math.max(needed, MeasureSpec.getSize(heightSpec))
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (importances == null || importances.length == 0) return;

        float leftPad  = 220f;
        float rightPad = 40f;
        float topPad   = 20f;
        float barH     = 34f;
        float gap      = 18f;

        int count = Math.min(importances.length, 15);
        float maxImp = 0;
        for (int i = 0; i < count; i++) maxImp = (float) Math.max(maxImp, importances[i]);
        if (maxImp == 0) maxImp = 1;

        float availW = getWidth() - leftPad - rightPad;

        for (int i = 0; i < count; i++) {
            float top  = topPad + i * (barH + gap);
            float barW = (float) (importances[i] / maxImp) * availW;

            barPaint.setColor(BAR_COLORS[i % BAR_COLORS.length]);
            canvas.drawRoundRect(new RectF(leftPad, top, leftPad + barW, top + barH),
                    6, 6, barPaint);

            // Feature name
            String label = (names != null && i < names.length) ? names[i] : "f" + i;
            if (label.length() > 14) label = label.substring(0, 13) + "…";
            textPaint.setTextAlign(Paint.Align.RIGHT);
            textPaint.setColor(Color.DKGRAY);
            canvas.drawText(label, leftPad - 8, top + barH - 8, textPaint);

            // Value
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(Color.DKGRAY);
            canvas.drawText(String.format("%.3f", importances[i]),
                    leftPad + barW + 6, top + barH - 8, textPaint);
        }
    }
}
