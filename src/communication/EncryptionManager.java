package communication;

import main.Main;
import network.Peer;
import network.packets.DHInitialExchange;
import network.packets.DHKeyExchange;
import network.packets.Transcript;
import util.FileUtil;
import util.MainUtil;

import javax.crypto.*;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.util.Arrays;
import java.util.List;

public class EncryptionManager {
    private final Peer peer;
    private KeyPair DHKeyPair;
    private SecretKeySpec AESKeySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    private final String protocolId = "p2p-test";

    private final byte[] nonce = genNonce();

    private PublicKey peerPublicAuthKey;

    private byte[] transcript;
    private byte[] signedTranscript;

    // server peer gens keypair + p & g, sends p & g + public key to client peer
    // client peer gens keypair with given p & g, generates shared secret with server's public key, sends public key to server
    // server peer gens shared secret
    // peers now use AES + shared secret key to encrypt + decrypt messages

    public static KeyPair genAuthenticationKeys() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
            return kpg.generateKeyPair();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public EncryptionManager(Peer peer) {
        this.peer = peer;
        startHandshakeTimeout();
    }

    private void log(String logText) { peer.log("CRYPTO - " + logText); }
    private static void logMain(String logText) { Main.logMain("CRYPTO - " + logText); }

    public boolean encryptionReady() { return AESKeySpec != null; }

    public void initServerSide() { genKeyPair(); }

    private boolean transcriptIsVerified = false;
    private boolean isHandshakeCompleted() {
        return (AESKeySpec != null) && transcriptIsVerified;
    }

    private void startHandshakeTimeout() {
        new Thread(() -> {
            try {
                Thread.sleep(10000);
                if (!isHandshakeCompleted()) {
                    log("Handshake timed out; disconnecting");
                    peer.disconnect();
                }

            } catch (InterruptedException _) {}
        }).start();
    }

    public static boolean isKeyTrusted(PublicKey key) {
        List<PublicKey> trustedKeys = FileUtil.getAllTrustedKeys();
        if (trustedKeys.isEmpty()) {
            return false;
        }

        byte[] encodedKey = key.getEncoded();

        for (PublicKey trustedKey : trustedKeys) {
            boolean equal = Arrays.equals(trustedKey.getEncoded(), encodedKey);
            if (equal) {
                return true;
            }
        }

        return false;
    }

    public void handleInitialExchange(DHInitialExchange packet) {
        PublicKey peerPublicDHKey = packet.getPublicDHKey();
        BigInteger p = packet.getP();
        BigInteger g = packet.getG();

        genKeyPair(p, g);
        genSharedSecret(peerPublicDHKey);
        
        peerPublicAuthKey = packet.getPublicAuthKey();
        if (!isKeyTrusted(peerPublicAuthKey)) {
            log("Peer's public key is untrusted; disconnecting");
            peer.disconnect();
            return;
        } else {
            transcriptIsVerified = true;
            log("Peer's public key is trusted");
        }

        transcript = genTranscript(
                protocolId,
                FileUtil.getPublicAuthKey(),
                peerPublicAuthKey,
                DHKeyPair.getPublic(),
                peerPublicDHKey,
                nonce,
                packet.getNonce()
        );

        signedTranscript = signTranscript(transcript, FileUtil.getPrivateAuthKey());
        peer.writePacket(new Transcript(signedTranscript));
    }

    public void handleKeyExchange(DHKeyExchange packet) {
        PublicKey peerPublicDHKey = packet.getPublicDHKey();
        genSharedSecret(peerPublicDHKey);

        peerPublicAuthKey = packet.getPublicAuthKey();
        if (!isKeyTrusted(peerPublicAuthKey)) {
            log("Peer's public key is untrusted; disconnecting");
            peer.disconnect();
            return;
        } else {
            transcriptIsVerified = true;
            log("Peer's public key is trusted");
        }

        // this is different to the one in handleInitialExchange()
        // not me vs peer but server vs client
        transcript = genTranscript(
                protocolId,
                peerPublicAuthKey,
                FileUtil.getPublicAuthKey(),
                peerPublicDHKey,
                DHKeyPair.getPublic(),
                packet.getNonce(),
                nonce
        );

        signedTranscript = signTranscript(transcript, FileUtil.getPrivateAuthKey());
        peer.writePacket(new Transcript(signedTranscript));
    }

    public void handleTranscript(Transcript packet) {
        byte[] peerSignedTranscript = packet.getSignedTranscript();
        boolean mitmOccurring = !verifyTranscript(transcript, peerSignedTranscript, peerPublicAuthKey);

        if (!mitmOccurring) {
            log("Verified peer's signature");
        } else {
            log(MainUtil.cmdIndent("Peer's signature was NOT verified!\nMITM detected; disconnecting"));
            peer.disconnect();
        }
    }
    
    private static boolean verifyTranscript(byte[] ownTranscript, byte[] peerSignedTranscript, PublicKey peerPublicAuthKey) {
        // verify that mitm is not occuring with own raw transcript, peer's signature and their public auth key
        try {
            Signature sign = Signature.getInstance("Ed25519");
            sign.initVerify(peerPublicAuthKey);
            sign.update(ownTranscript);
            return sign.verify(peerSignedTranscript);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] signTranscript(byte[] transcript, PrivateKey privateKey) {
        // all transcripts send MUST be signed
        try {
            // signs transcript with private key
            Signature sign = Signature.getInstance("Ed25519");
            sign.initSign(privateKey);
            sign.update(transcript);
            return sign.sign();

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] genNonce() {
        byte[] nonce = new byte[32];
        secureRandom.nextBytes(nonce);
        return nonce;
    }

    private static byte[] genTranscript(
            String protocolId,
            PublicKey publicAuthKey,
            PublicKey peerPublicAuthKey,
            PublicKey publicDHKey,
            PublicKey peerPublicDHKey,
            byte[] nonce,
            byte[] peerNonce
    ) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        writeTranscriptField(baos, protocolId.getBytes());
        writeTranscriptField(baos, publicAuthKey.getEncoded());
        writeTranscriptField(baos, peerPublicAuthKey.getEncoded());
        writeTranscriptField(baos, publicDHKey.getEncoded());
        writeTranscriptField(baos, peerPublicDHKey.getEncoded());
        writeTranscriptField(baos, nonce);
        writeTranscriptField(baos, peerNonce);

        return baos.toByteArray();
    }

    private static void writeTranscriptField(ByteArrayOutputStream baos, byte[] field) {
        try {
            // allows the byte array field length to be known, so can create a new byte[] with correct length and fill
            ByteBuffer length = ByteBuffer.allocate(4).putInt(field.length);
            baos.write(length.array());
            baos.write(field);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void genKeyPair() {
        if (DHKeyPair != null) {
            log("genKeyPair() was called when keyPair != null!");
            return;
        }

        try {
            // built in dh params
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
            kpg.initialize(3072); // gens p & g
            DHKeyPair = kpg.generateKeyPair(); // gens public & private key

            // extract dh params to get p & g
            DHPublicKey dhPublicKey = (DHPublicKey) DHKeyPair.getPublic();
            DHParameterSpec params = dhPublicKey.getParams();
            BigInteger p = params.getP();
            BigInteger g = params.getG();

            peer.writePacket(new DHInitialExchange(DHKeyPair.getPublic(), FileUtil.getPublicAuthKey(), p, g, nonce));

            log("DH - Generated key pair with keysize 3072");

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void genKeyPair(BigInteger p, BigInteger g) {
        if (DHKeyPair != null) {
            log("DH WARN - genKeyPair(BigInteger p, BigInteger g) was called when keyPair != null!");
            return;
        }

        try {
            DHParameterSpec params = new DHParameterSpec(p, g);
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
            kpg.initialize(params); // use the given p & g instead of auto creating
            DHKeyPair = kpg.generateKeyPair();

            peer.writePacket(new DHKeyExchange(DHKeyPair.getPublic(), FileUtil.getPublicAuthKey(), nonce));

            log("DH - Generated key pair from peer's prime and generator");

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    private void genSharedSecret(PublicKey peerPublicKey) {
        if (AESKeySpec != null) {
            log("DH WARN - genSharedSecret(PublicKey peerPublicKey) was called when keySpec != null!");
            return;
        }

        if (DHKeyPair == null) {
            log("DH WARN - genSharedSecret(PublicKey peerPublicKey) was called when keyPair == null!");
            return;
        }

        try {
            KeyAgreement agree = KeyAgreement.getInstance("DH");
            agree.init(DHKeyPair.getPrivate()); // sets private key
            agree.doPhase(peerPublicKey, true); // uses peer's key to complete dh exchange
            byte[] sharedSecret = agree.generateSecret();

            // convert raw bytes into usable aes key
            MessageDigest sha = MessageDigest.getInstance("SHA-256"); // HKDF is better, but not required
            byte[] aesKey = sha.digest(sharedSecret);

            AESKeySpec = new SecretKeySpec(aesKey, "AES");

            log("DH - AES key generated");
            log("Using AES-256-GCM/128");

            peer.onHandshakeCompleted();

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }



    public byte[] encrypt(byte[] plainText) {
        if (AESKeySpec == null) {
            log("encrypt(byte[] plainText) called when keySpec == null!");
            return null;
        }

        // create 12 byte iv (must be unique to all messages)
        byte[] iv = new byte[12];
        secureRandom.nextBytes(iv); // fills array with 12 random bytes

        try {
            // create gcm params & cipher
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, AESKeySpec, gcmSpec);
            byte[] cipherText = cipher.doFinal(plainText);

            // create array with 1st 12 bytes as iv, rest as the cipher text
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return buffer.array();

        } catch (GeneralSecurityException e) {
            log("GeneralSecurityException thrown when encrypting: " + e.getMessage());
            peer.disconnect();
            return null;
        }
    }

    public byte[] decrypt(byte[] cipherText) {
        if (AESKeySpec == null) {
            log("decrypt(byte[] cipherText) called when keySpec == null!");
            return null;
        }

        byte[] iv = Arrays.copyOfRange(cipherText, 0, 12);
        cipherText = Arrays.copyOfRange(cipherText, 12, cipherText.length);

        try {
            // create gcm params & cipher
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, AESKeySpec, gcmSpec);
            return cipher.doFinal(cipherText);

        } catch (GeneralSecurityException e) {
            log("GeneralSecurityException thrown when decrypting: " + e.getMessage());
            peer.disconnect();
            return null;
        }
    }

    public static void addTrustedKey() {
        Path selectedFile = Main.window().chooseFile(FileUtil.getTrustedKeysDir());
        if (selectedFile == null) {
            logMain("Key selection cancelled");
            return;
        }

        String extension = FileUtil.getExtension(selectedFile);
        if (!"key".equals(extension)) {
            logMain("Selected file was not a .key file; cancelling selection");
            return;
        }

        try {
            Path target = FileUtil.getTrustedKeysDir().resolve(selectedFile.getFileName());
            Files.move(selectedFile, target);
            logMain("Added trusted key: " + selectedFile.getFileName());
        } catch (IOException e) {
            e.printStackTrace();
            logMain("Error when adding trusted key: " + e.getMessage());
        }
    }

    public static void removeTrustedKey() {
        Path selectedFile = Main.window().chooseFile(FileUtil.getTrustedKeysDir());
        if (selectedFile == null) {
            logMain("Key selection cancelled");
            return;
        }

        String name = String.valueOf(selectedFile.getFileName());
        try {
            Files.delete(selectedFile);
            logMain("Removed trusted key: " + name);
        } catch (IOException e) {
            e.printStackTrace();
            logMain("Error when removing trusted key: " + e.getMessage());
        }
    }

    public static void resetAuthKeys() {
        logMain("Resetting public and private authentication keys");
        FileUtil.genAuthKeys();
    }
}
