package messages;

import lombok.Data;

@Data
public class FileDownloadRequest implements CloudMessage{

    private final String fileName;

}
