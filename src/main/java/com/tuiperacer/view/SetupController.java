package com.tuiperacer.view;

import com.tuiperacer.AppContext;
import com.tuiperacer.Main;
import com.tuiperacer.exception.DuplicatePlayerException;
import com.tuiperacer.exception.FileLoadException;
import com.tuiperacer.exception.InvalidTextException;
import com.tuiperacer.exception.PlayerNotFoundException;
import com.tuiperacer.exception.RaceAlreadyStartedException;
import com.tuiperacer.model.Player;
import com.tuiperacer.model.PlayerCpu;
import com.tuiperacer.model.PlayerLocal;
import com.tuiperacer.model.PlayerType;
import com.tuiperacer.persistence.TextFileLoader;
import com.tuiperacer.service.RaceService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;
import java.util.Optional;

public class SetupController {

    @FXML private TextField playerNameField;
    @FXML private TextField cpuWpmField;
    @FXML private ChoiceBox<String> playerTypeChoice;
    @FXML private ListView<String> playerListView;
    @FXML private TextField searchField;
    @FXML private TextArea textArea;

    private RaceService raceService;

    @FXML
    public void initialize() {
        raceService = AppContext.getInstance().getRaceService();

        playerTypeChoice.getItems().addAll("LOCAL", "CPU");
        playerTypeChoice.setValue("LOCAL");

        // Load random text on setup
        try {
            String text = TextFileLoader.loadBuiltinText(20);
            textArea.setText(text);
            raceService.getCurrentRace().setText(text);
        } catch (FileLoadException e) {
            textArea.setText("The quick brown fox jumps over the lazy dog");
            raceService.getCurrentRace().setText(textArea.getText());
        }

        refreshPlayerList();
    }

    @FXML
    public void onAddPlayer() {
        String name = playerNameField.getText().trim();
        if (name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Campo vazio", "Por favor insira um nome de jogador.");
            return;
        }

        String text = textArea.getText().trim();
        if (text.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Texto vazio", "Por favor defina um texto para a corrida.");
            return;
        }
        raceService.getCurrentRace().setText(text);

        try {
            Player player;
            String type = playerTypeChoice.getValue();
            if ("CPU".equals(type)) {
                int wpm = 60;
                try {
                    String wpmText = cpuWpmField.getText().trim();
                    if (!wpmText.isEmpty()) {
                        wpm = Integer.parseInt(wpmText);
                    }
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.WARNING, "WPM inválido", "Use um número inteiro para WPM da CPU.");
                    return;
                }
                player = new PlayerCpu(name, text, wpm);
            } else {
                player = new PlayerLocal(name, text);
            }

            raceService.addPlayer(player);
            playerNameField.clear();
            refreshPlayerList();

        } catch (DuplicatePlayerException e) {
            showAlert(Alert.AlertType.ERROR, "Jogador duplicado", e.getMessage());
        } catch (RaceAlreadyStartedException e) {
            showAlert(Alert.AlertType.ERROR, "Corrida já iniciada", e.getMessage());
        }
    }

    @FXML
    public void onRemovePlayer() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Campo vazio", "Digite o nome do jogador a remover.");
            return;
        }
        try {
            raceService.removePlayer(keyword);
            refreshPlayerList();
        } catch (PlayerNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "Jogador não encontrado", e.getMessage());
        }
    }

    @FXML
    public void onEditPlayer() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Campo vazio", "Digite o nome do jogador a editar.");
            return;
        }

        try {
            // Verify player exists before opening dialog
            raceService.findPlayer(keyword);

            TextInputDialog dialog = new TextInputDialog(keyword);
            dialog.setTitle("Editar Jogador");
            dialog.setHeaderText("Renomear jogador: " + keyword);
            dialog.setContentText("Novo nome:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String newName = result.get().trim();
                if (!newName.isEmpty()) {
                    raceService.editPlayerName(keyword, newName);
                    refreshPlayerList();
                }
            }
        } catch (PlayerNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "Jogador não encontrado", e.getMessage());
        } catch (DuplicatePlayerException e) {
            showAlert(Alert.AlertType.ERROR, "Nome duplicado", e.getMessage());
        }
    }

    @FXML
    public void onRandomText() {
        try {
            String text = TextFileLoader.loadBuiltinText(20);
            textArea.setText(text);
            raceService.getCurrentRace().setText(text);
        } catch (FileLoadException e) {
            showAlert(Alert.AlertType.ERROR, "Erro ao carregar texto", e.getMessage());
        }
    }

    @FXML
    public void onLoadFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Carregar texto de arquivo");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Arquivos de texto", "*.txt"),
                new FileChooser.ExtensionFilter("Todos os arquivos", "*.*")
        );
        Window window = textArea.getScene() != null ? textArea.getScene().getWindow() : null;
        File file = chooser.showOpenDialog(window);
        if (file != null) {
            try {
                String text = TextFileLoader.loadFromFile(file.getAbsolutePath());
                textArea.setText(text);
                raceService.getCurrentRace().setText(text);
            } catch (FileLoadException e) {
                showAlert(Alert.AlertType.ERROR, "Erro ao carregar arquivo", e.getMessage());
            } catch (InvalidTextException e) {
                showAlert(Alert.AlertType.WARNING, "Arquivo vazio", e.getMessage());
            }
        }
    }

    @FXML
    public void onStartRace() {
        String text = textArea.getText().trim();
        if (text.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Texto vazio", "Defina um texto para iniciar a corrida.");
            return;
        }

        if (raceService.getPlayers().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Sem jogadores", "Adicione pelo menos um jogador antes de iniciar.");
            return;
        }

        raceService.getCurrentRace().setText(text);

        try {
            raceService.startRace();
            Main.navigateTo("race");
        } catch (InvalidTextException e) {
            showAlert(Alert.AlertType.ERROR, "Texto inválido", e.getMessage());
        } catch (RaceAlreadyStartedException e) {
            showAlert(Alert.AlertType.ERROR, "Corrida já iniciada", e.getMessage());
        }
    }

    @FXML
    public void onBack() {
        Main.navigateTo("main_menu");
    }

    private void refreshPlayerList() {
        playerListView.getItems().clear();
        for (Player p : raceService.getPlayers()) {
            playerListView.getItems().add(p.toString());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
