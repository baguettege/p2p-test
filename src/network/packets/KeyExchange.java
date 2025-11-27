package network.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class KeyExchange implements Packet {
    private PublicKey publicKey;

    public KeyExchange() {}

    public KeyExchange(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        byte[] keyBytes = publicKey.getEncoded();
        out.writeInt(keyBytes.length);
        out.write(keyBytes);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
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
    }

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
