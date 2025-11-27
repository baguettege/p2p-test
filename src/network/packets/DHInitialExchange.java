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

public class DHInitialExchange implements Packet {
    private PublicKey publicKey;
    private BigInteger p;
    private BigInteger g;

    public DHInitialExchange() {}

    public DHInitialExchange(PublicKey publicKey, BigInteger p, BigInteger g) {
        this.publicKey = publicKey;
        this.p = p;
        this.g = g;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        // public key
        byte[] keyBytes = publicKey.getEncoded();
        out.writeInt(keyBytes.length);
        out.write(keyBytes);

        // p
        byte[] pBytes = p.toByteArray();
        out.writeInt(pBytes.length);
        out.write(pBytes);

        // g
        byte[] gBytes = g.toByteArray();
        out.writeInt(gBytes.length);
        out.write(gBytes);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        // public key
        int length = in.readInt();
        byte[] keyBytes = new byte[length];
        in.readFully(keyBytes);

        // build PublicKey
        try {
            KeyFactory kf = KeyFactory.getInstance("DH");
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            publicKey = kf.generatePublic(spec);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }

        // p
        int pLength = in.readInt();
        byte[] pBytes = new byte[pLength];
        in.readFully(pBytes);
        p = new BigInteger(pBytes);

        // g
        int gLength = in.readInt();
        byte[] gBytes = new byte[gLength];
        in.readFully(gBytes);
        g = new BigInteger(gBytes);
    }

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public BigInteger getP() {
        return p;
    }

    public BigInteger getG() {
        return g;
    }
}
