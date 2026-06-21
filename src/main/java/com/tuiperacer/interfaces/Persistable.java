package com.tuiperacer.interfaces;

import com.tuiperacer.exception.FileLoadException;

public interface Persistable {
    String serialize();
    void deserialize(String data) throws FileLoadException;
}
