package network.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class FileResponse implements Packet {
    // peer's response to a DataStart packet requesting to send a file.
    // when response = true, peer is accepting, visa versa

    private boolean response;

    public FileResponse() {}

    public FileResponse(boolean response) { this.response = response; }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeBoolean(response);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        response = in.readBoolean();
    }

    @Override
    public String getId() {
        return "FileResponse";
    }

    public boolean getResponse() { return response; }
}
