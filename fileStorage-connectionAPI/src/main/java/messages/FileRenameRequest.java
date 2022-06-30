package messages;

import lombok.Data;

@Data
public class FileRenameRequest implements CloudMessage{

    private int linkID;

    private String newName;

    public FileRenameRequest(int linkID, String newName) {
        this.linkID = linkID;
        this.newName = newName;
    }
}
