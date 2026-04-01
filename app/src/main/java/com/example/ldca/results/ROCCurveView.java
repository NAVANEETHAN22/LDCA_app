package com.example.ldca.results;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom view that draws an ROC curve using Android Canvas.
 * Set data via setData() before adding to layout.
 */
public class ROCCurveView extends View {

    private float[] fpr;
    private float[] tpr;
    private double  auc;

    private final Paint linePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint diagPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ROCCurveView(Context ctx) { super(ctx); init(); }
    public ROCCurveView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }

    private void init() {
        linePaint.setColor(Color.parseColor("#1565C0"));
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);

        diagPaint.setColor(Color.parseColor("#9E9E9E"));
        diagPaint.setStrokeWidth(2f);
        diagPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{10, 10}, 0));
        diagPaint.setStyle(Paint.Style.STROKE);

        axisPaint.setColor(Color.BLACK);
        axisPaint.setStrokeWidth(2f);

        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(28f);

        fillPaint.setColor(Color.parseColor("#301565C0"));
        fillPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(float[] fpr, float[] tpr, double auc) {
        this.fpr = fpr;
        this.tpr = tpr;
        this.auc = auc;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (fpr == null || tpr == null) return;

        float pad = 80f;
        float w = getWidth()  - pad * 2;
        float h = getHeight() - pad * 2;

        // Axes
        canvas.drawLine(pad, pad, pad, pad + h, axisPaint);
        canvas.drawLine(pad, pad + h, pad + w, pad + h, axisPaint);

        // Axis labels
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("False Positive Rate", pad + w / 2, pad + h + 60f, textPaint);
        canvas.save();
        canvas.rotate(-90, 30f, pad + h / 2);
        canvas.drawText("True Positive Rate", 30f, pad + h / 2, textPaint);
        canvas.restore();

        // Diagonal (random baseline)
        canvas.drawLine(pad, pad + h, pad + w, pad, diagPaint);

        // Fill under curve
        Path fillPath = new Path();
        fillPath.moveTo(pad, pad + h);
        for (int i = 0; i < fpr.length; i++) {
            float x = pad + fpr[i] * w;
            float y = pad + (1 - tpr[i]) * h;
            if (i == 0) fillPath.moveTo(x, pad + h);
            fillPath.lineTo(x, y);
        }
        fillPath.lineTo(pad + w, pad + h);
        fillPath.close();
        canvas.drawPath(fillPath, fillPaint);

        // ROC line
        Path path = new Path();
        for (int i = 0; i < fpr.length; i++) {
            float x = pad + fpr[i] * w;
            float y = pad + (1 - tpr[i]) * h;
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        canvas.drawPath(path, linePaint);

        // AUC label
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.parseColor("#1565C0"));
        textPaint.setTextSize(32f);
        canvas.drawText(String.format("AUC = %.3f", auc), pad + 16, pad + 40, textPaint);
    }
}
