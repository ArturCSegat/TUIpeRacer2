package com.tuiperacer.interfaces;

import com.tuiperacer.exception.InvalidInputException;

public interface Typeable {
    void processInput(char c) throws InvalidInputException;
    boolean isFinished();
    double getProgress();
    double getWPM();
    double getAccuracy();
}
