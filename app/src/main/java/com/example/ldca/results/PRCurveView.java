package com.example.ldca.results;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom view that draws a Precision-Recall curve using Android Canvas.
 */
public class PRCurveView extends View {

    private float[] recall;
    private float[] precision;

    private final Paint linePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PRCurveView(Context ctx) { super(ctx); init(); }
    public PRCurveView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }

    private void init() {
        linePaint.setColor(Color.parseColor("#6A1B9A"));
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);

        axisPaint.setColor(Color.BLACK);
        axisPaint.setStrokeWidth(2f);

        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(28f);

        fillPaint.setColor(Color.parseColor("#306A1B9A"));
        fillPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(float[] recall, float[] precision) {
        this.recall    = recall;
        this.precision = precision;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (recall == null || precision == null) return;

        float pad = 80f;
        float w = getWidth()  - pad * 2;
        float h = getHeight() - pad * 2;

        // Axes
        canvas.drawLine(pad, pad, pad, pad + h, axisPaint);
        canvas.drawLine(pad, pad + h, pad + w, pad + h, axisPaint);

        // Axis labels
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Recall", pad + w / 2, pad + h + 60f, textPaint);
        canvas.save();
        canvas.rotate(-90, 30f, pad + h / 2);
        canvas.drawText("Precision", 30f, pad + h / 2, textPaint);
        canvas.restore();

        // Fill under PR curve
        Path fillPath = new Path();
        for (int i = 0; i < recall.length; i++) {
            float x = pad + recall[i] * w;
            float y = pad + (1 - precision[i]) * h;
            if (i == 0) { fillPath.moveTo(x, pad + h); fillPath.lineTo(x, y); }
            else fillPath.lineTo(x, y);
        }
        fillPath.lineTo(pad + recall[recall.length - 1] * w, pad + h);
        fillPath.close();
        canvas.drawPath(fillPath, fillPaint);

        // PR curve line
        Path path = new Path();
        for (int i = 0; i < recall.length; i++) {
            float x = pad + recall[i] * w;
            float y = pad + (1 - precision[i]) * h;
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        canvas.drawPath(path, linePaint);

        // Title
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.parseColor("#6A1B9A"));
        textPaint.setTextSize(32f);
        canvas.drawText("Precision-Recall Curve", pad + 16, pad + 40, textPaint);
    }
}
