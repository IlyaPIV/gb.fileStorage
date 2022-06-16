package messages;

import lombok.Data;

@Data
public class NewDirRequest implements CloudMessage{

    private String folderName;

    public NewDirRequest(String folderName) {
        this.folderName = folderName;
    }
}
