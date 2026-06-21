package com.tuiperacer.view;

import com.tuiperacer.AppContext;
import com.tuiperacer.Main;
import com.tuiperacer.exception.InvalidInputException;
import com.tuiperacer.model.Player;
import com.tuiperacer.model.PlayerCpu;
import com.tuiperacer.model.PlayerLocal;
import com.tuiperacer.model.PlayerTcp;
import com.tuiperacer.model.PlayerType;
import com.tuiperacer.model.RaceResult;
import com.tuiperacer.network.RaceClient;
import com.tuiperacer.network.RaceServer;
import com.tuiperacer.service.RaceService;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RaceController {

    @FXML private VBox playersContainer;
    @FXML private Label timerLabel;

    private RaceService raceService;
    private AnimationTimer gameLoop;
    private final Map<String, PlayerPane> playerPanes = new HashMap<>();
    private long startNanos;
    private boolean finished = false;

    private RaceServer raceServer;
    private RaceClient raceClient;
    private final ConcurrentLinkedQueue<RemoteChar> remoteCharQueue = new ConcurrentLinkedQueue<>();

    private record RemoteChar(int playerId, char c) {}

    @FXML
    public void initialize() {
        raceService = AppContext.getInstance().getRaceService();
        raceServer = AppContext.getInstance().getRaceServer();
        raceClient = AppContext.getInstance().getRaceClient();
        startNanos = System.nanoTime();

        List<Player> players = raceService.getPlayers();
        for (Player p : players) {
            PlayerPane pane = new PlayerPane(p);
            playerPanes.put(p.getName(), pane);
            playersContainer.getChildren().add(pane.getRoot());
        }

        // Wire up network callbacks
        if (raceServer != null) {
            raceServer.setOnCharReceived((id, c) -> remoteCharQueue.add(new RemoteChar(id, c)));
        }
        if (raceClient != null) {
            raceClient.setOnCharReceived((id, c) -> remoteCharQueue.add(new RemoteChar(id, c)));
            raceClient.setOnError(err -> finishRace());
        }

        Platform.runLater(() -> {
            javafx.scene.Scene scene = playersContainer.getScene();
            if (scene == null) return;

            scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (!finished) {
                    KeyCode code = e.getCode();
                    if (code == KeyCode.SPACE || code == KeyCode.ENTER) e.consume();
                }
            });

            scene.addEventHandler(KeyEvent.KEY_TYPED, this::onKeyTyped);

            playersContainer.setFocusTraversable(true);
            playersContainer.requestFocus();
        });

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                onGameTick(now);
            }
        };
        gameLoop.start();
    }

    private void onKeyTyped(KeyEvent event) {
        if (finished) return;
        String ch = event.getCharacter();
        if (ch == null || ch.isEmpty()) return;

        char c = ch.charAt(0);
        if (c < 32) return;

        for (Player p : raceService.getPlayers()) {
            if (p.getType() == PlayerType.LOCAL) {
                PlayerLocal local = (PlayerLocal) p;
                try {
                    local.processInput(c);
                    PlayerPane pane = playerPanes.get(p.getName());
                    if (pane != null) pane.update();

                    // Broadcast typed char to other players via network
                    if (raceServer != null) raceServer.broadcastHostChar(c);
                    else if (raceClient != null) raceClient.sendChar(c);

                } catch (InvalidInputException e) {
                    PlayerPane pane = playerPanes.get(p.getName());
                    if (pane != null) pane.flashError();
                }
            }
        }
    }

    private void onGameTick(long now) {
        if (finished) return;

        long elapsedNanos = now - startNanos;
        long elapsedSecs = elapsedNanos / 1_000_000_000L;
        timerLabel.setText(String.format("%02d:%02d", elapsedSecs / 60, elapsedSecs % 60));

        long nowMs = System.currentTimeMillis();

        // Drain remote chars received over the network
        RemoteChar rc;
        while ((rc = remoteCharQueue.poll()) != null) {
            final RemoteChar rcFinal = rc;
            for (Player p : raceService.getPlayers()) {
                if (p.getType() == PlayerType.TCP) {
                    PlayerTcp tcp = (PlayerTcp) p;
                    if (tcp.getPlayerId() == rcFinal.playerId()) {
                        try {
                            tcp.processInput(rcFinal.c());
                            PlayerPane pane = playerPanes.get(p.getName());
                            if (pane != null) pane.update();
                        } catch (InvalidInputException ignored) {}
                        break;
                    }
                }
            }
        }

        // Tick CPU players
        for (Player p : raceService.getPlayers()) {
            if (p.getType() == PlayerType.CPU) {
                PlayerCpu cpu = (PlayerCpu) p;
                boolean typed = cpu.autoTick(nowMs);
                if (typed) {
                    PlayerPane pane = playerPanes.get(p.getName());
                    if (pane != null) pane.update();
                }
            }
            PlayerPane pane = playerPanes.get(p.getName());
            if (pane != null) pane.updateStats();
        }

        if (raceService.getCurrentRace().isAllFinished()) {
            finishRace();
        }
    }

    @FXML
    public void onGiveUp() {
        finishRace();
    }

    private void finishRace() {
        if (finished) return;
        finished = true;
        if (gameLoop != null) gameLoop.stop();

        RaceResult result = raceService.finishRace();
        AppContext.getInstance().setLastResult(result);
        Platform.runLater(() -> Main.navigateTo("results"));
    }

    // ---- Inner class: PlayerPane ----

    private static class PlayerPane {
        private final Player player;
        private final VBox root;
        private final TextFlow textFlow;
        private final ProgressBar progressBar;
        private final Label wpmLabel;
        private final Label accuracyLabel;
        private final Label nameLabel;
        private boolean errorFlash = false;

        PlayerPane(Player player) {
            this.player = player;
            root = new VBox(6);
            root.getStyleClass().add("player-card");

            nameLabel = new Label(player.getName() + " [" + player.getType() + "]");
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");

            textFlow = new TextFlow();
            textFlow.setMaxWidth(Double.MAX_VALUE);

            progressBar = new ProgressBar(0);
            progressBar.setMaxWidth(Double.MAX_VALUE);

            wpmLabel = new Label("WPM: 0.0");
            accuracyLabel = new Label("Precisão: 100.0%");

            VBox statsRow = new VBox(2, wpmLabel, accuracyLabel);

            root.getChildren().addAll(nameLabel, textFlow, progressBar, statsRow);
            update();
        }

        void update() {
            String text = player.getTextToType();
            String typed = player.getTypedText();
            int typedLen = typed.length();

            textFlow.getChildren().clear();

            if (typedLen > 0) {
                Text typedText = new Text(text.substring(0, typedLen));
                typedText.getStyleClass().add(errorFlash ? "race-error" : "race-typed");
                textFlow.getChildren().add(typedText);
            }

            if (typedLen < text.length()) {
                Text cursor = new Text(String.valueOf(text.charAt(typedLen)));
                cursor.getStyleClass().add("race-cursor");
                textFlow.getChildren().add(cursor);

                if (typedLen + 1 < text.length()) {
                    Text remaining = new Text(text.substring(typedLen + 1));
                    remaining.getStyleClass().add("race-remaining");
                    textFlow.getChildren().add(remaining);
                }
            }

            progressBar.setProgress(player.getProgress());
            errorFlash = false;
        }

        void updateStats() {
            wpmLabel.setText(String.format("WPM: %.1f", player.getWPM()));
            accuracyLabel.setText(String.format("Precisão: %.1f%%", player.getAccuracy()));
            progressBar.setProgress(player.getProgress());
        }

        void flashError() {
            errorFlash = true;
            update();
        }

        VBox getRoot() { return root; }
    }
}
