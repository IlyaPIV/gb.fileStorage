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

    private Thread waitingThread;


    public void setMainController(ClientMainController clientMainController) {
        this.clientUI = clientMainController;
    }

    @FXML
    public void btnTryReg(ActionEvent actionEvent) {
        sendRequestToServer(true);
    }

    /**
     * отправляет запрос насервер
     * @param isReg - признак операции - регистрация (true) / авторизация (false)
     */
    private void sendRequestToServer(boolean isReg) {
        boolean wasSended = clientUI.SendAuthRegRequest(new AuthRegRequest(loginField.getText(), passwordField.getText(), isReg));
        if (wasSended) {
            synchronized (this) {
                blockUI(true);
                try {
                    wait(10000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                blockUI(false);
            }
        }
    }

    /**
     * обработчик нажатия кнопки Log in
     * @param actionEvent - ни на что не виляет
     */
    @FXML
    public void btnTryLog(ActionEvent actionEvent) {
        sendRequestToServer(false);
    }

    /**
     * обработка получения ответа от сервера
     * @param answer - сообщение-ответ от сервера
     */
    public void getAnswerFromServer(AuthRegAnswer answer) {
        /*
        * тут будет снятие блокировки ожидания
         */
        synchronized (this) {
            notify();
        }

        if (answer.isResult()) {
            //ответ положительный
            if (answer.isOperationReg()) {
                Platform.runLater(()->{
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Operation is OK");
                    alert.setHeaderText("New user was created on server. Now you can connect.");
                    alert.setContentText(answer.getMessage());

                    alert.showAndWait();
                });
            }
            else {
                clientUI.switchOnConnection();

                Platform.runLater(() -> {
                    Stage thisStage = (Stage) btnLogin.getScene().getWindow();
                    thisStage.close();
                });
            }
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

    private void blockUI(boolean block) {
        passwordField.setDisable(block);
        loginField.setDisable(block);
        btnLogin.setDisable(block);
        btnRegistration.setDisable(block);
    }
}
