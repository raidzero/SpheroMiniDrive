package com.raidzero.sphero.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.raidzero.sphero.R;


/**
 * Created by posborn on 2/14/18.
 */

public class RgbSeekBar extends View {
    GradientDrawable mGradient;

    int mWidth, mHeight;
    Bitmap mGradientBitmap;
    RgbSeekBarListener mListener;

    Paint mTextPaint;
    int textX, textY;


    public interface RgbSeekBarListener {
        void onColorSelected(int color);
    }

    public RgbSeekBar(Context context) {
        super(context);
        init();
    }

    public RgbSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RgbSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setColorChangeListener(RgbSeekBarListener listener) {
        mListener = listener;
    }

    private void init() {
        mGradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {
                        Color.RED,
                        Color.YELLOW,
                        Color.GREEN,
                        Color.CYAN,
                        Color.BLUE,
                        Color.MAGENTA,
                        Color.WHITE
                });

        mTextPaint = new Paint();
        mTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(16 * getResources().getDisplayMetrics().density);
        mTextPaint.setShadowLayer(5.0f, 3.0f, 3.0f, Color.BLACK);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        setBackground(mGradient);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);

        textX = mWidth/2;
        textY = mHeight / 2;

        mGradientBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mGradientBitmap);
        mGradient.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        mGradient.draw(canvas);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int x = (int) event.getX();
        int y = (int) event.getY();

        if (x < mGradientBitmap.getWidth() && y < mGradientBitmap.getHeight()
                && x >= 0 && y >= 0) {
            int pixel = mGradientBitmap.getPixel(x, y);

            if (mListener != null) {
                mListener.onColorSelected(pixel);
            }
        }

        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawText(getResources().getString(R.string.rgb_bar_hint),
                textX, textY, mTextPaint);
        invalidate();

        super.onDraw(canvas);
    }

}
