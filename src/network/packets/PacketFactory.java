package network.packets;

public class PacketFactory {

    // used purely for receiving packets so an empty packet can be made, then packet.read(in);

    public static Packet create(String id) {
        return switch (id) {
            case "Ping" -> new Ping();
            case "Message" -> new Message();
            case "KeepAlive" -> new KeepAlive();
            case "FileData" -> new FileData();
            case "FileHeader" -> new FileHeader();
            case "FileFooter" -> new FileFooter();
            case "FileResponse" -> new FileResponse();
            case "FileRequest" -> new FileRequest();
            case "FileCancelUpload" -> new FileCancelUpload();
            case "FileCancelDownload" -> new FileCancelDownload();
            case "DHInitialExchange" -> new DHInitialExchange();
            case "DHKeyExchange" -> new DHKeyExchange();
            case "Transcript" -> new Transcript();
            default -> null;
        };
    }
}
