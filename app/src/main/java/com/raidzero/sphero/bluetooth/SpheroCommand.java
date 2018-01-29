package com.raidzero.sphero.bluetooth;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by raidzero on 1/26/18.
 */

public class SpheroCommand {
    private static byte COUNT = 0x0;

    private static final short[] CMD_START_BYTES = new short[] { 0x8d, 0x0a };

    private static final short[] CMD_END_BYTES = new short[] { 0xd8 };

    // command IDs that I know of
    enum CID {
        MAIN_RGB(0x1A);

        private byte byteCode = 0x00;

        CID(int code) {
            this.byteCode = (byte) code;
        }

        public byte getByteCode() {
            return byteCode;
        }
    }

    private static ByteArrayOutputStream createBaseCommand() {
        ByteArrayOutputStream os = new ByteArrayOutputStream(2);
        for (short b : CMD_START_BYTES) {
            os.write(b);
        }

        return os;
    }

    public static short[] createRgbCommand(short red, short green, short blue) {
        ByteArrayOutputStream os = createBaseCommand();

    }
}
