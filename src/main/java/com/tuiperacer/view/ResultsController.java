package com.tuiperacer.view;

import com.tuiperacer.AppContext;
import com.tuiperacer.Main;
import com.tuiperacer.exception.FileLoadException;
import com.tuiperacer.model.PlayerResult;
import com.tuiperacer.model.RaceResult;
import com.tuiperacer.persistence.RaceResultManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import java.util.ArrayList;
import java.util.List;

public class ResultsController {

    @FXML private TableView<PlayerResult> resultsTable;
    @FXML private TableColumn<PlayerResult, String> colRank;
    @FXML private TableColumn<PlayerResult, String> colName;
    @FXML private TableColumn<PlayerResult, String> colType;
    @FXML private TableColumn<PlayerResult, String> colWpm;
    @FXML private TableColumn<PlayerResult, String> colAccuracy;
    @FXML private TableColumn<PlayerResult, String> colFinished;
    @FXML private TextField savePathField;

    private RaceResultManager resultManager;

    @FXML
    public void initialize() {
        resultManager = AppContext.getInstance().getRaceResultManager();

        colRank.setCellValueFactory(cellData -> {
            int idx = resultsTable.getItems().indexOf(cellData.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(idx));
        });

        colName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPlayerName()));

        colType.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPlayerType().name()));

        colWpm.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.1f", data.getValue().getWpm())));

        colAccuracy.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.1f%%", data.getValue().getAccuracy())));

        colFinished.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isFinished() ? "Sim" : "Não"));

        RaceResult lastResult = AppContext.getInstance().getLastResult();
        if (lastResult != null) {
            populateTable(lastResult.getResults());
        } else {
            List<RaceResult> allResults = resultManager.getResults();
            if (!allResults.isEmpty()) {
                populateTable(allResults.get(allResults.size() - 1).getResults());
            }
        }
    }

    @FXML
    public void onSave() {
        String path = savePathField.getText().trim();
        if (path.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Caminho vazio", "Por favor insira um caminho de arquivo para salvar.");
            return;
        }
        RaceResult last = AppContext.getInstance().getLastResult();
        if (last == null) {
            showAlert(Alert.AlertType.WARNING, "Sem resultado", "Não há resultado para salvar.");
            return;
        }
        try {
            resultManager.saveResult(path, last);
            showAlert(Alert.AlertType.INFORMATION, "Salvo", "Resultado salvo em: " + path);
        } catch (FileLoadException e) {
            showAlert(Alert.AlertType.ERROR, "Erro ao salvar", e.getMessage());
        }
    }

    @FXML
    public void onBack() {
        Main.navigateTo("main_menu");
    }

    private void populateTable(List<PlayerResult> playerResults) {
        List<PlayerResult> sorted = new ArrayList<>(playerResults);
        sorted.sort((a, b) -> {
            if (a.isFinished() && !b.isFinished()) return -1;
            if (!a.isFinished() && b.isFinished()) return 1;
            return Double.compare(b.getWpm(), a.getWpm());
        });
        resultsTable.getItems().clear();
        resultsTable.getItems().addAll(sorted);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
