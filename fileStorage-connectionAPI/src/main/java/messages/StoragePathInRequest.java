package messages;

import lombok.Data;

@Data
public class StoragePathInRequest implements CloudMessage{

    private String dirName;
    private long dirID;

    public StoragePathInRequest(String dirName, long dirID) {
        this.dirName = dirName;
        this.dirID = dirID;
    }
}
