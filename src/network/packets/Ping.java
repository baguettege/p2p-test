package network.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Ping implements Packet {
    private long timestamp;
    private boolean isReturning;

    public Ping() {
        timestamp = System.currentTimeMillis();
        isReturning = false;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeLong(timestamp);
        out.writeBoolean(isReturning);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        timestamp = in.readLong();
        isReturning = in.readBoolean();
    }

    @Override
    public String getId() {
        return "Ping";
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isReturning() {
        return isReturning;
    }

    public void setReturning() { isReturning = true; }
}
