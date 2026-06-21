package com.tuiperacer.model;

import com.tuiperacer.exception.FileLoadException;
import com.tuiperacer.interfaces.Persistable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class RaceResult implements Persistable {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private String text;
    private LocalDateTime date;
    private List<PlayerResult> results;

    public RaceResult() {
        this.results = new ArrayList<>();
        this.date = LocalDateTime.now();
    }

    public RaceResult(String text, LocalDateTime date, List<PlayerResult> results) {
        this.text = text;
        this.date = date;
        this.results = new ArrayList<>(results);
    }

    @Override
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("DATE:").append(date.format(FORMATTER)).append("\n");
        sb.append("TEXT:").append(text.replace("\n", "\\n")).append("\n");
        for (PlayerResult pr : results) {
            sb.append("PLAYER:").append(pr.serialize()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public void deserialize(String data) throws FileLoadException {
        try {
            results = new ArrayList<>();
            String[] lines = data.split("\n");
            for (String line : lines) {
                if (line.startsWith("DATE:")) {
                    String dateStr = line.substring(5).trim();
                    try {
                        this.date = LocalDateTime.parse(dateStr, FORMATTER);
                    } catch (DateTimeParseException e) {
                        throw new FileLoadException("race result", "Invalid date format: " + dateStr);
                    }
                } else if (line.startsWith("TEXT:")) {
                    this.text = line.substring(5).replace("\\n", "\n");
                } else if (line.startsWith("PLAYER:")) {
                    String playerData = line.substring(7);
                    try {
                        results.add(PlayerResult.deserialize(playerData));
                    } catch (IllegalArgumentException e) {
                        throw new FileLoadException("race result", "Invalid player data: " + e.getMessage());
                    }
                }
            }
            if (text == null) {
                throw new FileLoadException("race result", "Missing TEXT field");
            }
        } catch (FileLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new FileLoadException("race result", "Parse error: " + e.getMessage());
        }
    }

    // Getters and setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public List<PlayerResult> getResults() { return results; }
    public void setResults(List<PlayerResult> results) { this.results = results; }
}
