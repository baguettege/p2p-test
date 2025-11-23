package util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MainUtil {
    public static void log(String logText) {
        System.out.println(getLocalTime() + " | " + logText);
    }

    private static final DateTimeFormatter localTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static String getLocalTime() {
        return LocalTime.now().format(localTimeFormatter);
    }

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
}
