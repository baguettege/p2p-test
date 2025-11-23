package main;

import network.Connection;
import network.packets.*;
import util.FileUtil;

import java.io.IOException;

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
            case "Ping" -> ping((Ping) packet);
            case "Message" -> message((Message) packet);
            case "Verify" -> verify();
            case "Disconnect" -> disconnect((Disconnect) packet);
            case "Auth" -> auth((Auth) packet);
            case "KeepAlive" -> keepAlive();
            case "Data" -> data((Data) packet);
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

    private void ping(Ping packet) {
        if (packet.isReturning()) { // was sent by self
            long timeTaken = System.currentTimeMillis() - packet.getTimestamp();
            connection.logConsole("Ping echoed successfully in " + timeTaken + "ms");

        } else { // was sent by peer, so echo back
            packet.setReturning();
            connection.writePacket(packet);
        }
    }

    private void keepAlive() {
        // do nothing, purely so socket does not close on its own
    }

    private void data(Data packet) {
        connection.logConsole("Received file: " + packet.getFileName() + " | " + FileUtil.getFileSize(packet.getLength()));

        try {
            packet.saveTo(FileUtil.getDownloadsDir());
        } catch (IOException e) {
            e.printStackTrace();
            connection.logConsole("Error whilst saving file: " + e.getMessage());
        }
    }
}
