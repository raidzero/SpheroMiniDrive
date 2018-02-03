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

    private static final String CMD_START_BYTES = "8d:0a"; //new short[] { 0x8d, 0x0a };

    private static final String CMD_END_BYTES = "d8"; // new short[] { 0xd8 };

    private static final String CMD_MAIN_LED_RGB = "1a:0e"; //new short[] { 0x1a, 0x0e, };

    private static short COUNTER = 1;

    private static byte[] createBaseCommand() {
        return Sphero.hexStringToBytes(CMD_START_BYTES);
    }

    public static String createRgbString(short red, short green, short blue) {
        StringBuilder sb = new StringBuilder();

        /*
        8d 0a (common with all), 1a 0e (seems to be command), 00 (command number - increments),
        00 0e, (common with all rgb commands), RR GG BB, then ?? (chec), then d8
         */
        sb.append(CMD_START_BYTES + ":");
        sb.append(CMD_MAIN_LED_RGB + ":");
        sb.append(String.format("%02X:", COUNTER++));
        sb.append("00:0e:");
        sb.append(String.format("%02X:%02X:%02X:", red, green, blue));
        sb.append("a2:"); // checksum?
        sb.append(CMD_END_BYTES);

        Log.d(TAG, "createRgbCommand: " + sb.toString());
        return sb.toString();
    }

    public static byte[] createRgbCommand(short red, short green, short blue) {
        return Sphero.hexStringToBytes(createRgbString(red, green, blue));
    }

    public static String createDisconnectString() {
        StringBuilder sb = new StringBuilder();

        sb.append(CMD_START_BYTES + ":");
        sb.append("13:01:");
        sb.append(String.format("%02X:", COUNTER++));
        sb.append("84:"); // checksum?
        sb.append(CMD_END_BYTES);

        Log.d(TAG, "createDisconnectString: " + sb.toString());
        return sb.toString();
    }

    public static byte[] createDisconnectCommand() {
        return Sphero.hexStringToBytes(createDisconnectString());
    }

    /*
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
    */
}
