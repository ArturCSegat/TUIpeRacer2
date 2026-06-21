package com.tuiperacer.exception;

public class FileLoadException extends Exception {
    private final String filePath;

    public FileLoadException(String filePath, String reason) {
        super("Failed to load file '" + filePath + "': " + reason);
        this.filePath = filePath;
    }

    public FileLoadException(String message, Throwable cause) {
        super(message, cause);
        this.filePath = "";
    }

    public String getFilePath() { return filePath; }
}
