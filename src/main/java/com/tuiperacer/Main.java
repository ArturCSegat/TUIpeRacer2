package com.tuiperacer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("TUIpeRacer 2");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        navigateTo("main_menu");
        primaryStage.show();
    }

    public static void navigateTo(String screenName) {
        try {
            URL fxmlUrl = Main.class.getResource("/com/tuiperacer/" + screenName + ".fxml");
            if (fxmlUrl == null) {
                System.err.println("FXML not found: " + screenName);
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root);

            URL cssUrl = Main.class.getResource("/com/tuiperacer/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Failed to load screen '" + screenName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) {
        launch(args);
    }
}
