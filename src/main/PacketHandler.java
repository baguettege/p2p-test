package main;

import network.Connection;
import network.packets.*;
import util.FileUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
            case "DataBytes" -> dataBytes((DataBytes) packet);
            case "DataStart" -> dataStart((DataStart) packet);
            case "DataEnd" -> dataEnd((DataEnd) packet);
            case "DataResponse" -> dataResponse((DataResponse) packet);
            case "DataRequest" -> dataRequest((DataRequest) packet);
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


    // file transfers
    // ----------------------------------------------------------------

    private OutputStream output;
    private String fileName;
    private long fileSize;
    private long bytesReceived;
    private int expectedIndex;

    private String fmtName;
    private int lastPercent = -1;

    private boolean allowFileReceive = false;
    private boolean isReceivingFile = false;

    public void allowFileReceive() {
        allowFileReceive = true;
    }

    private void dataStart(DataStart packet) {
        if (!allowFileReceive) {
            connection.logConsole("Received DataStart packet while allowing data to be received!");
            return;
        }
        if (isReceivingFile) {
            connection.logConsole("Cannot receive multiple files at once");
            return;
        }
        isReceivingFile = true;

        this.fileName = packet.getFileName();
        this.fileSize = packet.getFileSize();
        this.bytesReceived = 0;
        this.expectedIndex = 0;

        fmtName = FileUtil.getFileNameWithTime(fileName);
        Path target = FileUtil.getDownloadsDir().resolve(fmtName);
        connection.logConsole("Receiving file: " + fmtName + " | " + FileUtil.getFileSize(fileSize));

        try {
            this.output = Files.newOutputStream(target);
        } catch (IOException e) {
            e.printStackTrace();
            connection.logConsole("Error when creating file output stream: " + e.getMessage());
        }
    }

    private void dataBytes(DataBytes packet) {
        if (!allowFileReceive) {
            connection.logConsole("Received DataBytes packet while allowing data to be received!");
            return;
        }

        int index = packet.getIndex();

        if (index != expectedIndex) {
            connection.logConsole("WARN - Expected chunk index " + expectedIndex + " but got " + index + "!");
        }

        int length = packet.getLength();
        try {
            output.write(packet.getData(), 0, length);
            //connection.logConsole("Wrote chunk");
        } catch (IOException e) {
            e.printStackTrace();
            connection.logConsole("Error when writing chunk to disk: " + e.getMessage());
        }

        // update
        bytesReceived += length;
        expectedIndex++;

        // progress checking
        String received = FileUtil.getFileSize(bytesReceived);
        String total = FileUtil.getFileSize(fileSize);
        double percent = ((double) bytesReceived /fileSize) * 100.0;
        int wholePercent = (int) percent;

        if (wholePercent % 10 == 0 && wholePercent != lastPercent) {
            connection.logConsole("File receive progress: " + received + "/" + total + " - " + wholePercent + "%");
            lastPercent = wholePercent;
        };
    }

    private void dataEnd(DataEnd packet) {
        if (!allowFileReceive) {
            connection.logConsole("Received DataEnd packet while allowing data to be received!");
            return;
        }

        try {
            if (output != null) {
                output.close();
            }

            connection.logConsole("Successfully wrote file to disk: " + fmtName);
            clearDataStates();

        } catch (IOException e) {
            connection.logConsole("Error closing file: " + e.getMessage());
        }
    }

    private void clearDataStates() {
        output = null;
        fileName = null;
        fmtName = null;
        fileSize = 0;
        bytesReceived = 0;
        expectedIndex = 0;
        lastPercent = -1;
        allowFileReceive = false;
        isReceivingFile = false;
    }

    private void dataResponse(DataResponse packet) {
        boolean isAccepting = packet.getResponse();
        boolean isWritingFile = connection.isWritingFile();

        if (isAccepting && isWritingFile) {
            connection.logConsole("Peer accepted file send request, sending...");
            connection.writeFile();
        } else if (!isAccepting && isWritingFile) {
            connection.logConsole("Peer declined file send request");
            connection.stopWritingFile();
        } else {
            // peer sent a DataResponse packet when self did not request to send file; ignore this
        }
    }

    private boolean hasReceivedDataRequest = false;

    public boolean hasReceivedDataRequest() { return hasReceivedDataRequest; }
    public void stopHasReceivedDataRequest() { hasReceivedDataRequest = false; }

    private void dataRequest(DataRequest packet) {
        if (isReceivingFile) {
            connection.logConsole("Peer requested file transfer while already receiving a file; declining");
            connection.writePacket(new DataResponse(false));
            return;
        }

        hasReceivedDataRequest = true;
        connection.logConsole("Peer requested file transfer: " + packet.getFileName() + " | " + FileUtil.getFileSize(packet.getFileSize()));
        connection.logConsole("cmd 'accept' or 'decline' for this request");
    }
}
