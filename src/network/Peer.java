package network;

import gui.Console;
import communication.EncryptionManager;
import communication.FileTransferManager;
import main.Main;
import communication.PacketDispatcher;
import network.packets.*;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class Peer implements Runnable {
    private final Socket socket;
    private Console console;

    private final PacketWriter packetWriter = new PacketWriter();
    private final PacketReader packetReader = new PacketReader();

    private final PacketDispatcher packetDispatcher = new PacketDispatcher(this);;
    private final FileTransferManager fileTransferManager = new FileTransferManager(this);
    private final EncryptionManager encryptionManager = new EncryptionManager(this);

    public final String ip;

    private DataOutputStream out;
    private DataInputStream in;

    private boolean firstPacket = true;


    public Peer(Socket socket) { // inbound connection (server)
        initSocket(socket);
        this.socket = socket;
        ip = socket.getInetAddress().getHostAddress();
        initConnection();

        encryptionManager.initServerSide();
    }

    public Peer(Socket socket, int port) { // outbound connection (client)
        initSocket(socket);
        this.socket = socket;
        ip = socket.getInetAddress().getHostAddress() + ":" + port;
        initConnection();
    }

    private void initSocket(Socket socket) {
        try {
            socket.setKeepAlive(true);
            socket.setSoTimeout(30000); // stop random port scanners connecting
        } catch (SocketException e) {
            log("Failure to initSocket(): " + e.getMessage());
        }
    }

    public void log(String logText) { if (console != null) console.log(logText); }

    public EncryptionManager encryptionHandler() { return encryptionManager; }
    public FileTransferManager fileProcessor() { return fileTransferManager; }

    public void onHandshakeCompleted() {
        try {
            socket.setSoTimeout(0);  // disable timeouts for normal encrypted traffic
        } catch (SocketException e) {
            log("Failed to unset socket timeout: " + e.getMessage());
        }
    }

    private void initConnection() {
        try {
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());

        } catch (IOException e) {
            Main.logMain("Error when creating output/input streams: " + e.getMessage());
            disconnect();
            return;
        }

        try { // run on EDT
            SwingUtilities.invokeAndWait(() -> {
                console = Main.window().createConsole(ip, this);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // listen to incoming packets from peer
    @Override
    public void run() {
        while (true) {
            boolean readSuccess;
            if (encryptionManager.encryptionReady()) {
                readSuccess = packetReader.readEncrypted();
            } else {
                readSuccess = packetReader.readPlain();
            }

            if (!readSuccess) break;
        }

        disconnect();
    }

    // send data to the peer
    public synchronized void writePacket(Packet packet) {
        if (isDisconnecting) return;

        if (encryptionManager.encryptionReady()) {
            packetWriter.writeEncrypted(packet);
        } else {
            String id = packet.getId();
            if ("DHInitialExchange".equals(id) || "DHKeyExchange".equals(id)) {
                packetWriter.writePlain(packet);
            } else {
                log("WARN - Attempted to write a plaintext packet that was not DHInitialExchange or DHKeyExchange!");
            }
        }
    }

    // disconnect the peer
    private boolean isDisconnecting;
    public synchronized void disconnect() {
        if (isDisconnecting) return;
        isDisconnecting = true;

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        } catch (IOException _) {}

        log("Peer disconnected");
        Main.logMain("Peer disconnected: " + ip);

        new Thread(() -> {
            try {
                log("Console closing in 10s...");
                Thread.sleep(10000);
                if (console != null) console.close();
            } catch (InterruptedException _) {}
        }).start();
    }

    private class PacketWriter {
        void writeEncrypted(Packet packet) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                dos.writeUTF(packet.getId());
                packet.write(dos);
                dos.flush();

                byte[] plainText = baos.toByteArray();
                byte[] cipherText = encryptionManager.encrypt(plainText);
                if (cipherText == null) return;

                out.writeInt(cipherText.length);
                out.write(cipherText);
                out.flush();

            } catch (IOException e) {
                log("Error when writing encrypted packet: " + e.getMessage());
                disconnect();
            }
        }

        void writePlain(Packet packet) {
            try {
                out.writeUTF(packet.getId());
                packet.write(out);
                out.flush();

            } catch (IOException e) {
                log("Error when writing plaintext packet: " + e.getMessage());
                disconnect();
            }
        }
    }

    private class PacketReader {
        boolean readEncrypted() {
            try {
                int length = in.readInt();
                byte[] cipherText = new byte[length];
                in.readFully(cipherText);

                byte[] plainText = encryptionManager.decrypt(cipherText);
                if (plainText == null) return false;

                ByteArrayInputStream bais = new ByteArrayInputStream(plainText);
                DataInputStream dis = new DataInputStream(bais);

                String id = dis.readUTF();
                Packet packet = PacketFactory.create(id);

                if (packet == null) {
                    log("Packet with unknown id received: " + id);
                    return false;
                }

                packet.read(dis);
                packetDispatcher.handle(packet);

            } catch (SocketException _) { // socket closed, ignore
            } catch (IOException e) {
                log("Error when reading encrypted packet: " + e.getMessage());
                return false;
            }

            return true;
        }

        boolean readPlain() {
            try {
                String id;
                try {
                    id = in.readUTF();
                } catch (IOException e) {
                    log("Invalid UTF or no data; disconnecting...");
                    return false;
                }

                if (firstPacket) {
                    if (!"DHInitialExchange".equals(id) && !"DHKeyExchange".equals(id)) {
                        log("First packet received was unexpected: " + id);
                        disconnect();
                        return false;
                    }

                    firstPacket = false;
                }

                Packet packet = PacketFactory.create(id);
                if (packet == null) {
                    log("Packet with unknown id received: " + id);
                    return false;
                }

                packet.read(in);
                packetDispatcher.handle(packet);

            } catch (SocketException _) { // socket closed, ignore
            } catch (IOException e) {
                log("Error when reading plaintext packet: " + e.getMessage());
                return false;
            }

            return true;
        }
    }
}
