package messages;

import lombok.Data;

@Data
public class DeleteRequest implements CloudMessage{

    private boolean isDir;
    private int id;

    public DeleteRequest(boolean isDir, int id) {
        this.isDir = isDir;
        this.id = id;
    }
}
