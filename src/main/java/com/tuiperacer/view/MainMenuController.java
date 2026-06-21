package com.tuiperacer.view;

import com.tuiperacer.AppContext;
import com.tuiperacer.Main;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class MainMenuController {

    @FXML
    public void onLocalRace() {
        // Reset race service for a fresh setup
        AppContext.getInstance().getRaceService().reset();
        Main.navigateTo("setup");
    }

    @FXML
    public void onHostOnline() {
        AppContext.getInstance().getRaceService().reset();
        OnlineLobbyController.setHostMode(true);
        Main.navigateTo("online_lobby");
    }

    @FXML
    public void onJoinOnline() {
        OnlineLobbyController.setHostMode(false);
        Main.navigateTo("online_lobby");
    }

    @FXML
    public void onResults() {
        Main.navigateTo("results");
    }

    @FXML
    public void onExit() {
        Stage stage = Main.getPrimaryStage();
        if (stage != null) {
            stage.close();
        }
    }
}
