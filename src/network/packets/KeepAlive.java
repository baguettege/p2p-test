package network.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class KeepAlive implements Packet {
    public KeepAlive() {}

    @Override
    public void write(DataOutputStream out) throws IOException {
    }

    @Override
    public void read(DataInputStream in) throws IOException {
    }

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }
}
