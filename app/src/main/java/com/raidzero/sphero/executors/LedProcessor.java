package com.raidzero.sphero.executors;

import android.graphics.Color;
import android.util.Log;

import com.raidzero.sphero.bluetooth.Sphero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by posborn on 2/9/18.
 */

public class LedProcessor {
    private static final String TAG = "LedProcessor";

    private LedThread thread = new LedThread();
    private ExecutorService service = Executors.newSingleThreadExecutor();
    private Sphero sphero;

    public enum LedMode {
        FADE_RGB,
        BREATHE,
        BREATHE_RANDOM,
        SOLID,
        STROBE,
        STROBE_RANDOM,
        PULL_OVER,
        OFF,
    }


    public LedProcessor(Sphero sphero, LedMode mode, int ledColor) {
        this.sphero = sphero;

        thread.setMode(mode);
        thread.setColor(ledColor);

        if (!service.isShutdown()) {
            service.execute(thread);
        }
    }

    class LedThread implements Runnable {
        boolean running;
        LedMode mode;
        int ledColor;
        boolean solidColorSet;
        boolean modeChanged = false;
        boolean colorChanged = false;
        boolean ledAlreadyOff = false;

        @Override
        public void run() {
            running = true;
            int loopCount = 0;
            while (running) {
                if (loopCount > 100) {
                    loopCount = 0;
                }
                loopCount++;

                outerLoop: switch (mode) {
                    case STROBE:
                    case STROBE_RANDOM:
                    case PULL_OVER:
                        solidColorSet = false;
                        int color = ledColor;
                        if (mode == LedMode.STROBE_RANDOM) {
                            color = getRandomColor();
                        } else if (mode == LedMode.PULL_OVER) {
                            int[] colors = new int[] { Color.RED, Color.BLUE };
                            if (loopCount % 2 == 0) {
                                color = colors[0];
                            } else {
                                color = colors[1];
                            }
                        }

                        sphero.mainLedRgb(color);
                        try {
                            Thread.sleep(50); } catch (Exception e) {}

                        sphero.mainLedRgb(Color.parseColor("#ff000000"));
                        try {
                            Thread.sleep(50); } catch (Exception e) {}

                        break;
                    case BREATHE:
                    case BREATHE_RANDOM:
                        solidColorSet = false;
                        int steps = 5;
                        int breatheColor = ledColor;
                        if (mode == LedMode.BREATHE_RANDOM) {
                            breatheColor = getRandomColor();
                        }
                        for (int stepColor : createBreatheSteps(breatheColor, steps)) {
                            sphero.mainLedRgb(stepColor);

                            // if LED is off, wait a bit before the next breath
                            if (stepColor == Color.BLACK) {
                                try { Thread.sleep(300); } catch (Exception e) {}
                            } else {
                                try { Thread.sleep(100); } catch (Exception e) {}
                            }

                            if (modeChanged || !running) {
                                modeChanged = false;
                                sphero.clearCommands();
                                break outerLoop;
                            }
                        }
                        break;
                    case FADE_RGB:
                        solidColorSet = false;

                        int[] rgbColor = new int[3];

                        rgbColor[0] = 255;
                        rgbColor[1] = 0;
                        rgbColor[2] = 0;

                        for (int decColor = 0; decColor < 3; decColor += 1) {
                            int incColour = decColor == 2 ? 0 : decColor + 1;

                            // cross-fade the two colors.
                            for (int i = 0; i < 255; i += 1) {

                                rgbColor[decColor] -= 1;
                                rgbColor[incColour] += 1;

                                sphero.mainLedRgb(Color.parseColor(String.format("#ff%02x%02x%02x",
                                        rgbColor[0], rgbColor[1], rgbColor[2])));
                                try { Thread.sleep(40); } catch (Exception e) {}

                                if (modeChanged || !running) {
                                    modeChanged = false;
                                    break outerLoop;
                                }
                            }
                            if (modeChanged || !running) {
                                modeChanged = false;
                                break outerLoop;
                            }
                        }
                        break;
                    case SOLID:
                        if (!solidColorSet) {
                            sphero.mainLedRgb(ledColor);
                            solidColorSet = true;
                        }

                        break;
                    case OFF:
                        if (!ledAlreadyOff) {
                            sphero.mainLedRgb(Color.BLACK);
                            ledAlreadyOff = true;
                        }
                        break;


                }
            // end outer loop
            }
        }

        public void setColor(int color) {
            this.ledColor = color;
        }

        public void setMode(LedMode mode) {
            this.mode = mode;
            modeChanged = true;
        }

        public void interrupt() {
            running = false;
        }

        public void resume() {
            running = true;
        }
    }

    public void setLedMode(LedMode mode) {
        if (thread.mode != mode) {
            thread.setMode(mode);
        }
    }

    public void setLedColor(int color) {
        thread.ledColor = color;
        thread.solidColorSet = false;
    }

    public void stop() {
        thread.interrupt();
        service.shutdownNow();
    }

    private List<Integer> createBreatheSteps(int color, int steps) {
        List<Integer> colors = new ArrayList<Integer>();

        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        int redSteps = safeIntegerDivision(red, steps);
        int greenSteps = safeIntegerDivision(green, steps);
        int blueSteps = safeIntegerDivision(blue, steps);

        int r = 0;
        int g = 0;
        int b = 0;

        // create fade in
        List<Integer> faded = new ArrayList<Integer>();
        faded.add(Color.BLACK);

        for (int i = 0; i < steps; i++) {
            r += redSteps;
            g += greenSteps;
            b += blueSteps;

            faded.add(Color.argb(255, r, g, b));
        }

        colors.addAll(faded);

        // now reverse and add
        Collections.reverse(faded);
        colors.addAll(faded);

        return colors;
    }

    private int getRandomColor() {
        int[] colors = new int[] {
                Color.WHITE,
                Color.RED,
                Color.GREEN,
                Color.BLUE,
                Color.YELLOW,
                Color.CYAN,
                Color.MAGENTA
        };

        Random r = new Random();
        r.setSeed(new Date().getTime());
        int colorIndex = r.nextInt(colors.length);

        return colors[colorIndex];
    }

    private int safeIntegerDivision(int numerator, int denominator) {
        if (numerator == 0 || denominator == 0) {
            return 0;
        } else {
            return numerator / denominator;
        }
    }
}
