package serverFiles;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;


public class ServerFile implements Serializable {

    private String fileName;
    private long size;
    private LocalDateTime lastUpdate;
    private long serverID;


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public long getServerID() {
        return serverID;
    }

    public void setServerID(long serverID) {
        this.serverID = serverID;
    }

    @Override
    public String toString() {
        return "ServerFile{" +
                "fileName='" + fileName + '\'' +
                ", size=" + size + " bytes"+
                ", lastUpdate=" + lastUpdate +
                ", serverID=" + serverID +
                '}';
    }

}
