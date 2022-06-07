package com.gb.filestorage.filestorageclient;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import messages.AuthRegAnswer;
import messages.AuthRegRequest;

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

    @FXML
    public void btnTryReg(ActionEvent actionEvent) {
        sendRequestToServer(true);
    }

    private void sendRequestToServer(boolean isReg) {
        boolean wasSended = clientUI.SendAuthRegRequest(new AuthRegRequest(loginField.getText(), passwordField.getText(), isReg));

        if (wasSended) {
            //тут будет обработчик ожидания ответа от сервера - надо брокировать интерфейс
        }
    }

    @FXML
    public void btnTryLog(ActionEvent actionEvent) {
        sendRequestToServer(false);
    }

    public void getAnswerFromServer(AuthRegAnswer answer) {
        /*
        * тут будет снятие блокировки ожидания
         */

        if (answer.isResult()) {
            //ответ положительный
            clientUI.switchOnConnection();

            Platform.runLater(()->{
                Stage thisStage = (Stage) btnLogin.getScene().getWindow();
                thisStage.close();
            });

        } else {
            Platform.runLater(()->{
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Operation error");
                alert.setHeaderText("Failed to "
                        + (answer.isOperationReg() ? " register new user " : " sign in user ")
                        + " on server!");
                alert.setContentText(answer.getMessage());

                alert.showAndWait();
            });
        }
    }
}
