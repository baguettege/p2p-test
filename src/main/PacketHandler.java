package main;

import gui.Console;
import network.packets.*;

public class PacketHandler {
    private Console console;

    public PacketHandler(Console console) {
        this.console = console;
    }

    public void handle(Packet packet) {
        switch (packet.getId()) {
            case "Message" -> message((Message) packet);
        }
    }

    private void message(Message packet) {
        console.log("MSG - " + packet.getText());
    }
}
