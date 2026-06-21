package com.tuiperacer.model;

import com.tuiperacer.exception.InvalidInputException;
import java.time.LocalDateTime;
import java.util.Random;

public class PlayerCpu extends Player {

    private final int targetWpm;
    private final double charsPerMs;
    private long lastCharTimeMs;
    private final Random random;

    public PlayerCpu(String name, String textToType, int targetWpm) {
        super(name, textToType);
        this.targetWpm = targetWpm;
        // chars per ms = (wpm * 5 chars/word) / (60000 ms/min)
        this.charsPerMs = (targetWpm * 5.0) / 60000.0;
        this.lastCharTimeMs = 0;
        this.random = new Random();
    }

    public int getTargetWpm() { return targetWpm; }

    @Override
    public PlayerType getType() {
        return PlayerType.CPU;
    }

    @Override
    public void processInput(char c) throws InvalidInputException {
        if (isFinished()) return;

        if (startTime == null) {
            startTime = LocalDateTime.now();
        }

        typedText.append(c);
        correctChars++;
        totalAttempts++;

        if (isFinished()) {
            endTime = LocalDateTime.now();
        }
    }

    /**
     * Auto-tick: called from the game loop with current time in ms.
     * Returns true if a character was typed this tick.
     */
    public boolean autoTick(long nowMs) {
        if (isFinished()) return false;

        if (startTime == null) {
            startTime = LocalDateTime.now();
            lastCharTimeMs = nowMs;
        }

        if (charsPerMs <= 0) return false;

        // Delay between chars with ±20% jitter
        double baseDelayMs = 1.0 / charsPerMs;
        double jitter = baseDelayMs * 0.2 * (random.nextDouble() * 2 - 1);
        double delayMs = baseDelayMs + jitter;

        long elapsed = nowMs - lastCharTimeMs;
        if (elapsed >= (long) delayMs) {
            int pos = typedText.length();
            if (pos < textToType.length()) {
                char nextChar = textToType.charAt(pos);
                try {
                    processInput(nextChar);
                } catch (InvalidInputException e) {
                    // CPU never makes mistakes
                }
                lastCharTimeMs = nowMs;
                return true;
            }
        }
        return false;
    }
}
