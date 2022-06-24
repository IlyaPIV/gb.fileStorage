package messages;

import lombok.Data;

@Data
public class FileLinkRequest implements CloudMessage{

    private int linkID;

    public FileLinkRequest(int linkID) {
        this.linkID = linkID;
    }
}
