package main;

import gui.Window;
import util.FileUtil;

public class Main {
    private static Window window;

    public static void main(String[] args) {
        window = new Window();
        FileUtil.initFiles();
    }

    public static void logMain(String logText) { window.logMain(logText); }

    public static Window window() { return window; }
}
