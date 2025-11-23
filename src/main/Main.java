package main;

import gui.Console;
import gui.Window;
import network.Connection;
import util.MainUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    private static Window window;

    static void main(String[] args) {
        MainUtil.log("Startup");
        window = new Window();
    }

    private static boolean isWaiting = false;
    public static void waitForConnections(int port) {
        if (!MainUtil.isPort(port)) return;

        if (isWaiting) return;
        isWaiting = true;

        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                window.logMainConsole("Waiting for connections on port " + port + "...");

                while (true) {
                    Socket newSocket = serverSocket.accept();
                    newSocket.setKeepAlive(true);

                    String ip = newSocket.getInetAddress().getHostAddress();
                    window.logMainConsole("Connection received: " + ip);

                    Console console = window.createConsole(ip);
                    Connection connection = new Connection(newSocket, console);
                    console.setConnection(connection);

                    new Thread(connection).start();
                }

            } catch (IOException e) {
                window.logMainConsole("Error while waiting for connections: " + e.getMessage());
            }
        }).start();
    }

    public static void connect(String ipPort) {
        if (!MainUtil.isIpPort(ipPort)) {
            window.logMainConsole("Invalid IP; failed to connect");
            return;
        }
        window.logMainConsole("Attempting to connect to " + ipPort);

        Console console = window.createConsole(ipPort);
        console.log("Attempting connection...");

        String[] split = ipPort.split(":");

        new Thread(() -> {
            try {
                Socket newSocket = new Socket(split[0], Integer.parseInt(split[1]));
                newSocket.setKeepAlive(true);
                Connection connection = new Connection(newSocket, console);
                console.setConnection(connection);
                new Thread(connection).start();

            } catch (IOException e) {
                String msg = e.getMessage();
                window.logMainConsole(msg);
                console.log(msg);
            }

            window.logMainConsole("Connection to " + ipPort + " was successful!");
            console.log("Connection success!");

        }).start();
    }
}
