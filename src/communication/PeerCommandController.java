package communication;

import network.Peer;
import network.packets.*;
import util.MainUtil;

import java.util.Arrays;

public class PeerCommandController implements CommandController {
    private final Peer peer;
    private String currentCmd;

    // handles all commands inputted into console

    public PeerCommandController(Peer peer) { this.peer = peer; }

    private void log(String logText) { peer.log("PACKET - " + logText); }

    private void invalidCommand() { log("Invalid command: " + currentCmd); }

    public void processInput(String input) {
        String[] args = input.split(" ");
        if (args.length == 0 ) return;

        currentCmd = input;

        String arg0 = args[0];

        switch (arg0) {
            case "ping" -> ping();
            case "cmd" -> cmd();
            case "msg" -> message(args);
            case "disconnect" -> peer.disconnect();
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
            ping - ping peer
            file [accept/decline/upload]
                 [cancel upload/download] - file transfer
            disconnect - disconnect from peer
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
