package sk.zdila.chromalux;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by martin on 2/5/15.
 */
public class BrightnessPicker extends View {

    private Bitmap brightnessBitmap;
    //private int x;

    private float[] hsv = new float[3];

    //private float hue;
    //private float saturation;

    private Paint colorPointerPaint1;
    private Paint colorPointerPaint2;


    private OnColorChangeListener onColorChangeListener;

    public void setHS(final float hue, final float saturation) {
        hsv[0] = hue;
        hsv[1] = saturation;

        drawToBitmap();
        invalidate();
    }

    public interface OnColorChangeListener {
        void onColorChanged(BrightnessPicker colorPicker, int color, float[] colorHSV);
    }

    public void setOnColorChangeListener(final OnColorChangeListener onColorChangeListener) {
        this.onColorChangeListener = onColorChangeListener;
    }

    public BrightnessPicker(final Context context) {
        super(context);
        init();
    }

    public BrightnessPicker(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BrightnessPicker(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        colorPointerPaint1 = new Paint();
        colorPointerPaint1.setStyle(Paint.Style.STROKE);
        colorPointerPaint1.setStrokeWidth(4f);
        colorPointerPaint1.setARGB(255, 0, 0, 0);

        colorPointerPaint2 = new Paint();
        colorPointerPaint2.setStyle(Paint.Style.STROKE);
        colorPointerPaint2.setStrokeWidth(4f);
        colorPointerPaint2.setARGB(255, 255, 255, 255);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        brightnessBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawToBitmap();
    }

    private void drawToBitmap() {
        final Canvas c = new Canvas(brightnessBitmap);

        float[] hsv = new float[3];
        hsv[0] = this.hsv[0];
        hsv[1] = this.hsv[1];
        hsv[2] = 1f;

        final Shader mShader = new LinearGradient(0, 0, brightnessBitmap.getWidth(), 0, new int[] { Color.BLACK, Color.HSVToColor(hsv) }, null, Shader.TileMode.CLAMP);
        final Paint paint = new Paint();
        paint.setShader(mShader);
        c.drawPaint(paint);
    }

    public void setColor(final int color) {
        Color.colorToHSV(color, hsv);
        // TODO elsewhere
//        x = (int) (hsv[2] * getWidth());
//
//        drawToBitmap();
//        invalidate();
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        canvas.drawBitmap(brightnessBitmap, 0, 0, null);
        int x = (int) (hsv[2] * getWidth());
        canvas.drawRect(x - 8f, 8f, x + 8f, getHeight() - 8f, x > getWidth() / 2 ? colorPointerPaint1 : colorPointerPaint2);
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                int x = (int) event.getX();

                if (x < 0) {
                    x = 0;
                } else if (x > getWidth()) {
                    x = getWidth();
                }

                invalidate();

                if (onColorChangeListener != null) {
                    hsv[2] = ((float) x) / getWidth();
                    onColorChangeListener.onColorChanged(this, Color.HSVToColor(hsv), hsv);
                }

                return true;
        }
        return super.onTouchEvent(event);    }
}
