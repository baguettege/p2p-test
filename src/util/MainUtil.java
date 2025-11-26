package util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MainUtil {
    public static void log(String logText) {
        String log = getLocalTime() + " | " + logText;
        System.out.println(log);
        FileUtil.writeLog(log);
    }

    private static final DateTimeFormatter localTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static String getLocalTime() {
        return LocalTime.now().format(localTimeFormatter);
    }

    private static final DateTimeFormatter localDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    public static String getLocalDateTime() {
        return LocalDateTime.now().format(localDateTimeFormatter);
    }

    // check if a given ip:port or port is within the format 1.1.1.1:1 or port > 0 & < 65536
    public static boolean isIpPort(String s) {
        return s.matches(
                "^((localhost)|((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}"
                        + "(25[0-5]|2[0-4]\\d|[01]?\\d\\d?))"
                        + ":([0-9]{1,5})$"
        );
    }

    public static boolean isPort(int port) {
        return port >= 0 && port <= 65535;
    }

    public static String cmdIndent(String output) {
        String indent = " ".repeat(11); // 11 is the # of spaces used by the time in console logs
        String[] lines = output.split("\n");

        for (int i = 1; i < lines.length; i++) {
            lines[i] = indent + lines[i];
        }

        return String.join("\n", lines);
    }
}
