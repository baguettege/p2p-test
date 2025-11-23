package main;

import gui.Console;
import network.Connection;
import network.packets.*;
import util.MainUtil;

public class InputHandler {
    private final Console console;
    private Connection connection;

    public InputHandler(Console console) {
        this.console = console;
    }

    public void setConnection(Connection conn) { connection = conn; }

    private void onInvalidCommand() {
        console.log("Invalid arguments; cmd fail");
    }

    private void onUnknownCommand(String input) {
        console.log("Unknown or invalid command: " + input);
    }

    // --------------------------------------------------------------------------------------

    public void handleMainInput(String input) {
        String[] args = input.split(" ");
        if (args.length == 0 ) return;

        switch (args[0]) {
            case "cmd" -> mainCmd();
            case "connect" -> mainConnect(args);
            case "setport" -> mainPort(args);
            default -> onUnknownCommand(input);
        }
    }

    private void mainCmd() {
        console.log("CMDs - cmd, connect, setport");
    }

    private void mainConnect(String[] args) {
        if (args.length != 2) {
            onInvalidCommand();
            return;
        }

        String ipPort = args[1];
        if (!MainUtil.isIpPort(ipPort)) {
            console.log("Invalid IP; failed to connect");
            return;
        }

        Main.connect(ipPort);
    }

    private void mainPort(String[] args) {
        if (args.length != 2) {
            onInvalidCommand();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[1]);
            if (!MainUtil.isPort(port)) {
                onInvalidCommand();
                return;
            }
        } catch (NumberFormatException e) {
            console.log("Invalid port entered: " + e.getMessage());
            return;
        }

        Main.waitForConnections(port);
    }

    // --------------------------------------------------------------------------------------

    public void handleConnectionInput(String input) {
        String[] args = input.split(" ");
        if (args.length == 0 ) return;

        switch (args[0]) {
            case "cmd" -> connCmd();
            case "msg" -> connMessage(args);
            default -> onUnknownCommand(input);
        }
    }

    private void connCmd() {
        console.log("CMDs - cmd, msg");
    }

    private void connMessage(String[] args) {
        if (args.length != 2) {
            onInvalidCommand();
            return;
        }

        String text = args[1];

        if (connection != null) {
            connection.writePacket(new Message(text));
        } else {
            console.log("Connection was null? This error should not be possible.");
        }

        console.log("MSG (You) - " + text);
    }
}
