package main;

import network.Connection;
import network.packets.DataBytes;
import network.packets.DataEnd;
import network.packets.DataStart;

public class FileTransferHandler {
    private Connection connection;

    enum SendState {
        IDLE,
        REQUESTED,
        SENDING
    }

    enum ReceiveState {
        IDLE,
        REQUESTING,
        RECEIVING
    }

    private volatile SendState sendState = SendState.IDLE;
    private volatile ReceiveState receiveState = ReceiveState.IDLE;

    public void handleStart(DataStart packet) {

    }

    public void handleBytes(DataBytes packet) {

    }

    public void handleEnd(DataEnd packet) {

    }
}
