package com.tuiperacer.view;

import com.tuiperacer.AppContext;
import com.tuiperacer.Main;
import com.tuiperacer.exception.ConnectionException;
import com.tuiperacer.exception.DuplicatePlayerException;
import com.tuiperacer.exception.FileLoadException;
import com.tuiperacer.exception.InvalidTextException;
import com.tuiperacer.exception.RaceAlreadyStartedException;
import com.tuiperacer.model.PlayerLocal;
import com.tuiperacer.model.PlayerTcp;
import com.tuiperacer.network.RaceClient;
import com.tuiperacer.network.RaceServer;
import com.tuiperacer.persistence.TextFileLoader;
import com.tuiperacer.service.RaceService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class OnlineLobbyController {

    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private VBox hostPanel;
    @FXML private TextField hostNameField;
    @FXML private Label hostInfoLabel;
    @FXML private Label playerCountLabel;
    @FXML private ListView<String> lobbyListView;
    @FXML private VBox joinPanel;
    @FXML private TextField joinHostField;
    @FXML private TextField joinPortField;
    @FXML private TextField joinNameField;
    @FXML private VBox waitingPanel;
    @FXML private ListView<String> waitingPlayerListView;

    private static boolean hostMode = true;

    private RaceServer server;
    private RaceClient client;
    private RaceService raceService;

    // Join mode state: collected from JOIN messages before START
    private String myName = "Player";
    private int myId = -1;
    private String raceText;
    private final Map<Integer, String> joinedPlayers = new HashMap<>();

    public static void setHostMode(boolean isHost) {
        hostMode = isHost;
    }

    @FXML
    public void initialize() {
        raceService = AppContext.getInstance().getRaceService();

        if (hostMode) {
            titleLabel.setText("Hospedar Corrida Online");
            hostPanel.setVisible(true);
            hostPanel.setManaged(true);
            joinPanel.setVisible(false);
            joinPanel.setManaged(false);
            waitingPanel.setVisible(false);
            waitingPanel.setManaged(false);
            startServer();
        } else {
            titleLabel.setText("Entrar em Corrida Online");
            hostPanel.setVisible(false);
            hostPanel.setManaged(false);
            joinPanel.setVisible(true);
            joinPanel.setManaged(true);
            waitingPanel.setVisible(false);
            waitingPanel.setManaged(false);
        }
    }

    private void startServer() {
        try {
            String text = TextFileLoader.loadBuiltinText(20);

            RaceServer started = null;
            int boundPort = -1;
            for (int port = 3000; port <= 3010; port++) {
                try {
                    RaceServer candidate = new RaceServer(port, text);
                    candidate.start();
                    started = candidate;
                    boundPort = port;
                    break;
                } catch (ConnectionException ignored) {}
            }

            if (started == null) {
                showAlert(Alert.AlertType.ERROR, "Erro de conexão",
                        "Não foi possível abrir nenhuma porta entre 3000 e 3010.");
                return;
            }

            server = started;
            final int finalPort = boundPort;
            server.setOnPlayerJoined(name -> {
                playerCountLabel.setText("Jogadores conectados: " + server.getClientCount());
                lobbyListView.getItems().add(name);
            });

            String ip;
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                ip = "127.0.0.1";
            }
            hostInfoLabel.setText("Servidor em: " + ip + ":" + finalPort);
            statusLabel.setText("Aguardando jogadores...");

        } catch (FileLoadException e) {
            showAlert(Alert.AlertType.ERROR, "Erro ao carregar texto", e.getMessage());
        }
    }

    @FXML
    public void onStartRace() {
        if (server == null || !server.isRunning()) {
            showAlert(Alert.AlertType.WARNING, "Servidor não iniciado", "Servidor não está rodando.");
            return;
        }

        String hostName = hostNameField.getText().trim();
        if (hostName.isEmpty()) hostName = "Host";
        String text = server.getText();

        RaceServer serverRef = this.server;

        try {
            AppContext.getInstance().resetForNewRace();
            raceService = AppContext.getInstance().getRaceService();
            raceService.createRace(text);

            PlayerLocal hostPlayer = new PlayerLocal(hostName, text);
            raceService.addPlayer(hostPlayer);

            for (Map.Entry<Integer, String> entry : serverRef.getPlayerNames().entrySet()) {
                if (entry.getKey() == 0) continue;
                PlayerTcp tcp = new PlayerTcp(entry.getValue(), text, entry.getKey());
                raceService.addPlayer(tcp);
            }

            raceService.startRace();
            serverRef.setHostName(hostName);
            serverRef.broadcastStart();
            AppContext.getInstance().setRaceServer(serverRef);
            Main.navigateTo("race");

        } catch (InvalidTextException | DuplicatePlayerException | RaceAlreadyStartedException e) {
            showAlert(Alert.AlertType.ERROR, "Erro ao iniciar", e.getMessage());
        }
    }

    @FXML
    public void onJoin() {
        String host = joinHostField.getText().trim();
        String portStr = joinPortField.getText().trim();
        myName = joinNameField.getText().trim();

        if (host.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Host vazio", "Insira o endereço do servidor.");
            return;
        }
        if (myName.isEmpty()) myName = "Player";

        int port;
        try {
            port = portStr.isEmpty() ? 3000 : Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Porta inválida", "Insira um número de porta válido.");
            return;
        }

        final String finalMyName = myName;

        client = new RaceClient(host, port);

        client.setOnTextReceived(text -> raceText = text);

        client.setOnMyIdReceived(id -> myId = id);

        client.setOnPlayerJoined((id, name) -> {
            joinedPlayers.put(id, name);
            refreshWaitingList();
        });

        client.setOnStartReceived(this::startRaceAsClient);

        client.setOnError(err -> showAlert(Alert.AlertType.ERROR, "Erro de conexão", err));

        try {
            client.connect();
            client.sendName(finalMyName);

            joinPanel.setVisible(false);
            joinPanel.setManaged(false);
            waitingPanel.setVisible(true);
            waitingPanel.setManaged(true);
            statusLabel.setText("Conectado! Aguardando o host iniciar...");

        } catch (ConnectionException e) {
            showAlert(Alert.AlertType.ERROR, "Falha ao conectar", e.getMessage());
        }
    }

    private void startRaceAsClient() {
        if (raceText == null || raceText.isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Erro", "Texto da corrida não recebido.");
            return;
        }

        RaceClient clientRef = this.client;

        try {
            AppContext.getInstance().resetForNewRace();
            raceService = AppContext.getInstance().getRaceService();
            raceService.createRace(raceText);

            PlayerLocal me = new PlayerLocal(myName, raceText);
            raceService.addPlayer(me);

            for (Map.Entry<Integer, String> entry : joinedPlayers.entrySet()) {
                if (entry.getKey() == myId) continue;
                PlayerTcp tcp = new PlayerTcp(entry.getValue(), raceText, entry.getKey());
                raceService.addPlayer(tcp);
            }

            raceService.startRace();
            AppContext.getInstance().setRaceClient(clientRef);
            Main.navigateTo("race");

        } catch (InvalidTextException | DuplicatePlayerException | RaceAlreadyStartedException e) {
            showAlert(Alert.AlertType.ERROR, "Erro ao iniciar", e.getMessage());
        }
    }

    private void refreshWaitingList() {
        waitingPlayerListView.getItems().clear();
        joinedPlayers.forEach((id, name) ->
                waitingPlayerListView.getItems().add(name + (id == 0 ? " (host)" : "")));
    }

    @FXML
    public void onBack() {
        if (server != null) { server.stop(); server = null; }
        if (client != null) { client.disconnect(); client = null; }
        Main.navigateTo("main_menu");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
