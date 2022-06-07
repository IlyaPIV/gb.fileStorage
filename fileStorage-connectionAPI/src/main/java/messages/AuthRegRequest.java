package messages;

import lombok.Data;

@Data
public class AuthRegRequest implements CloudMessage{

    private String login;
    private String password;
    private boolean operationReg;

    public AuthRegRequest(String login, String password, boolean isReg) {
        this.login = login;
        this.password = password;
        this.operationReg = isReg;
    }
}
