package network;

import gui.Console;
import main.PacketHandler;
import network.packets.Packet;
import network.packets.PacketFactory;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Connection implements Runnable {
    private final Socket socket;
    private final Console console;

    private DataOutputStream out;
    private DataInputStream in;

    public Connection(Socket socket, Console console) {
        this.socket = socket;
        this.console = console;

        try {
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            console.log("Error when creating output/input streams: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        PacketHandler packetHandler = new PacketHandler(console);

        while (true) {
            try {
                String id = in.readUTF();
                Packet packet = PacketFactory.create(id);

                if (packet == null) {
                    console.log("Packet received was null: " + id);
                    continue;
                }

                packet.read(in);
                packetHandler.handle(packet);

            } catch (IOException e) {
                console.log("Error while reading packets: " + e.getMessage());
            }
        }
    }

    public void writePacket(Packet packet) {
        synchronized (out) {
            try {
                out.writeUTF(packet.getId());
                packet.write(out);
                out.flush();
            } catch (IOException e) {
                console.log("Error when writing packet: " + e.getMessage());
            }
        }
    }

    public void close() {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            console.log("Error when closing connection: " + e.getMessage());
        }

        console.close();
    }
}
