package network.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Auth implements Packet {
    private String status;

    public Auth() {}

    public Auth(String status) {
        this.status = status;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(status);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        status = in.readUTF();
    }

    @Override
    public String getId() {
        return "Auth";
    }

    public String getStatus() { return status; }
}
