/*
 * Copyright 2013 Piotr Adamus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sk.zdila.chromalux;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ComposeShader;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.SweepGradient;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorPicker extends View {

    /**
     * Customizable display parameters (in percents)
     */

    private Paint colorWheelPaint;

    private Paint colorPointerPaint1;
    private Paint colorPointerPaint2;
    private RectF colorPointerCoords;

    private Bitmap colorWheelBitmap;

    private int wheelRadius;

    /** Currently selected color */
    private float[] colorHSV = new float[] { 0f, 0f, 1f };

    private OnColorChangeListener onColorChangeListener;

    //private float value = 1f;


    public interface OnColorChangeListener {
        void onColorChanged(ColorPicker colorPicker, int color, float[] colorHSV);
    }

    public void setOnColorChangeListener(final OnColorChangeListener onColorChangeListener) {
        this.onColorChangeListener = onColorChangeListener;
    }

    public ColorPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public ColorPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorPicker(Context context) {
        super(context);
        init();
    }

    private void init() {
        colorPointerPaint1 = new Paint();
        colorPointerPaint1.setStyle(Style.STROKE);
        colorPointerPaint1.setStrokeWidth(4f);
        colorPointerPaint1.setARGB(255, 0, 0, 0);

        colorPointerPaint2 = new Paint();
        colorPointerPaint2.setStyle(Style.STROKE);
        colorPointerPaint2.setStrokeWidth(4f);
        colorPointerPaint2.setARGB(255, 255, 255, 255);

        colorWheelPaint = new Paint();
        colorWheelPaint.setAntiAlias(true);
        colorWheelPaint.setDither(true);

        colorPointerCoords = new RectF();

    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        final int size = Math.min(widthSize, heightSize);
        setMeasuredDimension(size, size);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(final Canvas canvas) {
        final int centerX = getWidth() / 2;
        final int centerY = getHeight() / 2;

        // drawing color wheel

        Paint p = new Paint();
        ColorFilter filter = new LightingColorFilter(Color.HSVToColor(new float[] { 0f, 0f, colorHSV[2] }), 0);
        p.setColorFilter(filter);
        canvas.drawBitmap(colorWheelBitmap, centerX - wheelRadius, centerY - wheelRadius, p);

        // drawing color wheel pointer

        final float hueAngle = (float) Math.toRadians(colorHSV[0]);
        final int colorPointX = (int) (-Math.cos(hueAngle) * colorHSV[1] * wheelRadius) + centerX;
        final int colorPointY = (int) (-Math.sin(hueAngle) * colorHSV[1] * wheelRadius) + centerY;

        final float pointerRadius = 0.075f * wheelRadius;
        final int pointerX = (int) (colorPointX - pointerRadius / 2);
        final int pointerY = (int) (colorPointY - pointerRadius / 2);

        colorPointerCoords.set(pointerX, pointerY, pointerX + pointerRadius, pointerY + pointerRadius);
        canvas.drawOval(colorPointerCoords, colorHSV[2] < 0.5  ? colorPointerPaint2 : colorPointerPaint1);
    }

    @Override
    protected void onSizeChanged(final int width, final int height, final int oldw, final int oldh) {
        wheelRadius = width / 2;

        colorWheelBitmap = createColorWheelBitmap(wheelRadius * 2, wheelRadius * 2);

        Matrix gradientRotationMatrix = new Matrix();
        gradientRotationMatrix.preRotate(270, width / 2, height / 2);
    }

    private Bitmap createColorWheelBitmap(int width, int height) {
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);

        final int colorCount = 12;
        final int colorAngleStep = 360 / 12;
        final int colors[] = new int[colorCount + 1];
        final float hsv[] = new float[] { 0f, 1f, 1f };
        for (int i = 0; i < colors.length; i++) {
            hsv[0] = (i * colorAngleStep + 180) % 360;
            colors[i] = Color.HSVToColor(hsv);
        }
        colors[colorCount] = colors[0];

        final SweepGradient sweepGradient = new SweepGradient(width / 2, height / 2, colors, null);
        final RadialGradient radialGradient = new RadialGradient(width / 2, height / 2, wheelRadius, 0xFFFFFFFF, 0x00FFFFFF, TileMode.CLAMP);
        final ComposeShader composeShader = new ComposeShader(sweepGradient, radialGradient, PorterDuff.Mode.SRC_OVER);

        colorWheelPaint.setShader(composeShader);

        final Canvas canvas = new Canvas(bitmap);
        canvas.drawCircle(width / 2, height / 2, wheelRadius, colorWheelPaint);

        return bitmap;
    }

    public void setValue(float value) {
        //this.value = value;
        colorHSV[2] = value;
        //colorWheelBitmap = createColorWheelBitmap(getWidth(), getHeight());
        invalidate();
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_MOVE:
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            final int cx = x - getWidth() / 2;
            final int cy = y - getHeight() / 2;
            final double d = Math.sqrt(cx * cx + cy * cy);

            colorHSV[0] = (float) (Math.toDegrees(Math.atan2(cy, cx)) + 180f);
            colorHSV[1] = Math.max(0f, Math.min(1f, (float) (d / wheelRadius)));
            invalidate();

            if (onColorChangeListener != null) {
                onColorChangeListener.onColorChanged(this, Color.HSVToColor(colorHSV), colorHSV);
            }

            return true;
        }
        return super.onTouchEvent(event);
    }

    public void setColor(int color) {
        Color.colorToHSV(color, colorHSV);
    }

    public int getColor() {
        return Color.HSVToColor(colorHSV);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle state = new Bundle();
        state.putFloatArray("color", colorHSV);
        state.putParcelable("super", super.onSaveInstanceState());
        return state;
    }

    @Override
    protected void onRestoreInstanceState(final Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle bundle = (Bundle) state;
            colorHSV = bundle.getFloatArray("color");
            super.onRestoreInstanceState(bundle.getParcelable("super"));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

}
