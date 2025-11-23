package network.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Template implements Packet {
    private String PLACEHOLDER;

    public Template() {}

    public Template(String PLACEHOLDER) {
        this.PLACEHOLDER = PLACEHOLDER;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(PLACEHOLDER);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        PLACEHOLDER = in.readUTF();
    }

    @Override
    public String getId() {
        return "Template";
    }
}
