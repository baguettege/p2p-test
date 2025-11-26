package network;

import gui.Console;
import processors.FileProcessor;
import main.Main;
import processors.PacketProcessor;
import network.packets.*;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class Peer implements Runnable {
    private final Socket socket;
    private final Console console;

    private PacketProcessor packetProcessor;
    private FileProcessor fileProcessor;

    public final String ip;
    private boolean isDisconnecting;

    private DataOutputStream out;
    private DataInputStream in;

    public void log(String logText) {
        if (console != null) console.log(logText);
    }

    public Peer(Socket socket) {
        this.socket = socket;
        ip = socket.getInetAddress().getHostAddress();
        initConnection();

        console = Main.window().createConsole(ip, this);
    }

    public Peer(Socket socket, int port) {
        this.socket = socket;
        ip = socket.getInetAddress().getHostAddress() + ":" + port;
        initConnection();

        console = Main.window().createConsole(ip, this);
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
    }

    // listen to incoming packets from peer
    // wait 5s for an Accept packet to be sent by the peer
    // otherwise terminate the connection
    @Override
    public void run() {
        try {
            while (true) {
                String id = in.readUTF();
                Packet packet = PacketFactory.create(id);

                if (packet == null) {
                    log("Packet received was null: " + id);
                    Main.logMain("Packet received was null: " + id);
                    continue;
                }

                packet.read(in);
                packetProcessor.handle(packet);
            }
        } catch (IOException _) {}
        finally {
            close();
        }
    }

    public FileProcessor fileProcessor() { return fileProcessor; }

    // send data to the peer
    public synchronized void writePacket(Packet packet) {
        if (isDisconnecting) return;

        try {
            out.writeUTF(packet.getId());
            packet.write(out);
            out.flush();
        } catch (IOException e) {
            log("Error when writing packet: " + e.getMessage());
        }
    }

    // disconnect the peer without logging
    // used to avoid console spam from port scanners
    public synchronized void silentClose() {
        if (isDisconnecting) return;
        isDisconnecting = true;

        //log("Silently closing peer"); //debug

        try {
            if (!socket.isClosed()) {
                socket.close();
            }

        } catch (IOException e) {
            log("Error when closing connection: " + e.getMessage());
            Main.logMain("Error when closing connection: " + e.getMessage());
        }

        if (console != null) console.close();
    }

    // disconnect the peer with logging
    public synchronized void close() {
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
