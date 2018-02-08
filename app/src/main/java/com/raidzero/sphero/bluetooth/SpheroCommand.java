package com.raidzero.sphero.bluetooth;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static com.raidzero.sphero.global.ByteUtils.hexStringToBytes;

/**
 * Created by raidzero on 1/26/18.
 */

public class SpheroCommand {
    private static final String TAG = "SpheroCommand";

    private static final String CMD_START_BYTES = "8d:0a"; //new short[] { 0x8d, 0x0a };

    private static final String CMD_END_BYTES = "d8"; // new short[] { 0xd8 };

    private static final String CMD_MAIN_LED_RGB_BYTES = "1a:0e"; //new short[] { 0x1a, 0x0e, };

    private static final String CMD_RAW_MOTOR_BYTES = "16:01"; //new short[] { 0x16, 0x01, };
    private static final String CMD_ROLL_BYTES = "16:07"; //new short[] { 0x16, 0x07, };

    private static short COUNTER = 1;

    // takes all bytes up until checksum byte
    private static String calculateCheckSum(byte[] data) {
        int sum = 0;
        for (byte b : data) {
            sum += b;
        }

        int checksum = (((sum % 256) ^ 0xff) + data[0]) & 0xff;

        return String.format("%02x:", checksum);
    }

    static String createRgbCommand(short red, short green, short blue) {
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
        String checkSum = calculateCheckSum(hexStringToBytes(sb.toString()));
        sb.append(checkSum);
        sb.append(CMD_END_BYTES);

        Log.d(TAG, "createRgbCommand: " + sb.toString());
        return sb.toString();
    }

    static String createRearLedCommand(boolean on) {
        /*
        8d:0a:1a:0e:CC:00:01:ff:CS:d8 (on)
        8d:0a:1a:0e:CC:00:01:00:CS:d8 (off)
         */
        StringBuilder sb = new StringBuilder();
        int brightness = on ? 0xff : 0x00;

        sb.append(CMD_START_BYTES + ":");
        sb.append(CMD_MAIN_LED_RGB_BYTES + ":" );
        sb.append(String.format("%02X:", COUNTER++ & 0xff));
        sb.append("00:01:");
        sb.append(String.format("%02X:", brightness & 0xff));
        String checkSum = calculateCheckSum(hexStringToBytes(sb.toString()));
        sb.append(checkSum);
        sb.append(CMD_END_BYTES);

        return sb.toString();
    }

    static String createRawMotorCommand(int left, int right) {
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
        sb.append(calculateCheckSum(hexStringToBytes(sb.toString())));
        sb.append(CMD_END_BYTES);

        return sb.toString();
    }

    static String createRotateCommand(int force) {
        /*
        8d:0a:16:07:13:00:01:62:04:5e:d8 // rotates a little to the right
        8d:0a:16:07:2e:00:00:f4:04:b2:d8 // rotates a lot to the left
        */
        int direction = force > 0 ? 0x01 : 0x00;
        force = Math.abs(force);

        StringBuilder sb = new StringBuilder();

        sb.append(CMD_START_BYTES + ":");
        sb.append(CMD_ROLL_BYTES +":");
        sb.append(String.format("%02X:", COUNTER++ & 0xff));
        sb.append(String.format("00:%02X:%02X:04:", direction, force & 0xff));
        sb.append(calculateCheckSum(hexStringToBytes(sb.toString())));
        sb.append(CMD_END_BYTES);

        return sb.toString();
    }

    static String createRollCommand(int speed, int heading, int aim) {
        /*
        8d:0a:16:07:CC:HE:HE:SP:00:CS:d8
        8d:0a:16:07:99:da:00:eb:00:7a:d8
        8d:0a:16:07:9b:dc:00:eb:00:76:d8
        */

        // adjust heading based on aim
        /*
        pointing at me is 57 degrees on joystick (aim)
        pulling down is 180 but it actually is going 180 - 57
        so the solution is to add aim to heading, wrapping if necessary
         */

        heading = (aim + heading) % 360;

        int headingByte1 = heading & 0xff; // 360: 104
        int headingByte2 = (heading >> 8) & 0xff; // 360:

        speed = speed & 0xff;

        StringBuilder sb = new StringBuilder();
        sb.append(CMD_START_BYTES + ":");
        sb.append(CMD_ROLL_BYTES + ":");
        sb.append(String.format("%02X:", COUNTER++ & 0xff));

        // 8d:0a:16:07:74:2f:01:66:00:ce:d8
        // speed first and then heading
        sb.append(String.format("%02X:%02X:%02X:00:", speed, headingByte2, headingByte1));

        sb.append(calculateCheckSum(hexStringToBytes(sb.toString())));
        sb.append(CMD_END_BYTES);

        Log.d(TAG, "createRollCommand: speed: " + speed + " heading: " + heading + ": " + sb.toString());
        return sb.toString();
    }

    static List<String> createDisconnectStrings() {
        List<String> rtn = new ArrayList<String>();

        // just blast every endpoint from 13:00 to 13:12
        for (int i = 1; i < 12; i++) {
            StringBuilder sb = new StringBuilder();

            sb.append(CMD_START_BYTES + ":");
            sb.append(String.format("13:%02X:", i & 0xff));
            sb.append(String.format("%02X:", COUNTER++ & 0xff));
            sb.append(calculateCheckSum(hexStringToBytes(sb.toString())));
            sb.append(CMD_END_BYTES);
            rtn.add(sb.toString());
        }

        return rtn;
    }
}
