package com.raidzero.sphero.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.raidzero.sphero.R;

/**
 * Created by raidzero on 2/10/18.
 */

public class ColorPickerFragment extends DialogFragment implements SeekBar.OnSeekBarChangeListener {

    private SeekBar mSeekRed, mSeekGreen, mSeekBlue;
    private TextView mColorText;
    private LinearLayout mColorPreview;
    private int mCurrentColor;

    private ColorPickerListener mListener;

    public interface ColorPickerListener {
        void onColorChange(int newColor);
        int getCurrentColor();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mListener = (ColorPickerListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(getString(R.string.label_select_color));
        return inflater.inflate(R.layout.fragment_color_picker, container, false);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        mSeekRed = (SeekBar) v.findViewById(R.id.seek_red);
        mSeekGreen = (SeekBar) v.findViewById(R.id.seek_green);
        mSeekBlue = (SeekBar) v.findViewById(R.id.seek_blue);

        mColorText = (TextView) v.findViewById(R.id.color_value);
        mColorPreview = (LinearLayout) v.findViewById(R.id.color_preview);

        mSeekRed.setOnSeekBarChangeListener(this);
        mSeekGreen.setOnSeekBarChangeListener(this);
        mSeekBlue.setOnSeekBarChangeListener(this);

        int currentColor = mListener.getCurrentColor();
        mSeekRed.setProgress(Color.red(currentColor));
        mSeekGreen.setProgress(Color.green(currentColor));
        mSeekBlue.setProgress(Color.blue(currentColor));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        updateCurrentColor();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void updateCurrentColor() {
        int red = mSeekRed.getProgress();
        int green = mSeekGreen.getProgress();
        int blue = mSeekBlue.getProgress();

        mCurrentColor = Color.argb(255, red, green, blue);

        mColorPreview.setBackgroundColor(mCurrentColor);
        mColorText.setText(String.format("#%02X%02X%02X", red, green, blue));

        mListener.onColorChange(mCurrentColor);
    }
}
