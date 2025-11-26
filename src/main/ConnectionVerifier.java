package main;

import gui.Console;
import network.Peer;
import network.packets.Auth;
import network.packets.KeepAlive;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicReference;

public class ConnectionVerifier {
    private Peer peer;

    enum ConnectionState {
        CONNECTED, // socket made
        ACCEPTED, // peer ensured to be a real person
        AUTHORIZED // peer accepted in by other peer
    }

    private final int AUTH_TIMEOUT_MILLIS = 30000;
    private final int KEEP_ALIVE_COOLDOWN = 10000;

    public ConnectionVerifier(Peer peer) {
        this.peer = peer;
    }

    private void log(String logText) {
        peer.log(logText);
    }

    private volatile ConnectionState connState = ConnectionState.CONNECTED;

    public void accept() {
        if (autoAuthorized) return;
        if (isAccepted()) return;
        if (isAuthorized() && !autoAuthorized) {
            log("WARN - ConnectionVerifier.accept(); called when connState was ConnectionState.AUTHORIZED");
            return;
        }

        connState = ConnectionState.ACCEPTED;
        log("Connection to peer was accepted");

        try {
            AtomicReference<Console> ref = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> ref.set(Main.window().createConsole(peer.ip(), peer))); // run on EDT and wont be null
            peer.setConsole(ref.get());
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            try {
                log("cmd 'auth [accept/decline]' for this connection within 30s...");
                Thread.sleep(AUTH_TIMEOUT_MILLIS);
                if (!isAuthorized()) {
                    log("Authentication timed out; disconnecting...");
                    peer.writePacket(new Auth("Authorization timed out"));
                    peer.close();
                }
            } catch (InterruptedException e) {
                log("Error when waiting for authentication: " + e.getMessage());
            }
        }).start();
    }

    private boolean peerAuthorized = false;

    public void authorize() {
        peerAuthorized = true;

        if (autoAuthorized) return;

        if (connState == ConnectionState.AUTHORIZED) return;
        if (connState != ConnectionState.ACCEPTED) {
            log("WARN - Unable to authorize an unaccepted peer");
            return;
        }

        connState = ConnectionState.AUTHORIZED;
        peer.writePacket(new Auth("Connection authorized"));

        log("Connection to peer was authorized");

        // write KeepAlive packets
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(KEEP_ALIVE_COOLDOWN);
                    peer.writePacket(new KeepAlive());
                } catch (InterruptedException e) {
                    log("Error when writing keep alive packets: " + e.getMessage());
                }
            }
        }).start();
    }

    public void denyAuthorization() {
        peer.writePacket(new Auth("Authorization denied"));
        peer.close();
    }

    private boolean autoAuthorized = false;
    
    public void autoAuth() {
        autoAuthorized = true;
        peer.writePacket(new Auth(""));
        connState = ConnectionState.AUTHORIZED;

        try {
            AtomicReference<Console> ref = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> ref.set(Main.window().createConsole(peer.ip(), peer))); // run on EDT and wont be null
            peer.setConsole(ref.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isAccepted() { return connState != ConnectionState.CONNECTED; }
    public boolean isAuthorized() { return connState == ConnectionState.AUTHORIZED; }
    public boolean isPeerAuthorized() { return peerAuthorized; }
}
