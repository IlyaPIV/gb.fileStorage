package messages;

import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Data
public class FileTransferData implements CloudMessage{

    private final long size;
    private final byte[] data;
    private final String name;

    public FileTransferData(Path path) throws IOException {
        this.size = Files.size(path);
        this.data = Files.readAllBytes(path);
        this.name = path.getFileName().toString();
    }

    public FileTransferData(byte[] data, String fileName) {
        this.data = data;
        this.size = data.length;
        this.name = fileName;
    }
}
