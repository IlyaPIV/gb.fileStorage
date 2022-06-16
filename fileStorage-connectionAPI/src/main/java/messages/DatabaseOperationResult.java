package messages;

import lombok.Data;

@Data
public class DatabaseOperationResult implements CloudMessage{

    private boolean result;

    private String message;

    public DatabaseOperationResult(boolean result, String message) {
        this.result = result;
        this.message = message;
    }
}
