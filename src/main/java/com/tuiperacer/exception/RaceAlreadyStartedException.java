package com.tuiperacer.exception;

public class RaceAlreadyStartedException extends RuntimeException {
    public RaceAlreadyStartedException() {
        super("Race has already started. Cannot modify players.");
    }

    public RaceAlreadyStartedException(String message) {
        super(message);
    }
}
