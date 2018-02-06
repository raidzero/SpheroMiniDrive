package com.raidzero.sphero.bluetooth;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by raidzero on 1/26/18.
 */

public class SpheroCommand {
    private static final String TAG = "SpheroCommand";

    private static byte COUNT = 0x0;

    private static final String CMD_START_BYTES = "8d:0a"; //new short[] { 0x8d, 0x0a };

    private static final String CMD_END_BYTES = "d8"; // new short[] { 0xd8 };

    private static final String CMD_MAIN_LED_RGB_BYTES = "1a:0e"; //new short[] { 0x1a, 0x0e, };

    private static final String CMD_RAW_MOTOR_BYTES = "16:01"; //new short[] { 0x16, 0x01, };
    private static final String CMD_ROTATE_BYTES = "16:07"; //new short[] { 0x16, 0x07, };

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
        sb.append(CMD_MAIN_LED_RGB_BYTES + ":");
        sb.append(String.format("%02X:", COUNTER++ & 0xff));
        sb.append("00:0e:");
        sb.append(String.format("%02X:%02X:%02X:", red & 0xff, green & 0xff, blue & 0xff));
        String checkSum = calculateCheckSum(Sphero.hexStringToBytes(sb.toString()));
        sb.append(checkSum);
        sb.append(CMD_END_BYTES);

        Log.d(TAG, "createRgbCommand: " + sb.toString());
        return sb.toString();
    }

    // takes all bytes up until checksum byte
    private static String calculateCheckSum(byte[] data) {
        int sum = 0;
        for (byte b : data) {
            sum += b;
        }

        int checksum = (((sum % 256) ^ 0xff) + data[0]) & 0xff;

        return String.format("%02x:", checksum);
    }

    public static byte[] createRgbCommand(short red, short green, short blue) {
        return Sphero.hexStringToBytes(createRgbString(red, green, blue));
    }

    public static String createDisconnectString() {
        StringBuilder sb = new StringBuilder();

        sb.append(CMD_START_BYTES + ":");
        sb.append("13:01:");
        sb.append(String.format("%02X:", COUNTER++ & 0xff));
        sb.append(calculateCheckSum(Sphero.hexStringToBytes(sb.toString())));
        sb.append(CMD_END_BYTES);

        Log.d(TAG, "createDisconnectString: " + sb.toString());
        return sb.toString();
    }

    public static List<String> createDisconnectStrings() {
        List<String> rtn = new ArrayList<String>();

        // just blast every endpoint from 13:00 to 13:12
        for (int i = 1; i < 12; i++) {
            StringBuilder sb = new StringBuilder();

            sb.append(CMD_START_BYTES + ":");
            sb.append(String.format("13:%02X:", i & 0xff));
            sb.append(String.format("%02X:", COUNTER++ & 0xff));
            sb.append(calculateCheckSum(Sphero.hexStringToBytes(sb.toString())));
            sb.append(CMD_END_BYTES);
            rtn.add(sb.toString());
        }

        return rtn;
    }

    public static byte[] createDisconnectCommand() {
        return Sphero.hexStringToBytes(createDisconnectString());
    }

    public static String createRearLedCommand(byte brightness) {
        /*
        8d:0a:1a:0e:CC:00:01:ff:CS:d8 (on)
        8d:0a:1a:0e:CC:00:01:00:CS:d8 (off)
         */
        StringBuilder sb = new StringBuilder();

        sb.append(CMD_START_BYTES + ":");
        sb.append(CMD_MAIN_LED_RGB_BYTES + ":" );
        sb.append(String.format("%02X:", COUNTER++ & 0xff));
        sb.append("00:01:");
        sb.append(String.format("%02X:", brightness & 0xff));
        String checkSum = calculateCheckSum(Sphero.hexStringToBytes(sb.toString()));
        sb.append(checkSum);
        sb.append(CMD_END_BYTES);

        return sb.toString();
    }

    public static String createRawMotorCommand(int left, int right) {
        /*
        8d:0a:16:01:1d:01:ff:01:ff:c1:d8 // full speed to left & right
        8d:0a:16:01:1e:01:00:01:00:be:d8 // turn off both left & right
        8d:0a:16:01:1d:02:ff:02:ff:c1:d8 // full speed to left & right (backwards)
        8d:0a:16:01:1e:02:00:02:00:be:d8 // turn off both left & right (backwards)
         */
        int leftDir, rightDir;

        // get the direction based on positive/negative values for left & right motors
        leftDir = left > 0 ? 0x01 : 0x02;
        rightDir = right > 0 ? 0x01 : 0x02;
        // chop off sign on left & right power levels since it was only used to indicate forward/backward
        left = Math.abs(left);
        right = Math.abs(right);

        StringBuilder sb = new StringBuilder();

        sb.append(CMD_START_BYTES + ":");
        sb.append(CMD_RAW_MOTOR_BYTES + ":");
        sb.append(String.format("%02X:", COUNTER++ & 0xff));
        sb.append(String.format("%02X:%02X:%02X:%02X:", leftDir, left & 0xff, rightDir, right & 0xff));
        sb.append(calculateCheckSum(Sphero.hexStringToBytes(sb.toString())));
        sb.append(CMD_END_BYTES);

        return sb.toString();
    }

    public static String createRotateCommand(int force) {
        /*
        8d:0a:16:07:13:00:01:62:04:5e:d8 // rotates a little to the right
        8d:0a:16:07:2e:00:00:f4:04:b2:d8 // rotates a lot to the left
        */
        int direction = force > 0 ? 0x01 : 0x00;
        force = Math.abs(force);

        StringBuilder sb = new StringBuilder();

        sb.append(CMD_START_BYTES + ":");
        sb.append(CMD_ROTATE_BYTES +":");
        sb.append(String.format("%02X:", COUNTER++ & 0xff));
        sb.append(String.format("00:%02X:%02X:04:", direction, force & 0xff));
        sb.append(calculateCheckSum(Sphero.hexStringToBytes(sb.toString())));
        sb.append(CMD_END_BYTES);

        return sb.toString();
    }
}
