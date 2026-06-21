package com.tuiperacer.model;

import com.tuiperacer.exception.InvalidInputException;
import java.time.LocalDateTime;

public class PlayerTcp extends Player {

    private final int playerId; // slot number assigned by server (0 = host)

    public PlayerTcp(String name, String textToType, int playerId) {
        super(name, textToType);
        this.playerId = playerId;
    }

    public int getPlayerId() { return playerId; }

    @Override
    public PlayerType getType() { return PlayerType.TCP; }

    @Override
    public void processInput(char c) throws InvalidInputException {
        if (isFinished()) return;
        if (startTime == null) startTime = LocalDateTime.now();
        typedText.append(c);
        correctChars++;
        totalAttempts++;
        if (isFinished()) endTime = LocalDateTime.now();
    }
}
