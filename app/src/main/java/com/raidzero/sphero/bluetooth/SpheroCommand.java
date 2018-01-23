package com.raidzero.sphero.bluetooth;

import java.util.UUID;

/**
 * Created by raidzero on 1/21/18.
 */

public class SpheroCommand {

    enum CommandType {
        WRITE_CHARACTERISTIC,
        READ_CHARACTERISTIC,
        SUBSCRIBE_CHARACTERISTIC_NOTIFICATIONS
    }

    public UUID service;
    public UUID characteristic;
    public byte[] data;
    public CommandType commandType;

    public static SpheroCommand createWriteCommand(UUID service, UUID characteristic, byte[] data) {
        SpheroCommand command = new SpheroCommand();
        command.commandType = CommandType.WRITE_CHARACTERISTIC;
        command.service = service;
        command.characteristic = characteristic;
        command.data = data;

        return command;
    }

    public static SpheroCommand createSubscribeCommand(UUID service, UUID characteristic) {
        SpheroCommand command = new SpheroCommand();
        command.commandType = CommandType.SUBSCRIBE_CHARACTERISTIC_NOTIFICATIONS;
        command.service = service;
        command.characteristic = characteristic;

        return command;
    }

    public static SpheroCommand createReadCommand(UUID service, UUID characteristic) {
        SpheroCommand command = new SpheroCommand();
        command.commandType = CommandType.READ_CHARACTERISTIC;
        command.service = service;
        command.characteristic = characteristic;

        return command;
    }
}
