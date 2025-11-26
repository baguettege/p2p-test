package processors;

import network.Peer;
import network.packets.*;
import util.MainUtil;

import java.util.Arrays;
import java.util.List;

public class PeerInputProcessor implements InputProcessor {
    private final Peer peer;
    private String currentCmd;
    private List<String> limitedCommands = List.of("auth", "exit");

    // handles all commands inputted into console

    public PeerInputProcessor(Peer peer) { this.peer = peer; }

    private void log(String logText) {
        peer.log(logText);
    }

    private void invalidCommand() {
        log("Invalid command: " + currentCmd);
    }

    public void processInput(String input) {
        String[] args = input.split(" ");
        if (args.length == 0 ) return;

        currentCmd = input;

        String arg0 = args[0];
        if (!peer.connectionVerifier().isPeerAuthorized()) {
            if (peer.connectionVerifier().isAuthorized() && arg0.equals("auth")) { // when this is outbound connection
                log("Peer must authorize the connection on their side");
                return;
            }

            if (!limitedCommands.contains(arg0)) {
                log("Command is unavailable when connection is unauthorized");
                return; // dont allow other commands when peer hasnt authorized the connection
            }
        }

        switch (arg0) {
            case "ping" -> ping();
            case "cmd" -> cmd();
            case "msg" -> message(args);
            case "auth" -> auth(args);
            case "exit" -> peer.close();
            case "file" -> file(args);
            default -> invalidCommand();
        }

        currentCmd = null;
    }

    private void cmd() {
        log(MainUtil.cmdIndent("""
            CMDs:
            cmd - command list
            msg [text] - send message to peer
            auth [accept/decline] - authenticate inbound connection
            ping - ping peer
            file [accept/decline/upload]
                 [cancel upload/download] - file transfer
            exit - disconnect from peer
            """));
    }

    private void message(String[] args) {
        if (args.length < 2) {
            invalidCommand();
            return;
        }

        String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        peer.writePacket(new Message(text));
        log("MSG (You) - " + text);
    }

    private void ping() {
        log("Pinging peer...");
        peer.writePacket(new Ping());
    }

    private void auth(String[] args) {
        if (args.length != 2) {
            invalidCommand();
            return;
        }

        switch (args[1]) {
            case "accept" -> peer.connectionVerifier().authorize();
            case "decline" -> peer.connectionVerifier().denyAuthorization();
            default -> invalidCommand();
        }
    }

    private void file(String[] args) {
        if (args.length < 2) {
            invalidCommand();
            return;
        }

        String arg1 = args[1];

        if (args.length == 2) {
            switch (arg1) {
                case "upload" -> peer.fileProcessor().selectFile();
                case "accept" -> peer.fileProcessor().acceptRequest();
                case "decline" -> peer.fileProcessor().declineRequest();
                default -> invalidCommand();
            }
        } else if (args.length == 3) {
            if (arg1.equals("cancel")) {
                switch (args[2]) {
                    case "upload" -> peer.fileProcessor().cancelUpload(false);
                    case "download" -> peer.fileProcessor().cancelDownload(false);
                    default -> invalidCommand();
                }
            } else {
                invalidCommand();
            }
        } else {
            invalidCommand();
        }
    }
}
