package gui;

import main.InputHandler;
import network.Connection;
import util.MainUtil;

import javax.swing.*;

public class Console {
    private final Window window;
    private final JTextArea console;
    private boolean isMainConsole = false;
    private final InputHandler inputHandler;
    private final String name;

    // a tab in the main window which holds a console, inputs, for main console OR for connected peers
    // do NOT create via the constructor, use Window.createConsole();

    public Console(Window window, JTextArea console, JTextField input, String name) {
        this.window = window;
        this.console = console;
        this.name = name;

        input.addActionListener(e -> handleInput(input));

        log("Console startup - " + name + " | cmd for command list");

        if (name.equals("Main")) isMainConsole = true;

        inputHandler = new InputHandler(this);
    }

    public void setConnection(Connection conn) { inputHandler.setConnection(conn); }

    public void log(String logText) {
        console.append(MainUtil.getLocalTime() + " | " + logText + "\n");
        MainUtil.log(logText);
    }

    // from text field
    private void handleInput(JTextField input) {
        String text = input.getText();
        if (text.isBlank()) return;
        log("> " + text);
        input.setText("");

        if (isMainConsole) {
            inputHandler.handleMainInput(text);
        } else {
            inputHandler.handleConnectionInput(text);
        }
    }

    public void close() {
        window.removeConsole(name);
    }
}
