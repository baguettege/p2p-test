package util;

import communication.EncryptionManager;
import main.Main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FileUtil {
    private static final Path mainDir = Path.of(System.getProperty("user.home"), "Documents", "p2p-test");
    private static Path logFile;
    private static Path downloadsDir;

    private static Path publicKeyFile;
    private static Path privateKeyFile;

    private static PublicKey publicAuthKey;
    private static PrivateKey privateAuthKey;

    private static Path trustedKeysDir;

    private static boolean isFilesInitialized = false;

    private static void logMain(String logText) { Main.logMain("FILEUTIL - " + logText);}

    public static void initFiles() {
        try {
            Files.createDirectories(mainDir);

            downloadsDir = mainDir.resolve("downloads");
            Files.createDirectories(downloadsDir);

            logFile = mainDir.resolve("latest.log");
            if (Files.exists(logFile)) Files.delete(logFile); // prevent spam log creation
            Files.createFile(logFile);

            initKeyFiles();

            isFilesInitialized = true;
            logMain("Files initialized: " + mainDir);
            logMain("To ensure confidentiality, do NOT share your private.key file contents!");

        } catch (IOException e) {
            logMain("Error when initializing files: " + e.getMessage());
        }
    }

    private static void initKeyFiles() {
        try {
            Path keyDir = mainDir.resolve("keys");
            Files.createDirectories(keyDir);
            publicKeyFile = keyDir.resolve("public.key");
            privateKeyFile = keyDir.resolve("private.key");

            trustedKeysDir = keyDir.resolve("trusted");
            Files.createDirectories(trustedKeysDir);

            boolean publicExists = Files.exists(publicKeyFile);
            boolean privateExists = Files.exists(privateKeyFile);

            if (!publicExists || !privateExists) {
                genAuthKeys();
            }

        } catch (IOException e) {
            logMain("Error when initializing key files: " + e.getMessage());
        }
    }

    public static void genAuthKeys() {
        KeyPair authKeyPair = EncryptionManager.genAuthenticationKeys();
        genPublicKey(authKeyPair.getPublic());
        genPrivateKey(authKeyPair.getPrivate());
    }

    private static void genPublicKey(PublicKey key) {
        try {
            if (Files.exists(publicKeyFile)) Files.delete(publicKeyFile);

            Files.createFile(publicKeyFile);
            Files.write(publicKeyFile, key.getEncoded());
            logMain("Generated public authentication key");
            publicAuthKey = convertToPublicKey(publicKeyFile);

        } catch (IOException e) {
            logMain("Error when generating public key: " + e.getMessage());
        }
    }

    private static void genPrivateKey(PrivateKey key) {
        try {
            if (Files.exists(privateKeyFile)) Files.delete(privateKeyFile);

            Files.createFile(privateKeyFile);
            Files.write(privateKeyFile, key.getEncoded());
            logMain("Generated private authentication key");
            privateAuthKey = convertToPrivateKey(privateKeyFile);

        } catch (IOException e) {
            logMain("Error when generating private key: " + e.getMessage());
        }
    }

    public static List<PublicKey> getAllTrustedKeys() {
        List<PublicKey> keyList = new ArrayList<>();
        List<Path> lst = getAllTrustedKeyPaths();
        if (lst == null) {
            return keyList;
        }

        for (Path keyPath : lst) {
            keyList.add(convertToPublicKey(keyPath));
        }

        return keyList;
    }

    private static List<Path> getAllTrustedKeyPaths() {
        List<Path> lst = new ArrayList<>();
        try (Stream<Path> stream = Files.list(trustedKeysDir)) {
            stream.forEach(path -> {
                String ext = getExtension(path);

                if ("key".equals(ext)) {
                    lst.add(path);
                }
            });

            return lst;

        } catch (IOException e) {
            logMain("Error when getting trusted keys: " + e.getMessage());
            return null;
        }
    }

    private static PublicKey convertToPublicKey(Path pth) {
        try {
            byte[] keyBytes = Files.readAllBytes(pth);
            KeyFactory kf = KeyFactory.getInstance("Ed25519");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            return kf.generatePublic(keySpec);

        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    private static PrivateKey convertToPrivateKey(Path pth) {
        try {
            byte[] keyBytes = Files.readAllBytes(pth);
            KeyFactory kf = KeyFactory.getInstance("Ed25519");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return kf.generatePrivate(keySpec);

        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getExtension(Path pth) {
        String name = String.valueOf(pth.getFileName());
        String[] parts = name.split("\\.");

        if (parts.length > 1) {
            return parts[parts.length-1];
        } else {
            return "";
        }
    }



    public static PublicKey getPublicAuthKey() {
        if (publicAuthKey == null) {
            return convertToPublicKey(publicKeyFile);
        } else {
            return publicAuthKey;
        }
    }

    public static PrivateKey getPrivateAuthKey() {
        if (privateAuthKey == null) {
            return convertToPrivateKey(privateKeyFile);
        } else {
            return privateAuthKey;
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
            logMain("Error when writing to log: " + e.getMessage());
        }
    }

    public static Path getTrustedKeysDir() { return trustedKeysDir; }

    public static void debugSetKeys(Path publicKeyPath, Path privateKeyPath) {
        setPublicKey(convertToPublicKey(publicKeyPath));
        setPrivateKey(convertToPrivateKey(privateKeyPath));
    }

    private static void setPublicKey(PublicKey key) { publicAuthKey = key; }
    private static void setPrivateKey(PrivateKey key) { privateAuthKey = key; }

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
}
