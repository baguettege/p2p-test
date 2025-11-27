package network.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileRequest implements Packet {
    private String fileName;
    private long fileSize;

    public FileRequest() {}

    public FileRequest(Path path) throws IOException {
        this.fileName = path.getFileName().toString();
        this.fileSize = Files.size(path);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(fileName);
        out.writeLong(fileSize);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        fileName = in.readUTF();
        fileSize = in.readLong();
    }

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
}
