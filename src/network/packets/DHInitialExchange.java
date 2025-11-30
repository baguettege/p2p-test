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
    private PublicKey publicDHKey;
    private PublicKey publicAuthKey;
    private BigInteger p;
    private BigInteger g;
    private byte[] nonce;

    public DHInitialExchange() {}

    public DHInitialExchange(PublicKey publicDHKey, PublicKey publicAuthKey, BigInteger p, BigInteger g, byte[] nonce) {
        this.publicDHKey = publicDHKey;
        this.publicAuthKey = publicAuthKey;
        this.p = p;
        this.g = g;
        this.nonce = nonce;
    }

    private class WriteHelper {
        static void writePublicKey(DataOutputStream dos, PublicKey key) throws IOException {
            byte[] keyBytes = key.getEncoded();
            dos.writeInt(keyBytes.length);
            dos.write(keyBytes);
        }

        static void writeBigInteger(DataOutputStream dos, BigInteger bi) throws IOException {
            byte[] bytes = bi.toByteArray();
            dos.writeInt(bytes.length);
            dos.write(bytes);
        }
    }

    private class ReadHelper {
        static PublicKey readPublicKey(DataInputStream dis, String algorithm) throws IOException {
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

        static BigInteger readBigInteger(DataInputStream dis) throws IOException {
            int length = dis.readInt();
            byte[] bytes = new byte[length];
            dis.readFully(bytes);
            return new BigInteger(bytes);
        }
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        // public dh key
        WriteHelper.writePublicKey(out, publicDHKey);

        // public auth key
        WriteHelper.writePublicKey(out, publicAuthKey);

        // p
        WriteHelper.writeBigInteger(out, p);

        // g
        WriteHelper.writeBigInteger(out, g);

        // nonce
        out.writeInt(nonce.length);
        out.write(nonce);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        // public dh key
        publicDHKey = ReadHelper.readPublicKey(in, "DH");

        // public auth key
        publicAuthKey = ReadHelper.readPublicKey(in, "Ed25519");

        // p
        p = ReadHelper.readBigInteger(in);

        // g
        g = ReadHelper.readBigInteger(in);

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

    public BigInteger getP() {
        return p;
    }

    public BigInteger getG() {
        return g;
    }

    public byte[] getNonce() { return nonce; }
}
