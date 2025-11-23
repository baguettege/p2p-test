package network.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DataEnd implements Packet {
    public DataEnd() {}

    @Override
    public void write(DataOutputStream out) throws IOException {
    }

    @Override
    public void read(DataInputStream in) throws IOException {
    }

    @Override
    public String getId() {
        return "DataEnd";
    }
}
