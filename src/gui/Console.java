package gui;

import communication.CommandController;
import communication.MainCommandController;
import communication.PeerCommandController;
import network.Peer;
import util.MainUtil;

import javax.swing.*;

public class Console {
    private final Window window;
    private final JTextArea console;
    private final CommandController commandController;

    // a tab in the main window which holds a console, inputs, for main console OR for connected peers
    // do NOT create via the constructor, use Window.createConsole();

    public Console(Window window, JTextArea console, JTextField input, String name, Peer peer) {
        this.window = window;
        this.console = console;

        log("CONSOLE - Console " + name + " | cmd for command list");

        if (peer != null) {
            commandController = new PeerCommandController(peer);
        } else {
            commandController = new MainCommandController();
        }

        input.addActionListener(e -> {
            String text = input.getText();
            log("> " + text);
            input.setText("");
            commandController.processInput(text);
        });
    }

    public void log(String logText) {
        console.append(MainUtil.getLocalTime() + " | " + logText + "\n");
        MainUtil.log(logText);
    }

    public void close() { window.removeConsole(this); }
}
