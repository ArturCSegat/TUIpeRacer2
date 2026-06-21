package com.tuiperacer.service;

public class LobbyEntry {

    private final String host;
    private final int port;
    private final String hostName;
    private int playerCount;

    public LobbyEntry(String host, int port, String hostName, int playerCount) {
        this.host = host;
        this.port = port;
        this.hostName = hostName;
        this.playerCount = playerCount;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getHostName() { return hostName; }
    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }

    @Override
    public String toString() {
        return hostName + " @ " + host + ":" + port + " (" + playerCount + " players)";
    }
}
