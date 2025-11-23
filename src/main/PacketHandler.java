package main;

import network.Connection;
import network.packets.*;

public class PacketHandler {
    private final Connection connection;

    // take a packet received by a peer and process it

    public PacketHandler(Connection conn) {
        connection = conn;
    }

    public void handle(Packet packet) {
        String id = packet.getId();
        //Main.logMainConsole("PACKET RECEIVED: " + id);

        if (!connection.isVerified() && !id.equals("Verify")) return; // ignore non verify packets before verification
        if (!connection.isAuthenticated() && connection.isVerified()) return; // ignore packets if not authenticated yet

        switch (id) {
            case "Message" -> message((Message) packet);
            case "Verify" -> verify();
            case "Disconnect" -> disconnect((Disconnect) packet);
            case "Auth" -> auth((Auth) packet);
            default -> unknownPacket(packet);
        }
    }

    private void unknownPacket(Packet packet) {
        Main.logMainConsole("Unknown packet received: " + packet.getId() + " | " + packet);
    }

    private void message(Message packet) {
        connection.logConsole("MSG - " + packet.getText());
    }

    private void verify() {
        connection.verify();
    }

    private void disconnect(Disconnect packet) {
        connection.close(packet.getReason());
    }

    private void auth(Auth packet) {
        connection.logConsole("Authentication result: " + packet.getStatus());
    }
}
