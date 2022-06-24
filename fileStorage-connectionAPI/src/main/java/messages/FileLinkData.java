package messages;

import lombok.Data;

@Data
public class FileLinkData implements CloudMessage{

    private String cryptoLink;

    public FileLinkData(String cryptoLink) {
        this.cryptoLink = cryptoLink;
    }
}
