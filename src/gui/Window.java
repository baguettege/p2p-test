package gui;

import network.Peer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Window {
    private static JTabbedPane tabs;
    private static Console mainConsole;
    private static final Map<Console, Component> activeConsoles = new HashMap<>(); // <Console, tabIndex> -> used for removing consoles later

    private static final Color bg = new Color(0x1E1E1E);   // background
    private static final Color fg = new Color(0xCFCFCF);   // text

    private static final Color inputBg = new Color(0x252525);
    private static final Color inputFg = new Color(0xCFCFCF);

    // simple gui that holds each console for every peer/main console

    public Window() {
        JFrame frame = new JFrame("p2p-test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 600);
        frame.getContentPane().setBackground(Color.DARK_GRAY);

        tabs = new JTabbedPane();
        tabs.setBackground(Color.DARK_GRAY);

        // create main console
        try { // run on EDT
            SwingUtilities.invokeAndWait(() -> mainConsole = createConsole("Main"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        frame.add(tabs);
        frame.setVisible(true);
    }

    public void logMain(String logText) {
        mainConsole.log(logText);
    }

    private Console buildConsole(String name, Peer peer) {
        if (peer == null && mainConsole != null) return null;

        JPanel panel = new JPanel(new BorderLayout());

        JTextArea console = new JTextArea();
        console.setEditable(false);
        console.setFont(new Font("Monospaced", Font.PLAIN, 14));
        console.setForeground(fg);
        console.setBackground(bg);
        console.setBorder(BorderFactory.createLineBorder(new Color(0x3A3A3A)));

        JScrollPane scroll = new JScrollPane(console);

        JTextField input = new JTextField();
        input.setForeground(inputFg);
        input.setBackground(inputBg);
        input.setBorder(BorderFactory.createLineBorder(new Color(0x3A3A3A)));

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(input, BorderLayout.SOUTH);

        tabs.add(name, panel);

        Console newConsole;

        if (peer != null) {
            newConsole = new Console(this, console, input, name, peer);
        } else {
            newConsole = new Console(this, console, input, name, null);
            mainConsole = newConsole;
        }

        activeConsoles.put(newConsole, panel);

        return newConsole;
    }

    // MUST be called with SwingUtilities.invokeAndWait(() -> window.createConsole(peerName));
    public Console createConsole(String name) { return buildConsole(name, null); }
    public Console createConsole(String name, Peer peer) { return buildConsole(name, peer); }

    public void removeConsole(Console console) {
        Runnable removeTask = () -> {
            tabs.remove(activeConsoles.get(console));
            activeConsoles.remove(console);
        };

        if (SwingUtilities.isEventDispatchThread()) {
            removeTask.run();
        } else {
            SwingUtilities.invokeLater(removeTask);
        }
    }

    public Path chooseFile(Path pth) {
        JFileChooser chooser;
        if (pth == null) {
            chooser = new JFileChooser();
        } else {
            chooser = new JFileChooser(pth.toFile());
        }

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
