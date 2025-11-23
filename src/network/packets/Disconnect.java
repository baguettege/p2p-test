package network.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Disconnect implements Packet {
    private String reason;

    public Disconnect() {}

    public Disconnect(String reason) {
        this.reason = reason;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(reason);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        reason = in.readUTF();
    }

    @Override
    public String getId() {
        return "Disconnect";
    }

    public String getReason() { return reason; }
}
