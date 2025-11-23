package network;

import gui.Console;
import main.Main;
import main.PacketHandler;
import network.packets.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Connection implements Runnable {
    private final Socket socket;
    private Console console;

    private final String ip;
    private boolean verified;
    private boolean authenticated;
    private boolean isDisconnecting;

    private DataOutputStream out;
    private DataInputStream in;

    private final int AUTH_TIMEOUT_MILLIS = 30000;
    private final int VERIFY_TIMEOUT_MILLIS = 5000;

    public Connection(Socket socket) {
        this.socket = socket;
        ip = socket.getInetAddress().getHostAddress();
        initStreams();
    }

    public Connection(Socket socket, int port) {
        this.socket = socket;
        ip = socket.getInetAddress().getHostAddress() + ":" + port;
        initStreams();

        authenticated = true; // auto authenticate as this connection was an outbound request
    }

    private void initStreams() {
        try {
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            Main.logMainConsole("Error when creating output/input streams: " + e.getMessage());
        }

        writePacket(new Verify());
    }

    // listen to incoming packets from peer
    // wait 5s for an Auth packet to be sent by the peer
    // otherwise terminate the connection
    @Override
    public void run() {
        PacketHandler packetHandler = new PacketHandler(this);

        new Thread(() -> {
            try {
                Thread.sleep(VERIFY_TIMEOUT_MILLIS);
            } catch (InterruptedException e) {
                if (console != null) console.log("Thread interrupted whilst waiting for connection verification: " + e.getMessage());
            }

            if (!verified) {
                silentClose();
            }
        }).start();

        try {
            while (true) {
                String id = in.readUTF();
                Packet packet = PacketFactory.create(id);

                if (packet == null) {
                    logConsole("Packet received was null: " + id);
                    Main.logMainConsole("Packet received was null: " + id);
                    continue;
                }

                packet.read(in);
                packetHandler.handle(packet);
            }
        } catch (IOException _) {}
        finally {
            close("Peer disconnected");
        }
    }

    // when the peer sends an Auth packet
    // create console
    public synchronized void verify() {
        if (verified) return;
        verified = true;
        console = Main.window().createConsole(ip);
        console.setConnection(this);
        logConsole("Peer verification successful!");

        // wait for self to authenticate the connection manually
        if (authenticated) {
            console.log("Waiting for peer to authenticate this connection...");
            return; // exit early if this is outbound connection
        }

        new Thread(() -> {
            try {
                logConsole("cmd 'auth' to authenticate this connection within 30s...");
                Thread.sleep(AUTH_TIMEOUT_MILLIS);
                if (!authenticated) {
                    logConsole("Authentication timed out; disconnecting...");
                    writePacket(new Auth("Failed; timed out"));
                    close("Authentication time out");
                }
            } catch (InterruptedException e) {
                logConsole("Error when waiting for authentication: " + e.getMessage());
            }
        }).start();
    }

    public synchronized void authenticate() {
        if (authenticated) return;
        logConsole("Successfully authenticated connection!");
        authenticated = true;
        writePacket(new Auth("Success!"));
    }

    public boolean isAuthenticated() { return authenticated;}
    public boolean isVerified() { return verified; }

    public void logConsole(String logText) {
        if (console != null) console.log(logText);
    }

    // send data to the peer
    public synchronized void writePacket(Packet packet) {
        if (isDisconnecting) return;

        try {
            out.writeUTF(packet.getId());
            packet.write(out);
            out.flush();
        } catch (IOException e) {
            logConsole("Error when writing packet: " + e.getMessage());
        }
    }

    // disconnect the peer without logging
    // used to avoid console spam from port scanners
    private synchronized void silentClose() {
        if (isDisconnecting) return;
        isDisconnecting = true;

        try {
            if (!socket.isClosed()) {
                socket.close();
            }

        } catch (IOException e) {
            logConsole("Error when closing connection: " + e.getMessage());
            Main.logMainConsole("Error when closing connection: " + e.getMessage());
        }

        if (console != null) console.close();
    }

    // disconnect the peer with logging
    public synchronized void close(String reason) {
        if (isDisconnecting) return;
        isDisconnecting = true;

        logConsole("Attempting to disconnect: " + reason);
        Main.logMainConsole("Attempting to disconnect " + ip + " - " + reason);

        writePacket(new Disconnect(reason));

        try {
            if (!socket.isClosed()) {
                socket.close();
            }

        } catch (IOException e) {
            logConsole("Error when closing connection: " + e.getMessage());
            Main.logMainConsole("Error when closing connection: " + e.getMessage());
        }

        Main.logMainConsole("Disconnected " + ip);

        new Thread(() -> {
            try {
                logConsole("Closing console in 5s...");
                Thread.sleep(5000);
                if (console != null) console.close();
            } catch (InterruptedException e) {
                logConsole("Error when sleeping thread before closing console: " + e.getMessage());
            }
        }).start();
    }
}
