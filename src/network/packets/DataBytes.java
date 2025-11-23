package network.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DataBytes implements Packet {
    private int index;
    private int length;
    private byte[] data;

    public DataBytes() {}

    public DataBytes(int index, byte[] data) throws IOException {
        this.index = index;
        this.length = data.length;
        this.data = data;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(index);
        out.writeInt(length);
        out.write(data, 0, length);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        index = in.readInt();
        length = in.readInt();
        data = new byte[length];
        in.readFully(data);
    }

    @Override
    public String getId() {
        return "DataBytes";
    }

    public int getIndex() { return index; }
    public int getLength() { return length; }
    public byte[] getData() { return data; }
}
