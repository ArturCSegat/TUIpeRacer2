package com.tuiperacer.exception;

public class DuplicatePlayerException extends Exception {
    private final String playerName;

    public DuplicatePlayerException(String playerName) {
        super("A player with name '" + playerName + "' already exists in the race.");
        this.playerName = playerName;
    }

    public String getPlayerName() { return playerName; }
}
