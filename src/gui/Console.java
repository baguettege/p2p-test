package gui;

import processors.InputProcessor;
import processors.MainInputProcessor;
import processors.PeerInputProcessor;
import network.Peer;
import util.MainUtil;

import javax.swing.*;

public class Console {
    private final Window window;
    private final JTextArea console;
    private final InputProcessor inputProcessor;
    private final String name;

    // a tab in the main window which holds a console, inputs, for main console OR for connected peers
    // do NOT create via the constructor, use Window.createConsole();

    public Console(Window window, JTextArea console, JTextField input, String name, Peer peer) {
        this.window = window;
        this.console = console;
        this.name = name;

        log("Console " + name + " | cmd for command list");

        if (peer != null) {
            inputProcessor = new PeerInputProcessor(peer);
            log("Waiting for connection to be authorized...");
        } else {
            inputProcessor = new MainInputProcessor();
        }

        input.addActionListener(e -> {
            String text = input.getText();
            log("> " + text);
            input.setText("");
            inputProcessor.processInput(text);
        });
    }

    public void log(String logText) {
        console.append(MainUtil.getLocalTime() + " | " + logText + "\n");
        MainUtil.log(logText);
    }

    public void close() {
        window.removeConsole(this);
    }
}
