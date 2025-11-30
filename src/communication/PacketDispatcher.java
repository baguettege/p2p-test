package communication;

import network.Peer;
import network.packets.*;

public class PacketDispatcher {
    private final Peer peer;

    // take a packet received by a peer and process it

    public PacketDispatcher(Peer peer) { this.peer = peer; }

    private void log(String logText) { peer.log("PACKET - " + logText); }

    private void unknownPacket(Packet packet) { log("Unknown packet received: " + packet.getId()); }

    public void handle(Packet packet) {
        String id = packet.getId();

        switch (id) {
            case "Ping" -> ping((Ping) packet);
            case "Message" -> message((Message) packet);
            case "KeepAlive" -> {} // do nothing; is keeping socket alive
            case "FileData" -> peer.fileProcessor().processData((FileData) packet);
            case "FileHeader" -> peer.fileProcessor().processHeader((FileHeader) packet);
            case "FileFooter" -> peer.fileProcessor().processFooter();
            case "FileResponse" -> peer.fileProcessor().processResponse((FileResponse) packet);
            case "FileRequest" -> peer.fileProcessor().processRequest((FileRequest) packet);
            case "FileCancelUpload" -> peer.fileProcessor().cancelDownload(true); // we reverse these 2 as the packet means the peer is cancelling upload
            case "FileCancelDownload" -> peer.fileProcessor().cancelUpload(true); // so we must therefore cancel the download
            case "DHInitialExchange" -> peer.encryptionHandler().handleInitialExchange((DHInitialExchange) packet);
            case "DHKeyExchange" -> peer.encryptionHandler().handleKeyExchange((DHKeyExchange) packet);
            case "Transcript" -> peer.encryptionHandler().handleTranscript((Transcript) packet);
            default -> unknownPacket(packet);
        }
    }

    private void message(Message packet) {
        log("MSG - " + packet.getText());
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
