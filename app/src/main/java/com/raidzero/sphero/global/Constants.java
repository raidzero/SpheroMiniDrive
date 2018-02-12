package com.raidzero.sphero.global;

import java.util.UUID;

/**
 * Created by raidzero on 1/21/18.
 */

public class Constants {
    public static final UUID UUID_DESCRIPTOR_NOTIFY = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final UUID UUID_SERVICE_COMMAND = UUID.fromString("00010001-574f-4f20-5370-6865726f2121");
    public static final UUID UUID_SERVICE_INITIALIZE = UUID.fromString("00020001-574f-4f20-5370-6865726f2121");
    public static final UUID UUID_SERVICE_BATTERY = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");


    public static final UUID UUID_CHARACTERISTIC_BATTERY = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_HANDLE_1C = UUID.fromString("00010002-574f-4f20-5370-6865726f2121");
    public static final UUID UUID_CHARACTERISTIC_USETHEFORCE = UUID.fromString("00020005-574f-4f20-5370-6865726f2121");

    // magic data to get the sphero to listen to us and not immediately disconnect gatt
    // ...usetheforce. ..band
    public static final String STR_USE_THE_FORCE_BYTES = "75:73:65:74:68:65:66:6f:72:63:65:2e:2e:2e:62:61:6e:64";

    public static final byte[] USE_THE_FORCE_BYTES = new byte[] {
            0x75, 0x73, 0x65, 0x74, 0x68, 0x65, 0x72, 0x63, 0x65, 0x2e, 0x2e, 0x2e, 0x62, 0x61, 0x6e, 0x64
    };
}
