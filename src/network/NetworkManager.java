package network;

import main.Main;
import util.MainUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class NetworkManager {
    private static void logMain(String logText) { Main.logMain("NETWORK - " + logText); }

    public static void connect(String ipPort) {
        if (!MainUtil.isIpPort(ipPort)) {
            logMain("Invalid IP:Port, unable to attempt connection");
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
                new Thread(new Peer(newSocket, port)).start();

                logMain("Connected to " + ipPort);

            } catch (IOException e) {
                String msg = e.getMessage();
                logMain(msg);
            }

        }).start();
    }

    private static boolean acceptingInboundConnections = false;
    private static ServerSocket serverSocket;
    private static Thread serverThread;

    // opens a server socket which waits for connections from other peers
    // once a connection is made, new Connection obj is created
    public static void acceptInbound(int port) {
        if (!MainUtil.isPort(port)) return;

        if (acceptingInboundConnections) return;
        acceptingInboundConnections = true;

        serverThread = new Thread(() -> {
            serverSocket = null;

            try {
                serverSocket = new ServerSocket(port);
                logMain("Waiting for inbound connections on port " + port);

                while (true) {
                    Socket newSocket = serverSocket.accept();
                    newSocket.setKeepAlive(true);

                    String ip = newSocket.getInetAddress().getHostAddress();
                    logMain("Connection received from " + ip);

                    new Thread(new Peer(newSocket)).start();
                }

            } catch (IOException _) {
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
        acceptingInboundConnections = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException _) {}

        if (serverThread != null) {
            serverThread.interrupt();
        }

        logMain("Stopped waiting for inbound connections");
    }
}
