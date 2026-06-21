package com.tuiperacer.model;

import com.tuiperacer.exception.DuplicatePlayerException;
import com.tuiperacer.exception.InvalidTextException;
import com.tuiperacer.exception.PlayerNotFoundException;
import com.tuiperacer.exception.RaceAlreadyStartedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Race {

    private final String id;
    private String text;
    private final List<Player> players;   // polymorphic collection
    private RaceState state;
    private LocalDateTime startTime;

    public Race() {
        this.id = UUID.randomUUID().toString();
        this.players = new ArrayList<>();
        this.state = RaceState.WAITING;
    }

    public Race(String text) {
        this();
        this.text = text;
    }

    public void addPlayer(Player p) throws RaceAlreadyStartedException, DuplicatePlayerException {
        if (state == RaceState.RUNNING) {
            throw new RaceAlreadyStartedException();
        }
        for (Player existing : players) {
            if (existing.getName().equalsIgnoreCase(p.getName())) {
                throw new DuplicatePlayerException(p.getName());
            }
        }
        players.add(p);
    }

    public void removePlayer(String name) throws PlayerNotFoundException {
        Player found = findPlayer(name);
        players.remove(found);
    }

    public Player findPlayer(String name) throws PlayerNotFoundException {
        for (Player p : players) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        throw new PlayerNotFoundException(name);
    }

    public void start() throws InvalidTextException {
        if (text == null || text.isBlank()) {
            throw new InvalidTextException("Race text cannot be empty. Please set a text before starting.");
        }
        this.state = RaceState.RUNNING;
        this.startTime = LocalDateTime.now();
    }

    public void finish() {
        this.state = RaceState.FINISHED;
    }

    public boolean isAllFinished() {
        if (players.isEmpty()) return false;
        for (Player p : players) {
            if (!p.isFinished()) return false;
        }
        return true;
    }

    public RaceResult buildResult() {
        List<PlayerResult> results = new ArrayList<>();
        for (Player p : players) {
            results.add(PlayerResult.fromPlayer(p));
        }
        return new RaceResult(text, startTime != null ? startTime : LocalDateTime.now(), results);
    }

    // Getters and setters
    public String getId() { return id; }

    public String getText() { return text; }
    public void setText(String text) {
        if (text != null && !text.isBlank()) this.text = text;
    }

    public List<Player> getPlayers() { return players; }

    public RaceState getState() { return state; }

    public LocalDateTime getStartTime() { return startTime; }
}
