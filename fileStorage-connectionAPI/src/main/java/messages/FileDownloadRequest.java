package messages;

import lombok.Data;

@Data
public class FileDownloadRequest implements CloudMessage{

    private String fileName;

    private long fileID;

    public FileDownloadRequest(String fileName, long fileID) {
        this.fileName = fileName;
        this.fileID = fileID;
    }
}
