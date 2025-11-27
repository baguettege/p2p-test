package network.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileHeader implements Packet {
    private String fileName;
    private long fileSize;
    private int chunkSize;

    public FileHeader() {}

    public FileHeader(Path path) throws IOException {
        this.fileName = path.getFileName().toString();
        this.fileSize = Files.size(path);
        this.chunkSize = 65536; //64KB
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(fileName);
        out.writeLong(fileSize);
        out.writeInt(chunkSize);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        fileName = in.readUTF();
        fileSize = in.readLong();
        chunkSize = in.readInt();
    }

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public int getChunkSize() { return chunkSize; }
}
