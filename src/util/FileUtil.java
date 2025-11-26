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

            downloadsDir = mainDir.resolve("downloads");
            Files.createDirectories(downloadsDir);

            logFile = mainDir.resolve("latest.log");
            if (Files.exists(logFile)) Files.delete(logFile); // prevent spam log creation
            Files.createFile(logFile);

            isFilesInitialized = true;
            Main.logMain("Files successfully initialized: " + mainDir);

        } catch (IOException e) {
            Main.logMain("Error when initializing files: " + e.getMessage());
        }
    }

    public static void writeLog(String logText) {
        if (!isFilesInitialized) {
            System.out.println("Attempted to write to log file but files were not initialized: " + logText);
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(logText + "\n");

        } catch (IOException e) {
            Main.logMain("Error when writing to log: " + e.getMessage());
        }
    }

    public static Path getDownloadsDir() { return downloadsDir; }

    public static String getFileSize(long size) {
        double value = size;
        String[] units = {"B", "KiB", "MiB", "GiB", "TiB", "PiB"};

        int unitIndex = 0;

        // move to next until whilst >= 1024
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }

        return String.format("%.3g %s", value, units[unitIndex]); // 3sf
    }

    public static String getFileSize(double size) {
        double value = size;
        String[] units = {"B", "KiB", "MiB", "GiB", "TiB", "PiB"};
        int unitIndex = 0;

        // move to next until whilst >= 1024
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }

        return String.format("%.3g %s", value, units[unitIndex]);
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
