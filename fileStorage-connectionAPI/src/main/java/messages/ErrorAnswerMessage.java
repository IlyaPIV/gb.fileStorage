package messages;

import lombok.Data;

@Data
public class ErrorAnswerMessage implements CloudMessage{

    private String message;

    public ErrorAnswerMessage(String message) {
        this.message = message;
    }
}
