package network.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Message implements Packet {
    private String text;

    public Message() {}

    public Message(String text) {
        this.text = text;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(text);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        text = in.readUTF();
    }

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    public String getText() { return text; }
}
