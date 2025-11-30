package communication;

import main.Main;
import network.NetworkManager;
import util.FileUtil;
import util.MainUtil;

import java.nio.file.Path;
import java.util.Arrays;

public class MainCommandController implements CommandController {
    private String currentCmd;

    private void log(String logText) { Main.logMain("CMD - " + logText); }

    private void invalidCommand() { log("Invalid command: " + currentCmd); }

    public void processInput(String input) {
        String[] args = input.split(" ");
        if (args.length == 0 ) return;

        currentCmd = input;

        switch (args[0]) {
            case "cmd" -> cmd();
            case "connect" -> connect(args);
            case "port" -> port(args);
            case "exit" -> System.exit(0);
            case "debug" -> debug(args);
            case "key" -> key(args);
            default -> invalidCommand();
        }

        currentCmd = null;
    }

    private void cmd() {
        log(MainUtil.cmdIndent("""
            CMDs:
            cmd - command list
            connect [ip:port] - connect to a peer
            port [open/close] [number IF open] - open/close ports
            key [add/remove] - trusted peer's public keys
                [reset] - reset public and private keys
            exit - end the program
            """));
    }

    private void connect(String[] args) {
        if (args.length != 2) {
            invalidCommand();
            return;
        }

        String ipPort = args[1];
        if (!MainUtil.isIpPort(ipPort)) {
            invalidCommand();
            return;
        }

        NetworkManager.connect(ipPort);
    }

    private void port(String[] args) {
        if (args.length < 2) {
            invalidCommand();
            return;
        }

        String decision = args[1];
        if (decision.equals("close")) {
            NetworkManager.endInbound();

        } else if (decision.equals("open")) {
            if (args.length != 3) {
                invalidCommand();
                return;
            }

            int port;
            try {
                port = Integer.parseInt(args[2]);
                if (!MainUtil.isPort(port)) {
                    invalidCommand();
                    return;
                }
            } catch (NumberFormatException e) {
                invalidCommand();
                return;
            }

            NetworkManager.acceptInbound(port);
        } else {
            invalidCommand();
        }
    }

    private void debug(String[] args) {
        if (args[1].equals("key")) {
            String paths = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            String[] keyPaths = paths.split("\\|");
            FileUtil.debugSetKeys(Path.of(keyPaths[0]), Path.of(keyPaths[1]));
            log(MainUtil.cmdIndent("DEBUG - Set key paths to: \nPublic key: " + keyPaths[0]) + "\nPrivate key: " + keyPaths[1]);

        } else {
            invalidCommand();
        }
    }

    private void key(String[] args) {
        if (args.length < 2) {
            invalidCommand();
            return;
        }

        if (args.length == 2) {
            switch (args[1]) {
                case "add" -> EncryptionManager.addTrustedKey();
                case "remove" -> EncryptionManager.removeTrustedKey();
                case "reset" -> EncryptionManager.resetAuthKeys();
                default -> invalidCommand();
            }

        } else {
            invalidCommand();
        }
    }
}
