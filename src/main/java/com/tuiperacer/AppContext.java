package com.tuiperacer;

import com.tuiperacer.model.RaceResult;
import com.tuiperacer.network.RaceClient;
import com.tuiperacer.network.RaceServer;
import com.tuiperacer.persistence.RaceResultManager;
import com.tuiperacer.service.LobbyService;
import com.tuiperacer.service.RaceService;

public class AppContext {

    private static final AppContext instance = new AppContext();

    private RaceService raceService;
    private final LobbyService lobbyService;
    private final RaceResultManager raceResultManager;
    private RaceResult lastResult;

    private RaceServer raceServer;
    private RaceClient raceClient;

    private AppContext() {
        this.raceService = new RaceService();
        this.lobbyService = new LobbyService();
        this.raceResultManager = new RaceResultManager();
    }

    public static AppContext getInstance() {
        return instance;
    }

    /** Resets race state and clears network handles for a new game session. */
    public void resetForNewRace() {
        if (raceServer != null) { raceServer.stop(); raceServer = null; }
        if (raceClient != null) { raceClient.disconnect(); raceClient = null; }
        raceService = new RaceService();
    }

    public RaceService getRaceService() { return raceService; }
    public LobbyService getLobbyService() { return lobbyService; }
    public RaceResultManager getRaceResultManager() { return raceResultManager; }

    public RaceResult getLastResult() { return lastResult; }
    public void setLastResult(RaceResult lastResult) {
        this.lastResult = lastResult;
        if (lastResult != null) raceResultManager.addResult(lastResult);
    }

    public RaceServer getRaceServer() { return raceServer; }
    public void setRaceServer(RaceServer raceServer) { this.raceServer = raceServer; }

    public RaceClient getRaceClient() { return raceClient; }
    public void setRaceClient(RaceClient raceClient) { this.raceClient = raceClient; }
}
