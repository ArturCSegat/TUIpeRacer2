package com.tuiperacer.exception;

public class InvalidInputException extends RuntimeException {
    private final char expected;
    private final char received;

    public InvalidInputException(char expected, char received) {
        super("Invalid input: expected '" + expected + "' but received '" + received + "'");
        this.expected = expected;
        this.received = received;
    }

    public InvalidInputException(String message) {
        super(message);
        this.expected = '\0';
        this.received = '\0';
    }

    public char getExpected() { return expected; }
    public char getReceived() { return received; }
}
