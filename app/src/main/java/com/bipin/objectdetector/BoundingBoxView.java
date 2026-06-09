package com.bipin.objectdetector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class BoundingBoxView extends View {

    public static class Box {
        public float left, top, right, bottom;
        public String label;
        public float confidence;
    }

    private final List<Box> boxes = new ArrayList<>();
    private final Paint paint = new Paint();
    private final Paint textPaint = new Paint();

    public BoundingBoxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);

        textPaint.setColor(Color.GREEN);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(44f);
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(4f, 0f, 0f, Color.BLACK);
    }

    public void setBoxes(List<Box> newBoxes) {
        boxes.clear();
        boxes.addAll(newBoxes);
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Box b : boxes) {
            canvas.drawRect(b.left, b.top, b.right, b.bottom, paint);
            canvas.drawText(
                    b.label,
                    b.left + 10,
                    b.top - 15,
                    textPaint
            );
        }
    }
}