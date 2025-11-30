package network.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Transcript implements Packet {
    private byte[] signedTranscript;

    public Transcript() {}

    public Transcript(byte[] signedTranscript) {
        this.signedTranscript = signedTranscript;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(signedTranscript.length);
        out.write(signedTranscript);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        int length = in.readInt();
        signedTranscript = new byte[length];
        in.readFully(signedTranscript);
    }

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    public byte[] getSignedTranscript() { return signedTranscript; }
}
