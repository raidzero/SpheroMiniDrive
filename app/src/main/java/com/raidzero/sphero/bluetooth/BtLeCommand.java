package com.raidzero.sphero.bluetooth;

import java.util.UUID;

/**
 * Created by raidzero on 1/21/18.
 */

public class BtLeCommand {

    enum CommandType {
        WRITE_CHARACTERISTIC,
        READ_CHARACTERISTIC,
        SUBSCRIBE_CHARACTERISTIC_NOTIFICATIONS
    }

    public UUID service;
    public UUID characteristic;
    public byte[] data;
    public CommandType commandType;
    public int duration = 50;

    public static BtLeCommand createWriteCommand(UUID service, UUID characteristic, byte[] data) {
        BtLeCommand command = new BtLeCommand();
        command.commandType = CommandType.WRITE_CHARACTERISTIC;
        command.service = service;
        command.characteristic = characteristic;
        command.data = data;

        return command;
    }


    public static BtLeCommand createWriteCommand1c(String data) {
        return BtLeCommand.createWriteCommand(
                Constants.UUID_SERVICE_COMMAND,
                Constants.UUID_CHARACTERISTIC_HANDLE_1C,
                Sphero.hexStringToBytes(data)
        );
    }

    public static BtLeCommand createWriteCommand1c(String data, int duration) {
        BtLeCommand cmd = BtLeCommand.createWriteCommand(
                Constants.UUID_SERVICE_COMMAND,
                Constants.UUID_CHARACTERISTIC_HANDLE_1C,
                Sphero.hexStringToBytes(data));
        cmd.duration = duration;
        return cmd;
    }

    public static BtLeCommand createSubscribeCommand(UUID service, UUID characteristic) {
        BtLeCommand command = new BtLeCommand();
        command.commandType = CommandType.SUBSCRIBE_CHARACTERISTIC_NOTIFICATIONS;
        command.service = service;
        command.characteristic = characteristic;

        return command;
    }

    public static BtLeCommand createReadCommand(UUID service, UUID characteristic) {
        BtLeCommand command = new BtLeCommand();
        command.commandType = CommandType.READ_CHARACTERISTIC;
        command.service = service;
        command.characteristic = characteristic;

        return command;
    }
}
