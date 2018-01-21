package com.raidzero.sphero.bluetooth;

import java.util.UUID;

/**
 * Created by raidzero on 1/21/18.
 */

public class SpheroCommand {
    public UUID service;
    public UUID chracteristic;
    public byte[] data;

    public static SpheroCommand createCommand(UUID service, UUID characteristic, byte[] data) {
        SpheroCommand command = new SpheroCommand();
        command.service = service;
        command.chracteristic = characteristic;
        command.data = data;

        return command;
    }
}
