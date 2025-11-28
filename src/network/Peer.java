package network;

import gui.Console;
import main.EncryptionHandler;
import processors.FileProcessor;
import main.Main;
import processors.PacketProcessor;
import network.packets.*;

import java.io.*;
import java.net.Socket;

public class Peer implements Runnable {
    private final Socket socket;
    private Console console;

    private PacketProcessor packetProcessor;
    private FileProcessor fileProcessor;
    private EncryptionHandler encryptionHandler;

    public final String ip;
    private boolean isDisconnecting;

    private DataOutputStream out;
    private DataInputStream in;

    public void log(String logText) {
        if (console != null) console.log(logText);
    }

    public Peer(Socket socket) { // inbound connection (server)
        this.socket = socket;
        ip = socket.getInetAddress().getHostAddress();
        initConnection();

        encryptionHandler.initServerSide();
    }

    public Peer(Socket socket, int port) { // outbound connection (client)
        this.socket = socket;
        ip = socket.getInetAddress().getHostAddress() + ":" + port;
        initConnection();
    }

    public EncryptionHandler encryptionHandler() {
        return encryptionHandler;
    }

    private void initConnection() {
        try {
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            Main.logMain("Error when creating output/input streams: " + e.getMessage());
        }

        packetProcessor = new PacketProcessor(this);
        fileProcessor = new FileProcessor(this);
        console = Main.window().createConsole(ip, this);
        encryptionHandler = new EncryptionHandler(this);
    }

    // listen to incoming packets from peer
    @Override
    public void run() {
        try {
            while (true) {
                if (encryptionHandler.encryptionReady()) {
                    int length = in.readInt();
                    byte[] cipherText = new byte[length];
                    in.readFully(cipherText);

                    byte[] plainText = encryptionHandler.decrypt(cipherText);

                    ByteArrayInputStream bais = new ByteArrayInputStream(plainText);
                    DataInputStream dis = new DataInputStream(bais);

                    String id = dis.readUTF();
                    Packet packet = PacketFactory.create(id);

                    if (packet == null) {
                        log("Packet with unknown id received: " + id);
                        continue;
                    }

                    packet.read(dis);
                    packetProcessor.handle(packet);

                } else {
                    log("DEBUG WARN - Reading an unencrypted packet");
                    String id = in.readUTF();
                    Packet packet = PacketFactory.create(id);

                    if (packet == null) {
                        log("Packet with unknown id received: " + id);
                        continue;
                    }

                    packet.read(in);
                    packetProcessor.handle(packet);
                }
            }

        } catch (IOException _) {
        } finally {
            disconnect();
        }
    }

    public FileProcessor fileProcessor() { return fileProcessor; }

    // send data to the peer
    public synchronized void writePacket(Packet packet) {
        if (isDisconnecting) return;

        try {
            if (encryptionHandler.encryptionReady()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                dos.writeUTF(packet.getId());
                packet.write(dos);
                dos.flush();

                byte[] plainText = baos.toByteArray();
                byte[] cipherText = encryptionHandler.encrypt(plainText);

                out.writeInt(cipherText.length);
                out.write(cipherText);
                out.flush();

            } else {
                log("DEBUG WARN - Sending an unencrypted packet");
                out.writeUTF(packet.getId());
                packet.write(out);
                out.flush();
            }

        } catch (IOException e) {
            log("Error when writing packet: " + e.getMessage());
        }
    }

    // disconnect the peer with logging
    public synchronized void disconnect() {
        if (isDisconnecting) return;
        isDisconnecting = true;

        log("Disconnecting peer...");
        Main.logMain("Disconnecting peer " + ip + "...");

        try {
            if (!socket.isClosed()) {
                socket.close();
            }

        } catch (IOException e) {
            log("Error when disconnecting peer: " + e.getMessage());
            Main.logMain("Error when disconnecting peer: " + e.getMessage());
        }

        log("Disconnected peer successfully");
        Main.logMain("Disconnected peer " + ip + " successfully");

        new Thread(() -> {
            try {
                log("Console closing in 10s...");
                Thread.sleep(10000);
                if (console != null) console.close();
            } catch (InterruptedException e) {
                log("Error when sleeping thread before closing console: " + e.getMessage());
            }
        }).start();
    }
}
