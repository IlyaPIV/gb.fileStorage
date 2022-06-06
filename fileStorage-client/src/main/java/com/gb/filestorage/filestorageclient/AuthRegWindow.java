package com.gb.filestorage.filestorageclient;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AuthRegWindow {

    private ClientMainController clientUI;

    @FXML
    public PasswordField passwordField;
    @FXML
    public Button btnRegistration;
    @FXML
    public Button btnLogin;
    @FXML
    public TextField loginField;




    public void setMainController(ClientMainController clientMainController) {
        this.clientUI = clientMainController;
    }
}
