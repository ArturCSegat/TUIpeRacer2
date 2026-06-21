package com.tuiperacer.service;

import com.tuiperacer.exception.PlayerNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class LobbyService {

    private final List<LobbyEntry> entries;

    public LobbyService() {
        this.entries = new ArrayList<>();
    }

    /**
     * Upserts a lobby entry by host+port.
     */
    public void addEntry(LobbyEntry entry) {
        for (int i = 0; i < entries.size(); i++) {
            LobbyEntry existing = entries.get(i);
            if (existing.getHost().equals(entry.getHost()) &&
                existing.getPort() == entry.getPort()) {
                entries.set(i, entry);
                return;
            }
        }
        entries.add(entry);
    }

    public void removeEntry(String host, int port) throws PlayerNotFoundException {
        LobbyEntry found = null;
        for (LobbyEntry entry : entries) {
            if (entry.getHost().equals(host) && entry.getPort() == port) {
                found = entry;
                break;
            }
        }
        if (found == null) {
            throw new PlayerNotFoundException(host + ":" + port);
        }
        entries.remove(found);
    }

    public List<LobbyEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    public void clear() {
        entries.clear();
    }
}
