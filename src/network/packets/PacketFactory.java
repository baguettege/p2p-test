package network.packets;

public class PacketFactory {
    public static Packet create(String id) {
        return switch (id) {
            case "Message" -> new Message();
            case "Verify" -> new Verify();
            case "Disconnect" -> new Disconnect();
            case "Auth" -> new Auth();
            default -> null;
        };
    }
}
