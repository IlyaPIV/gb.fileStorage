package serverFiles;

import java.io.Serializable;
import java.time.LocalDateTime;


public class ServerFile implements Serializable {

    public static final String HOME_DIR_NAME = " . . ";

    private String fileName;
    private long size;
    private LocalDateTime lastUpdate;
    private long serverID;

    private boolean isDir;

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

    public boolean isDir() {
        return isDir;
    }

    public void setDir(boolean dir) {
        isDir = dir;
    }

    @Override
    public String toString() {
        return "ServerFile{" +
                (isDir ? "directory='": "fileName='")
                + fileName + '\'' +
                ", size=" + size + " bytes"+
                ", lastUpdate=" + lastUpdate +
                ", serverID=" + serverID +
                '}';
    }

}
