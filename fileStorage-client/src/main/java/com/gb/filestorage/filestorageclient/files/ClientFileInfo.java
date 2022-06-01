package com.gb.filestorage.filestorageclient.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ClientFileInfo {

    private String filename;
    private FileType type;
    private long size;
    private LocalDateTime lastTimeModified;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public LocalDateTime getLastTimeModified() {
        return lastTimeModified;
    }

    public void setLastTimeModified(LocalDateTime lastTimeModified) {
        this.lastTimeModified = lastTimeModified;
    }

    public ClientFileInfo(Path path){

        try {
            this.filename = path.getFileName().toString();
            this.type = Files.isDirectory(path) ? FileType.DIRECTORY : FileType.FILE;
            this.size = this.type == FileType.DIRECTORY ? -1L : Files.size(path);
            this.lastTimeModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(0));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create file info from path: "+path.getFileName());
        }

    }
}
