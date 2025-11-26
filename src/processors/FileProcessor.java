package processors;

import main.Main;
import network.Peer;
import network.packets.*;
import util.FileUtil;
import util.MainUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class FileProcessor {
    private final Peer peer;

    public FileProcessor(Peer conn) {
        peer = conn;
    }

    private void log(String logText) {
        if (peer != null) peer.log(logText);
    }

    enum SendState {
        IDLE,
        REQUESTED,
        SENDING
    }

    enum ReceiveState {
        IDLE,
        PEER_REQUESTED,
        RECEIVING
    }

    private volatile SendState sendState = SendState.IDLE;
    private volatile ReceiveState receiveState = ReceiveState.IDLE;

    // receiving files vars
    private OutputStream fileOutputStream;
    private String fileName;
    private long fileSize;
    private long bytesReceived;
    private int expectedIndex;

    private int lastPercent = -1;
    private Path target;
    private long receiveStartTime;

    public void processHeader(FileHeader packet) {
        if (receiveState != ReceiveState.RECEIVING) return;
        receiveState = ReceiveState.RECEIVING;

        receiveStartTime = System.currentTimeMillis();

        this.fileName = packet.getFileName();
        this.fileSize = packet.getFileSize();
        this.bytesReceived = 0;
        this.expectedIndex = 0;

        target = FileUtil.getDownloadsDir().resolve(fileName);
        log("Downloading file: " + fileName + " | " + FileUtil.getFileSize(fileSize));

        try {
            this.fileOutputStream = Files.newOutputStream(target);
        } catch (IOException e) {
            e.printStackTrace();
            log("Error when creating file output stream: " + e.getMessage());
        }
    }

    public void processData(FileData packet) {
        if (receiveState != ReceiveState.RECEIVING) {
            //log("DEBUG - Received FileData packet");
            return;
        }

        int index = packet.getIndex();

        if (index != expectedIndex) {
            log("WARN - Expected chunk index " + expectedIndex + " but got " + index + "!");
        }

        int length = packet.getLength();
        try {
            fileOutputStream.write(packet.getData(), 0, length); // wrote chunk
        } catch (IOException e) {
            //e.printStackTrace();
            //log("Error when writing chunk to disk: " + e.getMessage());
        }

        // update
        bytesReceived += length;
        expectedIndex++;

        // progress checking
        double percent = ((double) bytesReceived /fileSize) * 100.0;
        int wholePercent = (int) percent;

        if (wholePercent % 10 == 0 && wholePercent != lastPercent) {
            double elapsedSeconds = (System.currentTimeMillis() - receiveStartTime) / 1000.0;
            double speed = bytesReceived / elapsedSeconds;
            String fmtSpeed = FileUtil.getFileSize(speed);

            String received = FileUtil.getFileSize(bytesReceived);
            String total = FileUtil.getFileSize(fileSize);
            log("File download progress: " + received + "/" + total + " | " + wholePercent + "% | " + fmtSpeed + "/s | " + (long) elapsedSeconds + "s elapsed");
            lastPercent = wholePercent;
        }
    }

    public void processFooter() {
        if (receiveState != ReceiveState.RECEIVING) {
            //log("DEBUG - Received FileFooter packet");
            return;
        }

        try {
            if (fileOutputStream != null) fileOutputStream.close();
            long elapsedSeconds = (long) ((System.currentTimeMillis() - receiveStartTime) / 1000.0);
            log(MainUtil.cmdIndent("File downloaded successfully: " + fileName + " | " + elapsedSeconds + "s elapsed\nSaved to: " + target));

            resetReceiveStates();

        } catch (IOException e) {
            log("Error closing file: " + e.getMessage());
        }
    }

    private void resetReceiveStates() {
        fileOutputStream = null;
        fileName = null;
        fileSize = 0;
        bytesReceived = 0;
        expectedIndex = 0;
        lastPercent = -1;
        target = null;
        receiveStartTime = 0;
        receiveState = ReceiveState.IDLE;
    }

    private Thread requestTimeoutThread;

    public synchronized void processRequest(FileRequest packet) {
        if (receiveState == ReceiveState.RECEIVING) {
            //log("Peer requested file transfer while already receiving a file; declining");
            peer.writePacket(new FileResponse(false));
            return;
        }

        receiveState = ReceiveState.PEER_REQUESTED;
        log(MainUtil.cmdIndent("Peer requested file transfer: " + packet.getFileName() + " | " + FileUtil.getFileSize(packet.getFileSize()) + "\ncmd 'file accept/decline' for this request; will timeout in 30s...\ncmd 'file cancel download' to cancel mid-transfer"));

        requestTimeoutThread = new Thread(() -> {
            try {
                Thread.sleep(30000);
                declineRequest();
            } catch (InterruptedException _) {}
        });

        requestTimeoutThread.start();
    }

    public synchronized void processResponse(FileResponse packet) {
        boolean isAccepting = packet.getResponse();
        boolean hasRequestedToSend = sendState == SendState.REQUESTED;

        if (isAccepting && hasRequestedToSend) {
            peer.log("Peer accepted file transfer request, transferring...");
            writeFile();
        } else if (!isAccepting && hasRequestedToSend) {
            peer.log("Peer declined file transfer request");
            sendState = SendState.IDLE;
        } else {
            // peer sent a DataResponse packet when self did not request to send file; ignore this
        }
    }

    public synchronized void acceptRequest() {
        if (receiveState == ReceiveState.PEER_REQUESTED) {
            log("Accepted file transfer request; downloading...");
            peer.writePacket(new FileResponse(true));
            requestTimeoutThread.interrupt();
            receiveState = ReceiveState.RECEIVING;
        } else {
            log("No file transfer request was sent");
        }
    }

    public synchronized void declineRequest() {
        if (receiveState == ReceiveState.PEER_REQUESTED) {
            log("Declined file transfer request");
            peer.writePacket(new FileResponse(false));
            requestTimeoutThread.interrupt();
            receiveState = ReceiveState.IDLE;
        } else {
            log("No file transfer request was sent");
        }
    }

    private Path selectedFile;
    private InputStream fileInputStream;

    public synchronized void selectFile() { // send transfer request
        if (sendState != SendState.IDLE) {
            log("Unable to select file when uploading file/have sent file transfer request to peer");
            return;
        }

        selectedFile = Main.window().chooseFile();
        if (selectedFile == null) {
            log("File upload cancelled");
            return;
        }

        try {
            log(MainUtil.cmdIndent("Sending file transfer request to peer: " + selectedFile.getFileName() + " | " + FileUtil.getFileSize(Files.size(selectedFile)) + "\ncmd 'file cancel upload' to cancel transfer"));
            peer.writePacket(new FileRequest(selectedFile));
            sendState = SendState.REQUESTED;
        } catch (IOException e) {
            log("Error when requesting file transfer: " + e.getMessage());
        }
    }

    private void writeFile() {
        if (sendState == SendState.SENDING) {
            log("Unable to upload file whilst uploading another one");
            return;
        }
        if (selectedFile == null) {
            log("WARN - selectedFile was null when writeFile(); was called");
            return;
        }

        sendState = SendState.SENDING;
        long sendStartTime = System.currentTimeMillis();

        new Thread(() -> { // new thread so swing doesnt freeze
            try {
                peer.writePacket(new FileHeader(selectedFile));

                long bytesSent = 0;
                long fileSize = Files.size(selectedFile);
                int lastPercent = -1;

                fileInputStream = Files.newInputStream(selectedFile);

                byte[] buffer = new byte[65536]; //64KB
                int index = 0;
                int read;

                // read then send chunks of bytes from the file
                while ((read = fileInputStream.read(buffer)) != -1) { // while file hasnt ended
                    byte[] chunk = Arrays.copyOf(buffer, read);
                    peer.writePacket(new FileData(index, chunk));
                    index++;

                    // progress tracking
                    bytesSent += read;

                    double percent = ((double) bytesSent /fileSize) * 100.0;
                    int wholePercent = (int) percent;

                    if (wholePercent % 10 == 0 && wholePercent != lastPercent) {
                        double elapsedSeconds = (System.currentTimeMillis() - sendStartTime) / 1000.0;
                        double speed = bytesSent / elapsedSeconds;
                        String fmtSpeed = FileUtil.getFileSize(speed);

                        String received = FileUtil.getFileSize(bytesSent);
                        String total = FileUtil.getFileSize(fileSize);
                        log("File upload progress: " + received + "/" + total + " | " + wholePercent + "% | " + fmtSpeed + "/s | " + (long) elapsedSeconds + "s elapsed");
                        lastPercent = wholePercent;
                    }
                }

                peer.writePacket(new FileFooter());

                log(MainUtil.cmdIndent("File uploaded successfully: " + selectedFile.getFileName() + "\nFrom: " + selectedFile));
                selectedFile = null;
                sendState = SendState.IDLE;

            } catch (IOException e) {
                //log("Error when writing file: " + e.getMessage());
            } finally {
                try {
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                } catch (IOException ex) {
                    log("Error when closing fileInputStream: " + ex.getMessage());
                }
                fileInputStream = null;
            }
        }).start();
    }

    public void cancelUpload(boolean fromPeer) {
        if (sendState == SendState.IDLE) { // allow requested
            log("Unable to cancel upload as you are not currently uploading a file");
            return;
        }

        if (!fromPeer) { // will result in an endless loop of packet sending without this flag
            peer.writePacket(new FileCancelUpload());
        } else {
            log("Peer cancelled download");
        }

        log("Cancelling upload...");

        try {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        } catch (IOException ex) {
            log("Error when closing fileInputStream: " + ex.getMessage());
        }

        selectedFile = null;
        sendState = SendState.IDLE;
        log("Cancelled upload successfully");
    }

    public void cancelDownload(boolean fromPeer) {
        if (receiveState != ReceiveState.RECEIVING && !fromPeer) {
            log("Unable to cancel download as you are not currently downloading a file");
            return;
        }

        if (!fromPeer) { // will result in an endless loop of packet sending without this flag
            peer.writePacket(new FileCancelDownload());
        } else {
            log("Peer cancelled upload");
        }

        log("Cancelling download...");

        try {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        } catch (IOException e) {
            log("Error when closing fileOutputStream: " + e.getMessage());
        }

        try {
            if (target != null) {
                Files.delete(target);
            }
        } catch (IOException e) {
            log("Error when deleting partially downloaded file: " + e.getMessage());
        }

        resetReceiveStates();
        log("Cancelled download successfully");
    }
}
