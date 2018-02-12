package com.raidzero.sphero.bluetooth;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by raidzero on 1/26/18.
 */

public class SpheroCommand {
    private static final String TAG = "SpheroCommand";

    private static final byte[] CMD_START_BYTES =       new byte[] { (byte) 0x8d, (byte) 0x0a}; // 8d:0a
    private static final byte[] CMD_END_BYTES =         new byte[] { (byte) 0xd8 }; // d8

    private static final byte[] CMD_LED_BYTES =         new byte[] { (byte) 0x1a, (byte) 0x0e }; // 1a:0e
    private static final byte[] CMD_RAW_MOTOR_BYTES =   new byte[] { (byte) 0x16, (byte) 0x01 }; // 16:01
    private static final byte[] CMD_ROLL_BYTES =        new byte[] { (byte) 0x16, (byte) 0x07 }; //16:07

    private static short COUNTER = 1;

    private static byte calculateChecksumByte(byte[] data) {
        int sum = 0;
        for (byte b : data) {
            sum += b;
        }

        return (byte) ((((sum % 256) ^ 0xff) + data[0]) & 0xff);
    }

    private static byte[] createCommand(byte[] command, byte[] data) {
        ByteBuffer cmd = ByteBuffer.allocate(
                + CMD_START_BYTES.length
                + command.length
                + 1 /* counter */ + data.length + 1 /* checksum */
                + CMD_END_BYTES.length);
        cmd.put(CMD_START_BYTES);
        cmd.put(command);
        cmd.put((byte) (COUNTER++ & 0xff));
        cmd.put(data);
        cmd.put(calculateChecksumByte(Arrays.copyOfRange(cmd.array(), 0, 5 + data.length)));
        cmd.put(CMD_END_BYTES);

        return cmd.array();
    }

    static byte[] createRgbCommand(short red, short green, short blue) {
        // 8D 0A 1A 0E 02 00 0E 00 FF 00 BE D8
        // data: 00 0e RR GG BB
        ByteBuffer bytes = ByteBuffer.allocate(5);

        bytes.put(new byte[] {(byte) 0x00, (byte) 0x0e});
        bytes.put((byte) (red & 0xff));
        bytes.put((byte) (green & 0xff));
        bytes.put((byte) (blue & 0xff));

        return createCommand(CMD_LED_BYTES, bytes.array());
    }

    static byte[] createRearLedCommand(boolean on) {
        ByteBuffer bytes = ByteBuffer.allocate(3);
        int brightness = on ? 0xff : 0x00;

        bytes.put(new byte[] { (byte) 0x00, (byte) 0x01 });
        bytes.put((byte) (brightness & 0xff));

        return createCommand(CMD_LED_BYTES, bytes.array());
    }

    static byte[] createRawMotorCommand(int left, int right) {
        int leftDir, rightDir;

        // get the direction based on positive/negative values for left & right motors
        leftDir = left > 0 ? 0x01 : 0x02;
        rightDir = right > 0 ? 0x01 : 0x02;
        // chop off sign on left & right power levels since it was only used to indicate forward/backward
        left = Math.abs(left);
        right = Math.abs(right);

        ByteBuffer buffer = ByteBuffer.allocate(4);

        buffer.put(new byte[] {
                (byte) leftDir, (byte) (left & 0xff),
                (byte) rightDir, (byte) (right & 0xff)
        });

        return createCommand(CMD_RAW_MOTOR_BYTES, buffer.array());
    }

    static byte[] createRollCommand(int speed, int heading, int aim) {
        heading = (aim + heading) % 360;

        int headingByte1 = heading & 0xff; // 360: 104
        int headingByte2 = (heading >> 8) & 0xff; // 360:

        speed = speed & 0xff;

        ByteBuffer dataBytes = ByteBuffer.allocate(4);

        dataBytes.put(new byte[] {
                (byte) speed, (byte) headingByte2, (byte) headingByte1, (byte) 0x00
        });

        return createCommand(CMD_ROLL_BYTES, dataBytes.array());
    }

    static List<byte[]> createDisconnectCommands() {
        /*
        just duplicate this blindly
        writeData: 8D 0A 13 01 06 DB D8
        writeData: 8D 0A 13 02 07 D9 D8
        writeData: 8D 0A 13 03 08 D7 D8
        writeData: 8D 0A 13 04 09 D5 D8
        writeData: 8D 0A 13 05 0A D3 D8
        writeData: 8D 0A 13 06 0B D1 D8
        writeData: 8D 0A 13 07 0C CF D8
        writeData: 8D 0A 13 08 0D CD D8
        writeData: 8D 0A 13 09 0E CB D8
        writeData: 8D 0A 13 0A 0F C9 D8
        writeData: 8D 0A 13 0B 10 C7 D8
         */
        List<byte[]> rtn = new ArrayList<byte[]>();

        for (int i = 1; i < 12; i++) {
            byte[] cmd = new byte[7];
            cmd[0] = (byte) 0x8d;
            cmd[1] = (byte) 0x0a;
            cmd[2] = (byte) 0x13;
            cmd[3] = (byte) i;
            cmd[4] = (byte) (COUNTER++ & 0xff);
            // calculate checksum using only bytes currently added
            cmd[5] = calculateChecksumByte(Arrays.copyOfRange(cmd, 0, 5));
            cmd[6] = (byte) 0xd8;

            rtn.add(cmd);
        }

        return rtn;
    }
}
