package main;

import gui.Console;
import network.Connection;
import network.packets.*;
import util.FileUtil;
import util.MainUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class InputHandler {
    private final Console console;
    private Connection connection;

    // handles all commands inputted into console
    // split into main console and connection console which take different commands

    public InputHandler(Console console) {
        this.console = console;
    }

    public void setConnection(Connection conn) { connection = conn; }

    private void onInvalidCommand() {
        console.log("Invalid arguments for command");
    }

    private void onUnknownCommand(String input) {
        console.log("Unknown or invalid command: " + input);
    }

    // main commands
    // --------------------------------------------------------------------------------------

    public void handleMainInput(String input) {
        String[] args = input.split(" ");
        if (args.length == 0 ) return;

        switch (args[0]) {
            case "cmd" -> mainCmd();
            case "connect" -> mainConnect(args);
            case "openport" -> mainPort(args);
            case "closeport" -> mainClosePort();
            case "exit" -> mainExit();
            default -> onUnknownCommand(input);
        }
    }

    private void mainCmd() {
        console.log("CMDs:");
        console.log("cmd - command list");
        console.log("connect [ip:port] - connect to a peer");
        console.log("openport [port] - listen for inbound connections");
        console.log("closeport - end listening for inbound connections");
        console.log("exit - end the program");
    }

    private void mainConnect(String[] args) {
        if (args.length != 2) {
            onInvalidCommand();
            return;
        }

        String ipPort = args[1];
        if (!MainUtil.isIpPort(ipPort)) {
            console.log("Invalid IP; cannot connect");
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

        Main.acceptInbound(port);
    }

    private void mainClosePort() {
        Main.endInbound();
    }

    private void mainExit() {
        System.exit(0);
    }

    // connection commands
    // --------------------------------------------------------------------------------------

    public void handleConnectionInput(String input) {
        String[] args = input.split(" ");
        if (args.length == 0 ) return;

        switch (args[0]) {
            case "ping" -> connPing(args);
            case "cmd" -> connCmd(args);
            case "msg" -> connMessage(args);
            case "auth" -> connAuth(args);
            case "exit" -> connExit(args);
            case "send" -> connSend(args);
            case "accept" -> connAccept();
            case "decline" -> connDecline();
            default -> onUnknownCommand(input);
        }
    }

    private void connCmd(String[] args) {
        if (args.length != 1) {
            onInvalidCommand();
            return;
        }

        console.log("CMDs:");
        console.log("cmd - command list");
        console.log("msg [text] - send message to peer");
        console.log("auth - authenticate inbound connection");
        console.log("ping - ping peer");
        console.log("send - send file to peer");
        console.log("accept - allow peer to send file");
        console.log("decline - decline peer sending file");
        console.log("exit - end connection with peer");
    }

    private void connMessage(String[] args) {
        if (args.length < 2) {
            onInvalidCommand();
            return;
        }

        String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        connection.writePacket(new Message(text));
        console.log("MSG (You) - " + text);
    }

    private void connAuth(String[] args) {
        if (args.length != 1) {
            onInvalidCommand();
            return;
        }

        connection.authenticate();
    }

    private void connExit(String[] args) {
        if (args.length != 1) {
            onInvalidCommand();
            return;
        }

        connection.close("Connection forcibly closed");
    }

    private void connPing(String[] args) {
        if (args.length != 1) {
            onInvalidCommand();
            return;
        }

        connection.logConsole("Pinging peer...");
        connection.writePacket(new Ping());
    }

    private void connSend(String[] args) {
        Path selectedFile = Main.window().chooseFile();
        if (selectedFile == null) {
            connection.logConsole("File send cancelled");
            return;
        }

        try {
            connection.logConsole("Sending file transfer request to peer: " + selectedFile.getFileName() + " | " + FileUtil.getFileSize(Files.size(selectedFile)));
            connection.writePacket(new DataRequest(selectedFile));
            connection.setWritingFile(selectedFile);
        } catch (IOException e) {
            connection.logConsole("Error when sending DataRequest packet: " + e.getMessage());
        }
    }

    private void connAccept() {
        if (connection.hasReceivedDataRequest()) {
            connection.logConsole("Accepted file transfer from peer");
            connection.allowFileReceive();
            connection.writePacket(new DataResponse(true));
            connection.stopHasReceivedDataRequest();
        }
    }

    private void connDecline() {
        if (connection.hasReceivedDataRequest()) {
            connection.logConsole("Declined file transfer from peer");
            connection.writePacket(new DataResponse(false));
            connection.stopHasReceivedDataRequest();
        }
    }
}
