package gui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;

public class Window {
    private static JTabbedPane tabs;
    private static Console mainConsole;

    // simple gui that holds each console for every peer/main console

    public Window() {
        JFrame frame = new JFrame("p2p-test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 600);

        tabs = new JTabbedPane();

        mainConsole = createConsole("Main");

        frame.add(tabs);
        frame.setVisible(true);
    }

    public void logMainConsole(String logText) {
        mainConsole.log(logText);
    }

    public Console createConsole(String name) {
        JPanel panel = new JPanel(new BorderLayout());

        JTextArea console = new JTextArea();
        console.setEditable(false);
        console.setFont(new Font("Monospaced", Font.PLAIN, 14));

        JScrollPane scroll = new JScrollPane(console);

        JTextField input = new JTextField();

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(input, BorderLayout.SOUTH);

        tabs.add(name, panel);

        return new Console(this, console, input, name);
    }

    public void removeConsole(String name) {
        int index = tabs.indexOfTab(name);
        if (index != -1) {
            tabs.remove(index);
        }
    }

    public Path chooseFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            if (selectedFile != null) {
                return selectedFile.toPath();
            }
        }

        return null;
    }
}
