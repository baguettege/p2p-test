package network.packets;

public class PacketFactory {
    public static Packet create(String id) {
        return switch (id) {
            case "Message" -> new Message();
            default -> null;
        };
    }
}
