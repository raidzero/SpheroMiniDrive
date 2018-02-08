package com.raidzero.sphero.global;

import java.io.ByteArrayOutputStream;

/**
 * Created by posborn on 2/6/18.
 */

public class ByteUtils {
    // convert byte array to string, XX:XX:XX:XX
    public static String bytesToString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02X ", b));
        }

        return builder.toString();
    }

    // converts colon-separated string of hex digits (like wireshark gives) to byte array
    public static byte[] hexStringToBytes(String hexStr) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String[] hex = hexStr.split(":");

        for (String s : hex) {
            byte b = (byte) ((Character.digit(s.charAt(0), 16) << 4)
                    + Character.digit(s.charAt(1), 16));
            os.write(b);
        }

        return os.toByteArray();
    }
}
