package com.gb.filestorage.filestorageclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class ClienMainApplication extends Application {


    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClienMainApplication.class.getResource("clientMain-view.fxml"));



        Scene scene = new Scene(fxmlLoader.load(), 1000, 750);
        stage.setTitle("GeekBrains: File storage project");
        scene.getStylesheets().add(getClass().getResource("css/StyleMainWindow.css").toExternalForm());
        stage.setScene(scene);


        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}