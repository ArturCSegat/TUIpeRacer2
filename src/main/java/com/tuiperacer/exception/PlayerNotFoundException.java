package com.tuiperacer.exception;

public class PlayerNotFoundException extends Exception {
    private final String playerName;

    public PlayerNotFoundException(String playerName) {
        super("Player not found: '" + playerName + "'");
        this.playerName = playerName;
    }

    public String getPlayerName() { return playerName; }
}
