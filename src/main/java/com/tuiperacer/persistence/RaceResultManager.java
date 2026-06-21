package com.tuiperacer.persistence;

import com.tuiperacer.exception.FileLoadException;
import com.tuiperacer.interfaces.Persistable;
import com.tuiperacer.model.PlayerResult;
import com.tuiperacer.model.PlayerType;
import com.tuiperacer.model.RaceResult;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RaceResultManager implements Persistable {

    private static final String SECTION_BEGIN = "---BEGIN---";
    private static final String SECTION_END = "---END---";

    private final List<RaceResult> results;

    public RaceResultManager() {
        this.results = new ArrayList<>();
    }

    public void addResult(RaceResult result) {
        results.add(result);
    }

    public List<RaceResult> getResults() {
        return new ArrayList<>(results);
    }

    /**
     * Saves a single race result to a human-readable file.
     */
    public void saveResult(String path, RaceResult result) throws FileLoadException {
        if (path == null || path.isBlank()) {
            throw new FileLoadException("(empty)", "File path cannot be empty");
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, java.nio.charset.StandardCharsets.UTF_8))) {
            writer.write(formatResult(result));
        } catch (IOException e) {
            throw new FileLoadException(path, "Write error: " + e.getMessage());
        }
    }

    private String formatResult(RaceResult result) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String date = result.getDate() != null ? result.getDate().format(fmt) : "-";

        String rawText = result.getText() != null ? result.getText() : "";
        String textDisplay = rawText.length() > 55 ? rawText.substring(0, 52) + "..." : rawText;

        String line = "─".repeat(52);
        StringBuilder sb = new StringBuilder();

        sb.append("  TUIpeRacer2 — Resultado da Corrida\n");
        sb.append(line).append("\n");
        sb.append(String.format("  Data  : %s%n", date));
        sb.append(String.format("  Texto : \"%s\"%n", textDisplay));
        sb.append(line).append("\n");
        sb.append(String.format("  %-2s  %-18s  %6s  %8s  %s%n",
                "#", "Jogador", "WPM", "Precisão", ""));
        sb.append(line).append("\n");

        List<PlayerResult> players = result.getResults();
        for (int i = 0; i < players.size(); i++) {
            PlayerResult pr = players.get(i);
            String name = pr.getPlayerName();
            if (pr.getPlayerType() != PlayerType.LOCAL) {
                name += " [" + pr.getPlayerType().name() + "]";
            }
            if (name.length() > 18) name = name.substring(0, 17) + "…";
            String status = pr.isFinished() ? "✓" : "✗";
            sb.append(String.format("  %-2d  %-18s  %6.1f  %7.1f%%  %s%n",
                    i + 1, name, pr.getWpm(), pr.getAccuracy(), status));
        }

        sb.append(line).append("\n");
        return sb.toString();
    }

    /**
     * Saves all accumulated results to a file.
     */
    public void saveToFile(String path) throws FileLoadException {
        if (path == null || path.isBlank()) {
            throw new FileLoadException("(empty)", "File path cannot be empty");
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(serialize());
        } catch (IOException e) {
            throw new FileLoadException(path, "Write error: " + e.getMessage());
        }
    }

    /**
     * Loads results from a file, replacing current results.
     */
    public void loadFromFile(String path) throws FileLoadException {
        if (path == null || path.isBlank()) {
            throw new FileLoadException("(empty)", "File path cannot be empty");
        }
        try {
            String content = Files.readString(Path.of(path));
            deserialize(content);
        } catch (FileLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new FileLoadException(path, "Read error: " + e.getMessage());
        }
    }

    @Override
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (RaceResult result : results) {
            sb.append(SECTION_BEGIN).append("\n");
            sb.append(result.serialize());
            sb.append(SECTION_END).append("\n");
        }
        return sb.toString();
    }

    @Override
    public void deserialize(String data) throws FileLoadException {
        results.clear();
        if (data == null || data.isBlank()) return;

        String[] sections = data.split(SECTION_BEGIN);
        for (String section : sections) {
            String trimmed = section.trim();
            if (trimmed.isEmpty()) continue;
            int endIdx = trimmed.indexOf(SECTION_END);
            if (endIdx < 0) {
                throw new FileLoadException("results file", "Malformed section: missing " + SECTION_END);
            }
            String sectionContent = trimmed.substring(0, endIdx).trim();
            if (sectionContent.isEmpty()) continue;

            RaceResult raceResult = new RaceResult();
            raceResult.deserialize(sectionContent);
            results.add(raceResult);
        }
    }
}
