package com.example.ldca.results;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom view that draws cross-validation fold accuracies as a bar chart
 * with a mean accuracy line overlay.
 */
public class CrossValidationView extends View {

    private double[] foldAccuracies;
    private double   mean;

    private final Paint barPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint meanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint();

    public CrossValidationView(Context ctx) { super(ctx); init(); }
    public CrossValidationView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }

    private void init() {
        barPaint.setColor(Color.parseColor("#00897B"));
        barPaint.setStyle(Paint.Style.FILL);

        meanPaint.setColor(Color.parseColor("#E53935"));
        meanPaint.setStrokeWidth(4f);
        meanPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{14, 8}, 0));
        meanPaint.setStyle(Paint.Style.STROKE);

        axisPaint.setColor(Color.BLACK);
        axisPaint.setStrokeWidth(2f);

        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(28f);

        gridPaint.setColor(Color.parseColor("#22000000"));
        gridPaint.setStrokeWidth(1f);
    }

    public void setData(double[] foldAccuracies, double mean) {
        this.foldAccuracies = foldAccuracies;
        this.mean           = mean;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (foldAccuracies == null || foldAccuracies.length == 0) return;

        float pad    = 80f;
        float w      = getWidth()  - pad * 2;
        float h      = getHeight() - pad * 2;
        int   k      = foldAccuracies.length;
        float barW   = w / k * 0.6f;
        float spacing = w / k;

        // Axes
        canvas.drawLine(pad, pad, pad, pad + h, axisPaint);
        canvas.drawLine(pad, pad + h, pad + w, pad + h, axisPaint);

        // Y-axis labels (0.0 → 1.0)
        textPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 5; i++) {
            float val = i * 0.2f;
            float y   = pad + h - val * h;
            canvas.drawText(String.format("%.1f", val), pad - 8, y + 10, textPaint);
            canvas.drawLine(pad, y, pad + w, y, gridPaint);
        }

        for (int i = 0; i < k; i++) {
            float acc   = (float) foldAccuracies[i];
            float left  = pad + i * spacing + (spacing - barW) / 2;
            float top   = pad + h - acc * h;
            float right = left + barW;
            float bot   = pad + h;

            canvas.drawRoundRect(new RectF(left, top, right, bot), 8, 8, barPaint);

            // Fold label
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.DKGRAY);
            canvas.drawText("F" + (i + 1), left + barW / 2, pad + h + 45, textPaint);

            // Accuracy on bar
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(24f);
            canvas.drawText(String.format("%.2f", acc), left + barW / 2, top + 26, textPaint);
            textPaint.setTextSize(28f);
        }

        // Mean line
        float meanY = pad + h - (float) mean * h;
        canvas.drawLine(pad, meanY, pad + w, meanY, meanPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.parseColor("#E53935"));
        canvas.drawText(String.format("Mean = %.3f", mean), pad + 8, meanY - 8, textPaint);
    }

}
