package com.raidzero.sphero.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.raidzero.sphero.R;

/**
 * Created by posborn on 2/12/18.
 */

public class RgbSliderView extends LinearLayout implements SeekBar.OnSeekBarChangeListener {
    private SeekBar redSeekBar, greenSeekBar, blueSeekBar;
    private RgbSliderListener mListener;
    private TextView colorValue;
    private int currentColor;

    public RgbSliderView(Context context) {
        super(context);
        init(context);
    }

    public RgbSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RgbSliderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public interface RgbSliderListener {
        void onColorChanged(int newColor);
    }

    public void setOnColorChangeListener(RgbSliderListener listener) {
        mListener = listener;
    }

    private void init(Context context) {
        setOrientation(LinearLayout.VERTICAL);

        LayoutInflater.from(context).inflate(R.layout.view_rgb_slider, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        redSeekBar = (SeekBar) this.findViewById(R.id.seek_red);
        greenSeekBar = (SeekBar) this.findViewById(R.id.seek_green);
        blueSeekBar = (SeekBar) this.findViewById(R.id.seek_blue);

        colorValue = (TextView) this.findViewById(R.id.color_value);

        redSeekBar.setOnSeekBarChangeListener(this);
        greenSeekBar.setOnSeekBarChangeListener(this);
        blueSeekBar.setOnSeekBarChangeListener(this);
    }

    public void setColor(int color) {
        updateColor(color);

        redSeekBar.setProgress(Color.red(color));
        greenSeekBar.setProgress(Color.green(color));
        blueSeekBar.setProgress(Color.blue(color));
    }

    // seekbar listener methods
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        int red = redSeekBar.getProgress();
        int green = greenSeekBar.getProgress();
        int blue = blueSeekBar.getProgress();

        updateColor(Color.argb(255, red, green, blue));
    }

    private void updateColor(int color) {
        // if the new color is dark, make the text color white
        int brightness = getColorBrightness(color);
        if (brightness < 125) {
            colorValue.setTextColor(Color.WHITE);
        } else {
            colorValue.setTextColor(Color.BLACK);
        }

        if (brightness > 0) {
            colorValue.setText(String.format("#%02X%02X%02X",
                    Color.red(color), Color.green(color), Color.blue(color)));
        } else {
            //colorValue.setText(getContext().getString(R.string.off));
        }

        currentColor = color;
        colorValue.setBackgroundColor(color);
        mListener.onColorChanged(color);
    }

    private int getColorBrightness(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        return ((red * 299) + (green * 587) + (blue * 114)) / 1000;
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // ignore
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // ignore
    }
}
