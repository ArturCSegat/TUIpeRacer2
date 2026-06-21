package com.tuiperacer.service;

import com.tuiperacer.exception.DuplicatePlayerException;
import com.tuiperacer.exception.InvalidTextException;
import com.tuiperacer.exception.PlayerNotFoundException;
import com.tuiperacer.exception.RaceAlreadyStartedException;
import com.tuiperacer.model.Player;
import com.tuiperacer.model.Race;
import com.tuiperacer.model.RaceResult;
import com.tuiperacer.model.RaceState;
import java.util.List;

public class RaceService {

    private Race currentRace;

    public RaceService() {
        this.currentRace = new Race();
    }

    public void createRace(String text) throws InvalidTextException {
        if (text == null || text.isBlank()) {
            throw new InvalidTextException("Race text cannot be empty.");
        }
        this.currentRace = new Race(text);
    }

    public void addPlayer(Player p) throws RaceAlreadyStartedException, DuplicatePlayerException {
        currentRace.addPlayer(p);
    }

    public void removePlayer(String name) throws PlayerNotFoundException {
        currentRace.removePlayer(name);
    }

    public Player findPlayer(String name) throws PlayerNotFoundException {
        return currentRace.findPlayer(name);
    }

    /**
     * Edits the player's name.
     * Throws PlayerNotFoundException if old name not found.
     * Throws DuplicatePlayerException if new name already exists.
     */
    public void editPlayerName(String oldName, String newName)
            throws PlayerNotFoundException, DuplicatePlayerException {
        // Check new name is not already taken
        try {
            Player existing = currentRace.findPlayer(newName);
            // If we found a player with newName and it's not the same player
            if (!existing.getName().equalsIgnoreCase(oldName)) {
                throw new DuplicatePlayerException(newName);
            }
        } catch (PlayerNotFoundException e) {
            // new name is free — that's fine
        }

        Player player = currentRace.findPlayer(oldName);
        player.setName(newName);
    }

    public void startRace() throws InvalidTextException, RaceAlreadyStartedException {
        if (currentRace.getState() == RaceState.RUNNING) {
            throw new RaceAlreadyStartedException("Race is already running.");
        }
        currentRace.start();
    }

    public RaceResult finishRace() {
        currentRace.finish();
        return currentRace.buildResult();
    }

    public Race getCurrentRace() { return currentRace; }

    public List<Player> getPlayers() { return currentRace.getPlayers(); }

    /**
     * Resets the service with a fresh race.
     */
    public void reset() {
        this.currentRace = new Race();
    }
}
