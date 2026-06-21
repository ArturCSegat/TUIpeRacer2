package com.tuiperacer.exception;

public class ConnectionException extends Exception {
    private final String host;
    private final int port;

    public ConnectionException(String host, int port, String reason) {
        super("Cannot connect to " + host + ":" + port + " - " + reason);
        this.host = host;
        this.port = port;
    }

    public ConnectionException(String message) {
        super(message);
        this.host = "";
        this.port = 0;
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
        this.host = "";
        this.port = 0;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
}
