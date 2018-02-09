package com.raidzero.sphero.executors;

import android.graphics.Color;

import com.raidzero.sphero.bluetooth.Sphero;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by posborn on 2/9/18.
 */

public class LedProcessor {
    private LedThread thread = new LedThread();
    private ExecutorService service = Executors.newSingleThreadExecutor();
    private Sphero sphero;

    public enum LedMode {
        FADE_RGB,
        BREATHE,
        SOLID,
        STROBE,
        STROBE_RANDOM,
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

        @Override
        public void run() {
            running = true;
            while (running) {
                outerLoop: switch (mode) {
                    case STROBE:
                    case STROBE_RANDOM:
                        solidColorSet = false;
                        int color = ledColor;
                        if (mode == LedMode.STROBE_RANDOM) {
                            color = getRandomColor();
                        }

                        sphero.mainLedRgb(color, 0);
                        try {Thread.sleep(100);} catch (Exception e) {}

                        sphero.mainLedRgb(Color.parseColor("#ff000000"), 0);
                        try {Thread.sleep(100);} catch (Exception e) {}

                        break;
                    case BREATHE:
                        solidColorSet = false;
                        for (int g = 0; g < 256; g += 20) {
                            sphero.mainLedRgb(Color.parseColor(String.format("#ff00%02x00", g)), 0);
                            try { Thread.sleep(100); } catch (Exception e) {}
                            if (modeChanged) {
                                modeChanged = false;
                                break outerLoop;
                            }
                        }
                        for (int g = 255; g > 0; g -= 20) {

                            sphero.mainLedRgb(Color.parseColor(String.format("#ff00%02x00", g)), 0);
                            try { Thread.sleep(100); } catch (Exception e) {}
                            if (modeChanged) {
                                modeChanged = false;
                                break outerLoop;
                            }
                        }
                        break;
                    case FADE_RGB:
                        solidColorSet = false;

                        int [] rgbColor = new int[3];
                        rgbColor[0] = 255;
                        rgbColor[1] = 0;
                        rgbColor[2] = 0;

                        for (int decColor = 0; decColor < 3; decColor += 1) {
                            int incColour = decColor == 2 ? 0 : decColor + 1;

                            // cross-fade the two colors.
                            for(int i = 0; i < 255; i += 1) {

                                rgbColor[decColor] -= 1;
                                rgbColor[incColour] += 1;

                                sphero.mainLedRgb(Color.parseColor(String.format("#ff%02x%02x%02x",
                                        rgbColor[0], rgbColor[1], rgbColor[2])), 0);
                                try { Thread.sleep(40); } catch (Exception e) {}

                                if (modeChanged) {
                                    modeChanged = false;
                                    break outerLoop;
                                }
                            }
                            if (modeChanged) {
                                modeChanged = false;
                                break outerLoop;
                            }
                        }
                        break;
                    case SOLID:
                        if (!solidColorSet) {
                            sphero.mainLedRgb(ledColor, 0);
                            solidColorSet = true;
                        }

                        break;
                }
            }
        }

        public void setColor(int color) {
            this.ledColor = color;
        }

        public void setMode(LedMode mode) {
            modeChanged = this.mode != null && mode != this.mode;
            this.mode = mode;
        }

        public LedMode getMode() {
            return mode;
        }

        public void interrupt() {
            running = false;
        }

        public boolean isRunning() {
            return running;
        }

        private int getRandomColor() {
            Random r = new Random();
            int red = r.nextInt(256);
            int green = r.nextInt(256);
            int blue = r.nextInt(256);
            String colorStr = String.format("#ff%02x%02x%02x", red, green, blue);
            return Color.parseColor(colorStr);
        }
    }

    public void setLedMode(LedMode mode) {
        thread.setMode(mode);
    }

    public void setLedColor(int color) {
        thread.ledColor = color;
    }

    public void stop() {
        thread.interrupt();
        service.shutdownNow();
    }
}
