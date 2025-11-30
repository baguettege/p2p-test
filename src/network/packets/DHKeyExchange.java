package network.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class DHKeyExchange implements Packet {
    private PublicKey publicDHKey;
    private PublicKey publicAuthKey;
    private byte[] nonce;

    public DHKeyExchange() {}

    public DHKeyExchange(PublicKey publicDHKey, PublicKey publicAuthKey, byte[] nonce) {
        this.publicDHKey = publicDHKey;
        this.publicAuthKey = publicAuthKey;
        this.nonce = nonce;
    }

    private static void writePublicKey(DataOutputStream dos, PublicKey key) throws IOException {
        byte[] keyBytes = key.getEncoded();
        dos.writeInt(keyBytes.length);
        dos.write(keyBytes);
    }

    private static PublicKey readPublicKey(DataInputStream dis, String algorithm) throws IOException {
        int length = dis.readInt();
        byte[] keyBytes = new byte[length];
        dis.readFully(keyBytes);

        // build key
        try {
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return kf.generatePublic(spec);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        // dh key
        writePublicKey(out, publicDHKey);

        // auth key
        writePublicKey(out, publicAuthKey);

        // nonce
        out.writeInt(nonce.length);
        out.write(nonce);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        // dh key
        publicDHKey = readPublicKey(in, "DH");

        // auth key
        publicAuthKey = readPublicKey(in, "Ed25519");

        // nonce
        int nonceLength = in.readInt();
        nonce = new byte[nonceLength];
        in.readFully(nonce);

    }

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    public PublicKey getPublicDHKey() {
        return publicDHKey;
    }

    public PublicKey getPublicAuthKey() {
        return publicAuthKey;
    }

    public byte[] getNonce() { return nonce; }
}
