package processors;

import network.Peer;
import network.packets.*;

public class PacketProcessor {
    private final Peer peer;

    // take a packet received by a peer and process it

    public PacketProcessor(Peer conn) {
        peer = conn;
    }

    private void log(String logText) {
        peer.log(logText);
    }

    public void handle(Packet packet) {
        String id = packet.getId();

        //log("PACKET ID: " + id); //debug

        boolean isAccepted = peer.connectionVerifier().isAccepted();
        boolean isAuthorized = peer.connectionVerifier().isPeerAuthorized();

        if (!isAccepted && !id.equals("Accept")) return; // before auth -> only accept packets allowed
        if (isAccepted && !isAuthorized && !id.equals("Auth")) return; // before auth but accepted -> only accept auth packets

        switch (id) {
            case "Ping" -> ping((Ping) packet);
            case "Message" -> message((Message) packet);
            case "Accept" -> peer.connectionVerifier().accept();
            case "Auth" -> auth((Auth) packet);
            case "KeepAlive" -> {} // do nothing; is keeping socket alive
            case "FileData" -> peer.fileProcessor().processData((FileData) packet);
            case "FileHeader" -> peer.fileProcessor().processHeader((FileHeader) packet);
            case "FileFooter" -> peer.fileProcessor().processFooter();
            case "FileResponse" -> peer.fileProcessor().processResponse((FileResponse) packet);
            case "FileRequest" -> peer.fileProcessor().processRequest((FileRequest) packet);
            case "FileCancelUpload" -> peer.fileProcessor().cancelDownload(true); // we reverse these 2 as the packet means the peer is cancelling upload
            case "FileCancelDownload" -> peer.fileProcessor().cancelUpload(true); // so we must therefore cancel the download
            default -> unknownPacket(packet);
        }
    }

    private void unknownPacket(Packet packet) {
        log("Unknown packet received: " + packet.getId() + " | " + packet);
    }

    private void message(Message packet) {
        log("MSG - " + packet.getText());
    }

    private void auth(Auth packet) {
        String status = packet.getStatus();

        if (!status.isEmpty()) {
            log("Auth: " + status);
        }

        if (status.equals("Connection authorized")) {
            peer.connectionVerifier().authorize();
        }
    }

    private void ping(Ping packet) {
        if (packet.isReturning()) { // was sent by self
            long timeTaken = System.currentTimeMillis() - packet.getTimestamp();
            log("Ping echoed successfully in " + timeTaken + "ms");

        } else { // was sent by peer, so echo back
            packet.setReturning();
            peer.writePacket(packet);
        }
    }
}
