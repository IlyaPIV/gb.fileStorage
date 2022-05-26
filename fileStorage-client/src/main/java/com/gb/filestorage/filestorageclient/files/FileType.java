package com.gb.filestorage.filestorageclient.files;

public enum FileType {
    FILE("F"),
    DIRECTORY("D");

    private String name;

    public String getName() {
        return name;
    }

    FileType(String name) {
        this.name = name;
    }
}