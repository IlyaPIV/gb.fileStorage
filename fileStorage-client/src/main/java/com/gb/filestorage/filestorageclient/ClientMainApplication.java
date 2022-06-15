package com.gb.filestorage.filestorageclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;


import java.io.IOException;

public class ClientMainApplication extends Application {


    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientMainApplication.class.getResource("clientMain-view.fxml"));


        Scene scene = new Scene(fxmlLoader.load(), 1200, 780);
        stage.setTitle("GeekBrains: File storage project");
        scene.getStylesheets().add(getClass().getResource("css/StyleMainWindow.css").toExternalForm());
        stage.setScene(scene);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}