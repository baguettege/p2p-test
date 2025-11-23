package network.packets;

import util.FileUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Data implements Packet {
    private String fileName;
    private long length;
    private byte[] data;

    public Data() {}

    public Data(Path path) throws IOException {
        this.fileName = path.getFileName().toString();
        this.data = Files.readAllBytes(path); // this is what can cause the program to crash if file size is too large
        this.length = data.length;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(fileName);
        out.writeLong(length);
        out.write(data);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        fileName = in.readUTF();
        length = in.readLong();
        data = new byte[(int) length]; // readFully will throw NullPointerException if this line doesnt exist
        in.readFully(data);
    }

    @Override
    public String getId() {
        return "Data";
    }

    public void saveTo(Path directory) throws IOException {
        Files.createDirectories(directory);
        Path target = directory.resolve(FileUtil.getFileNameWithTime(fileName));
        Files.write(target, data);
    }

    public String getFileName() { return fileName; }

    public long getLength() { return length; }
}
