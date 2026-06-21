package com.tuiperacer.persistence;

import com.tuiperacer.exception.FileLoadException;
import com.tuiperacer.exception.InvalidTextException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TextFileLoader {

    private TextFileLoader() {}

    /**
     * Loads text from a file path.
     * Throws FileLoadException if the file cannot be read.
     * Throws InvalidTextException if the file is empty.
     */
    public static String loadFromFile(String path) throws FileLoadException, InvalidTextException {
        try {
            String content = Files.readString(Path.of(path));
            if (content == null || content.isBlank()) {
                throw new InvalidTextException("File is empty: " + path);
            }
            return content.trim();
        } catch (InvalidTextException e) {
            throw e;
        } catch (Exception e) {
            throw new FileLoadException(path, e.getMessage());
        }
    }

    /**
     * Loads a random sample of words from the built-in ptbr.txt resource.
     * Throws FileLoadException if the resource cannot be read.
     */
    public static String loadBuiltinText(int wordCount) throws FileLoadException {
        try {
            InputStream is = TextFileLoader.class.getResourceAsStream("/words/en.txt");
            if (is == null) {
                throw new FileLoadException("/words/en.txt", "Built-in word list resource not found");
            }
            List<String> words = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String w = line.trim();
                    if (!w.isEmpty()) {
                        words.add(w);
                    }
                }
            }
            if (words.isEmpty()) {
                throw new FileLoadException("/words/en.txt", "Word list is empty");
            }
            Collections.shuffle(words, new Random());
            int count = Math.min(wordCount, words.size());
            return String.join(" ", words.subList(0, count));
        } catch (FileLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new FileLoadException("/words/en.txt", e.getMessage());
        }
    }
}
