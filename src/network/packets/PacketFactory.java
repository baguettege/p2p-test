package network.packets;

public class PacketFactory {

    // used purely for receiving packets so an empty packet can be made, then packet.read(in);

    public static Packet create(String id) {
        return switch (id) {
            case "Ping" -> new Ping();
            case "Message" -> new Message();
            case "Verify" -> new Verify();
            case "Disconnect" -> new Disconnect();
            case "Auth" -> new Auth();
            case "KeepAlive" -> new KeepAlive();
            case "DataBytes" -> new DataBytes();
            case "DataStart" -> new DataStart();
            case "DataEnd" -> new DataEnd();
            case "DataResponse" -> new DataResponse();
            case "DataRequest" -> new DataRequest();
            default -> null;
        };
    }
}
