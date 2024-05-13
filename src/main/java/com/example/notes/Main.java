package com.example.notes;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException, SQLException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("Login.fxml"));
        Parent root = fxmlLoader.load();
        Controller controller = fxmlLoader.getController();

        if (controller.checkLoginState(primaryStage)) {
            return;
        }
        controller.openLoginWindow();
    }

    public static void main(String[] args) {
        launch();
    }
}