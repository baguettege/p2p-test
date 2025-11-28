package main;

import network.Peer;
import network.packets.DHInitialExchange;
import network.packets.KeyExchange;

import javax.crypto.*;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.*;
import java.util.Arrays;

public class EncryptionHandler {
    private final Peer peer;
    private KeyPair keyPair;
    private SecretKeySpec keySpec;

    // server peer gens keypair + p & g, sends p & g + public key to client peer
    // client peer gens keypair with given p & g, generates shared secret with server's public key, sends public key to server
    // server peer gens shared secret
    // peers now use AES + shared secret key to encrypt + decrypt messages

    public EncryptionHandler(Peer peer) {
        this.peer = peer;
    }

    private void log(String logText) {
        peer.log(logText);
    }

    public boolean encryptionReady() { return keySpec != null; }

    public void initServerSide() {
        genKeyPair();
    }

    public void handleInitialExchange(DHInitialExchange packet) {
        PublicKey peerPublicKey = packet.getPublicKey();
        BigInteger p = packet.getP();
        BigInteger g = packet.getG();

        genKeyPair(p, g);
        genSharedSecret(peerPublicKey);
    }

    public void handleKeyExchange(KeyExchange packet) {
        PublicKey peerPublicKey = packet.getPublicKey();
        genSharedSecret(peerPublicKey);
    }

    private void genKeyPair() {
        if (keyPair != null) {
            log("genKeyPair() was called when keyPair != null!");
            return;
        }

        try {
            // built in dh params
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
            kpg.initialize(3072); // gens p & g
            keyPair = kpg.generateKeyPair(); // gens public & private key

            // extract dh params to get p & g
            DHPublicKey dhPublicKey = (DHPublicKey) keyPair.getPublic();
            DHParameterSpec params = dhPublicKey.getParams();
            BigInteger p = params.getP();
            BigInteger g = params.getG();

            peer.writePacket(new DHInitialExchange(keyPair.getPublic(), p, g));

            log("DH - Generated key pair with keysize 3072");

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void genKeyPair(BigInteger p, BigInteger g) {
        if (keyPair != null) {
            log("DH WARN - genKeyPair(BigInteger p, BigInteger g) was called when keyPair != null!");
            return;
        }

        try {
            DHParameterSpec params = new DHParameterSpec(p, g);
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
            kpg.initialize(params); // use the given p & g instead of auto creating
            keyPair = kpg.generateKeyPair();

            peer.writePacket(new KeyExchange(keyPair.getPublic()));

            log("DH - Generated key pair from peer's p & g");

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    private void genSharedSecret(PublicKey peerPublicKey) {
        if (keySpec != null) {
            log("DH WARN - genSharedSecret(PublicKey peerPublicKey) was called when keySpec != null!");
            return;
        }

        if (keyPair == null) {
            log("DH WARN - genSharedSecret(PublicKey peerPublicKey) was called when keyPair == null!");
            return;
        }

        try {
            KeyAgreement agree = KeyAgreement.getInstance("DH");
            agree.init(keyPair.getPrivate()); // sets private key
            agree.doPhase(peerPublicKey, true); // uses peer's key to complete dh exchange
            byte[] sharedSecret = agree.generateSecret();

            // convert raw bytes into usable aes key
            MessageDigest sha = MessageDigest.getInstance("SHA-256"); // HKDF is better, but not required
            byte[] aesKey = sha.digest(sharedSecret);

            keySpec = new SecretKeySpec(aesKey, "AES");

            log("DH - AES key generated");
            log("Encryption - Using AES-256-GCM/128");
            //log("DH DEBUG - AES KEY: " + java.util.Base64.getEncoder().encodeToString(aesKey)); // do not log this in production

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] encrypt(byte[] plainText) {
        if (keySpec == null) {
            log("encrypt(byte[] plainText) called when keySpec == null!");
            return null;
        }

        // create 12 byte iv (must be unique to all messages)
        byte[] iv = new byte[12];
        SecureRandom random = new SecureRandom(); // cryptographically strong rng
        random.nextBytes(iv); // fills array with 12 random bytes

        try {
            // create gcm params & cipher
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] cipherText = cipher.doFinal(plainText);

            // create array with 1st 12 bytes as iv, rest as the cipher text
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return buffer.array();

        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] decrypt(byte[] cipherText) {
        if (keySpec == null) {
            log("decrypt(byte[] cipherText) called when keySpec == null!");
            return null;
        }

        byte[] iv = Arrays.copyOfRange(cipherText, 0, 12);
        cipherText = Arrays.copyOfRange(cipherText, 12, cipherText.length);

        try {
            // create gcm params & cipher
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            return cipher.doFinal(cipherText);

        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
