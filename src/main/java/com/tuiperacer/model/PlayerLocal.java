package com.tuiperacer.model;

import com.tuiperacer.exception.InvalidInputException;
import java.time.LocalDateTime;

public class PlayerLocal extends Player {

    public PlayerLocal(String name, String textToType) {
        super(name, textToType);
    }

    @Override
    public PlayerType getType() {
        return PlayerType.LOCAL;
    }

    @Override
    public void processInput(char c) throws InvalidInputException {
        if (isFinished()) return;

        // Set start time on first character
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }

        totalAttempts++;

        char expected = textToType.charAt(typedText.length());
        if (c != expected) {
            throw new InvalidInputException(expected, c);
        }

        typedText.append(c);
        correctChars++;

        if (isFinished()) {
            endTime = LocalDateTime.now();
        }
    }
}
