package com.tuiperacer.model;

import com.tuiperacer.exception.InvalidInputException;
import com.tuiperacer.interfaces.Typeable;
import java.time.LocalDateTime;

public abstract class Player implements Typeable {

    protected String name;
    protected final String textToType;
    protected StringBuilder typedText;
    protected int correctChars;
    protected int totalAttempts;
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;

    protected Player(String name, String textToType) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Player name cannot be blank");
        }
        if (textToType == null || textToType.isBlank()) {
            throw new InvalidInputException("Text to type cannot be blank");
        }
        this.name = name;
        this.textToType = textToType;
        this.typedText = new StringBuilder();
        this.correctChars = 0;
        this.totalAttempts = 0;
        this.startTime = null;
        this.endTime = null;
    }

    // Concrete methods
    public String getName() { return name; }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Player name cannot be blank");
        }
        this.name = name;
    }

    public String getTextToType() { return textToType; }

    public String getTypedText() { return typedText.toString(); }

    public char getExpectedChar() {
        int pos = typedText.length();
        if (pos >= textToType.length()) return 0;
        return textToType.charAt(pos);
    }

    @Override
    public boolean isFinished() {
        return typedText.length() >= textToType.length();
    }

    @Override
    public double getProgress() {
        if (textToType.isEmpty()) return 0.0;
        return (double) typedText.length() / textToType.length();
    }

    @Override
    public double getWPM() {
        if (startTime == null) return 0.0;
        LocalDateTime end = (endTime != null) ? endTime : LocalDateTime.now();
        long millis = java.time.Duration.between(startTime, end).toMillis();
        if (millis <= 0) return 0.0;
        double minutes = millis / 60000.0;
        return (correctChars / 5.0) / minutes;
    }

    @Override
    public double getAccuracy() {
        if (totalAttempts == 0) return 100.0;
        return (correctChars * 100.0) / totalAttempts;
    }

    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }

    // Abstract methods
    public abstract PlayerType getType();

    @Override
    public abstract void processInput(char c) throws InvalidInputException;

    // Concrete tick method (no-op, overridden by CPU)
    public void tick() {}

    @Override
    public String toString() {
        return String.format("[%s] %s  WPM=%.1f  ACC=%.1f%%  %d%%",
                getType(), name, getWPM(), getAccuracy(), (int)(getProgress() * 100));
    }
}
