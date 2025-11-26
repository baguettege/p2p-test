package processors;

import main.Main;
import util.MainUtil;

public class MainInputProcessor implements InputProcessor {
    private String currentCmd;

    private void log(String logText) {
        Main.logMain(logText);
    }

    private void invalidCommand() {
        log("Invalid command: " + currentCmd);
    }

    public void processInput(String input) {
        String[] args = input.split(" ");
        if (args.length == 0 ) return;

        currentCmd = input;

        switch (args[0]) {
            case "cmd" -> cmd();
            case "connect" -> connect(args);
            case "port" -> port(args);
            case "exit" -> System.exit(0);
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

        Main.connect(ipPort);
    }

    private void port(String[] args) {
        if (args.length < 2) {
            invalidCommand();
            return;
        }

        String decision = args[1];
        if (decision.equals("close")) {
            Main.endInbound();

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

            Main.acceptInbound(port);
        }
    }
}
