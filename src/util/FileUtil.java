package util;

import main.Main;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileUtil {
    private static final Path mainDir = Path.of(System.getProperty("user.home"), "Documents", "p2p-test");
    private static Path logFile;
    private static Path downloadsDir;

    private static boolean isFilesInitialized = false;

    public static void initFiles() {
        try {
            Files.createDirectories(mainDir);

            Path logDir = mainDir.resolve("logs");
            Files.createDirectories(logDir);

            downloadsDir = mainDir.resolve("downloads");
            Files.createDirectories(downloadsDir);

            logFile = logDir.resolve(MainUtil.getLocalDateTime() + ".log");
            Files.createFile(logFile);

            isFilesInitialized = true;
            Main.logMainConsole("Files successfully initialized: " + mainDir);
            Main.logMainConsole("Session log file: " + logFile);
            Main.logMainConsole("Session downloads file: " + downloadsDir);

        } catch (IOException e) {
            Main.logMainConsole("Error when initializing files: " + e.getMessage());
        }
    }

    public static void writeLog(String logText) {
        if (!isFilesInitialized) {
            System.out.println("Attempted to write to log file but files were not initialized!");
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(logText + "\n");

        } catch (IOException e) {
            Main.logMainConsole("Error when writing to log: " + e.getMessage());
        }
    }

    public static Path getDownloadsDir() { return downloadsDir; }

    public static String getFileSize(long size) {
        double value = size;
        String unit = "B";

        if (size < 1024) {
            // ignore
        } else if (size < 1024 * 1024) {
            value = value / 1024;
            unit = "KiB";
        } else if (size < 1024L * 1024 * 1024) {
            value = value / (1024 * 1024);
            unit = "MiB";
        } else {
            value = value / (1024L * 1024 * 1024);
            unit = "GiB";
        }

        return String.format("%.3g %s", value, unit); // 3sf
    }

    public static String getFileNameWithTime(String fileName) {
        String timestamp = MainUtil.getLocalDateTime();

        int dot = fileName.lastIndexOf(".");
        if (dot == -1) {
            return fileName + " " + timestamp;
        }

        String name = fileName.substring(0, dot);
        String extension = fileName.substring(dot);
        return name + " " + timestamp + extension;
    }
}
