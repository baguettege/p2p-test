package main;

import gui.Window;
import network.Peer;
import util.FileUtil;
import util.MainUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    private static Window window;

    public static void main(String[] args) {
        window = new Window();
        window.createMainConsole();
        FileUtil.initFiles();
    }

    public static void logMain(String logText) {
        window.logMain(logText);
    }

    private static boolean isWaiting = false;
    private static ServerSocket serverSocket;
    private static Thread serverThread;

    // opens a server socket which waits for connections from other peers
    // once a connection is made, new Connection obj is created
    public static void acceptInbound(int port) {
        if (!MainUtil.isPort(port)) return;

        if (isWaiting) return;
        isWaiting = true;

        serverThread = new Thread(() -> {
            serverSocket = null;

            try {
                serverSocket = new ServerSocket(port);
                logMain("Waiting for inbound connections on port " + port + "...");

                while (true) {
                    Socket newSocket = serverSocket.accept();
                    newSocket.setKeepAlive(true);

                    String ip = newSocket.getInetAddress().getHostAddress();
                    logMain("Connection received: " + ip);

                    Peer peer = new Peer(newSocket);

                    new Thread(peer).start();
                }

            } catch (IOException e) {
                //window.logMainConsole("Error while waiting for connections: " + e.getMessage());
            } finally {
                try {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                    }
                } catch (IOException _) {}
            }
        });

        serverThread.start();
    }

    // close port
    public static void endInbound() {
        isWaiting = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException _) {}

        if (serverThread != null) {
            serverThread.interrupt();
        }

        logMain("Stopped waiting for inbound connections.");
    }

    // attempts to connect to another peer which has an open port
    // once connected, sends an Auth packet for the other peer to accept
    // waits for Auth packet from peer for 2 way auth
    public static void connect(String ipPort) {
        if (!MainUtil.isIpPort(ipPort)) {
            logMain("Invalid IP; failed to connect");
            return;
        }
        logMain("Attempting to connect to " + ipPort);

        String[] split = ipPort.split(":");
        String ip = split[0];
        int port = Integer.parseInt(split[1]);

        new Thread(() -> {
            try {
                Socket newSocket = new Socket(ip, port);
                newSocket.setKeepAlive(true);
                Peer peer = new Peer(newSocket, port);
                new Thread(peer).start();

                logMain("Connection to " + ipPort + " was successful!");

            } catch (IOException e) {
                String msg = e.getMessage();
                logMain(msg);
            }

        }).start();
    }

    public static Window window() { return window; }
}
