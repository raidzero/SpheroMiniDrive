package com.raidzero.sphero.bluetooth;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by raidzero on 1/26/18.
 */

public class SpheroCommand {
    private static final String TAG = "SpheroCommand";

    private static byte COUNT = 0x0;

    private static final short[] CMD_START_BYTES = new short[] { 0x8d, 0x0a };

    private static final short[] CMD_END_BYTES = new short[] { 0xd8 };

    private static final short[] CMD_MAIN_LED_RGB = new short[] { 0x1a, 0x0e, };

    private static short COUNTER = 1;

    private static ByteArrayOutputStream createBaseCommand() {
        ByteArrayOutputStream os = new ByteArrayOutputStream(2);
        for (short b : CMD_START_BYTES) {
            os.write(b);
        }

        return os;
    }

    public static byte[] createRgbCommand(short red, short green, short blue) {
        ByteArrayOutputStream os = createBaseCommand();
        for (short b : CMD_MAIN_LED_RGB) {
            os.write(b);
        }

        os.write((byte) COUNTER++);
        os.write((byte) 0x00);
        os.write((byte) 0x0e);
        os.write((byte) red);
        os.write((byte) green);
        os.write((byte) blue);

        os.write((byte) 0xa2);

        for (short b : CMD_END_BYTES) {
            os.write(b);
        }

        byte[] rtn = os.toByteArray();

        StringBuilder builder = new StringBuilder();
        for (byte b : rtn) {
            builder.append(String.format("%02X ", b));
        }

        Log.d(TAG, "createRgbCommand: " + builder.toString());

        return os.toByteArray();
    }

    public static byte[] createTurnOnLedCommand() {
        // 8d 0a 1a 0e 03 00 01 00 c9 d8
        ByteArrayOutputStream os = createBaseCommand();
        for (short b : CMD_MAIN_LED_RGB) {
            os.write(b);
        }

        os.write((byte) COUNTER++);
        os.write(0x00);
        os.write(0x01);
        os.write(0xc9);

        for (short b : CMD_END_BYTES) {
            os.write(b);
        }

        return os.toByteArray();
    }
}
