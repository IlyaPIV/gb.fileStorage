package serverFiles;

import java.io.Serializable;
import java.time.LocalDateTime;


public class ServerFile implements Serializable {

    public static final String HOME_DIR_NAME = " . . ";

    private String fileName;
    private long size;
    private LocalDateTime lastUpdate;
    private long serverID;
    private int linkID;

    private boolean isDir;

    private int poz; //порядковый номер для сортировки

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

    public void setPoz(int poz) {
        this.poz = poz;
    }

    public int getPoz() {
        return poz;
    }

    public int getLinkID() {
        return linkID;
    }

    public void setLinkID(int linkID) {
        this.linkID = linkID;
    }

    @Override
    public String toString() {
        return "ServerFile{" +
                (isDir ? "directory='": "fileName='")
                + fileName + '\'' +
                ", size=" + size + " bytes"+
                ", lastUpdate=" + lastUpdate +
                ", fileID=" + serverID +
                ", linkID=" + linkID +
                '}';
    }

}
