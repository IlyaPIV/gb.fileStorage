package messages;

import lombok.Data;

@Data
public class AuthRegAnswer implements CloudMessage{

    private boolean operationReg;
    private boolean result;
    private String message;

    public AuthRegAnswer(boolean result, String message, boolean operationReg) {
        this.result = result;
        this.message = message;
        this.operationReg = operationReg;
    }
}
