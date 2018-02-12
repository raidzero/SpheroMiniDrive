package com.raidzero.sphero.bluetooth;

import com.raidzero.sphero.global.Constants;

import java.util.UUID;

/**
 * Created by raidzero on 1/21/18.
 */

class BtLeCommand {

    enum CommandType {
        WRITE_CHARACTERISTIC,
        READ_CHARACTERISTIC,
        SUBSCRIBE_CHARACTERISTIC_NOTIFICATIONS
    }

    UUID service;
    UUID characteristic;
    byte[] data;
    CommandType commandType;
    int duration = 0;

    static BtLeCommand createWriteCommand(UUID service, UUID characteristic, byte[] data) {
        BtLeCommand command = new BtLeCommand();
        command.commandType = CommandType.WRITE_CHARACTERISTIC;
        command.service = service;
        command.characteristic = characteristic;
        command.data = data;

        return command;
    }

    static BtLeCommand createWriteCommand1c(byte[] data) {
        BtLeCommand cmd = BtLeCommand.createWriteCommand(
                Constants.UUID_SERVICE_COMMAND,
                Constants.UUID_CHARACTERISTIC_HANDLE_1C,
                data);
        return cmd;
    }

    static BtLeCommand createSubscribeCommand(UUID service, UUID characteristic) {
        BtLeCommand command = new BtLeCommand();
        command.commandType = CommandType.SUBSCRIBE_CHARACTERISTIC_NOTIFICATIONS;
        command.service = service;
        command.characteristic = characteristic;

        return command;
    }

    static BtLeCommand createReadCommand(UUID service, UUID characteristic) {
        BtLeCommand command = new BtLeCommand();
        command.commandType = CommandType.READ_CHARACTERISTIC;
        command.service = service;
        command.characteristic = characteristic;

        return command;
    }
}
