package com.gb.filestorage.filestorageclient;

import com.gb.filestorage.filestorageclient.files.ClientFileInfo;
import com.gb.filestorage.filestorageclient.network_IO.NetworkConnection;
import com.gb.filestorage.filestorageclient.network_Netty.NettyConnection;
import com.gb.filestorage.filestorageclient.terminal_NIO.TerminalClient;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import serverFiles.ServerFile;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

@Slf4j
public class ClientMainController implements Initializable {


    private Stage mainWindow;
    private NetworkConnection connection;

    private NettyConnection nettyConnection;
    private boolean terminalIsRunning;

    private TerminalClient terminalClient;

    @FXML
    public VBox terminalWorkingArea;
    @FXML
    public TextField terminalCmndLine;
    public StackPane serverHalf;


    @FXML
    public VBox serverWorkingArea;
    @FXML
    public TextArea terminalDisplay;
    @FXML
    private TextField infoField;
    @FXML
    public Button btnTerminal;
    @FXML
    public Button button_share;
    @FXML
    public Button button_addLink;
    @FXML
    public Button button_rename;
    @FXML
    public Button button_delete;
    @FXML
    public Button button_upload;
    @FXML
    public Button button_download;
    @FXML
    public ComboBox<String> disksBox;

    @FXML
    public TextField pathField;

    @FXML
    public TableView<ClientFileInfo> clientFilesTable;
    @FXML
    public TableView<ServerFile> serverFilesTable;

    @Override @FXML
    public void initialize(URL url, ResourceBundle resourceBundle) {

        prepareClientTable();
        prepareDisksBox();
        updateClientList(Paths.get(disksBox.getSelectionModel().getSelectedItem()));
        prepareServerTable();

        /*
        * отключено IO соединение
         */
        //connectToServerIO();
        //
        try {
            createConnectionToServer();
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        /**
         * место под авторизацию
         */
        log.debug("Пользователь успешно авторизирован на сервере");
        nettyConnection.setAuthorizated(true);

        /**
         * обновление списка файлов - потом перенесётся в другое место к блоку авторизации
         */
        if (nettyConnection.isAuthorizated()){
            updateServerFilesList();
        }



        Platform.runLater(()->{
            mainWindow = (Stage) infoField.getScene().getWindow();
            mainWindow.setOnCloseRequest(windowEvent -> {
//                if (connection.isSocketInit() && !connection.isSocketClosed()) {
//                    connection.closeConnection();
//                }
                closeTerminalConnection();
            });
        });
    }

    /**
     * обновляет список файлов пользователя на сервере
     */
    private void updateServerFilesList() {
        try {
            nettyConnection.sendFilesListRequest();
        } catch (IOException e) {
            log.error("Ошибка отправки запроса списка файлов на сервер!");
        }
    }

    /**
     * создаёт подключение к серверу через Netty
     */
    private void createConnectionToServer() throws IOException {

        this.nettyConnection = new NettyConnection();

    }

    /**
     * выводит текст серверного сообщения в инфо поле
     * @param text текст сообщения
     */
    public void setInfoText(String text){
        infoField.clear();
        infoField.setText(text.toUpperCase());
    }

    /**
     * инициирует настройки сетевого подключения и пробует установить соединение с сервером (IO connection)
     */
    private void connectToServerIO() {
        if (this.connection == null || this.connection.isSocketClosed()) {
            this.connection = new NetworkConnection(this, "login", "pass");

            boolean connected = connection.connectToServer();
            setInfoText( connected ? "CONNECTION TO SERVER CREATED" : "CONNECTION TO SERVER FAILED");

            connection.startWorkingThreadWithServer();
        }

        connection.tryToAuthOnServer();
    }

    /**
    * подготавливает таблицу файлов на клиентской стороне
     */
    private void prepareClientTable() {

        TableColumn<ClientFileInfo,String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(20);

        TableColumn<ClientFileInfo,String> fileNameColumn = new TableColumn<>("Name");
        fileNameColumn.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getFilename()));
        fileNameColumn.setPrefWidth(290);


        TableColumn<ClientFileInfo, Long> fileSizeColumn = new TableColumn<>("Size");
        fileSizeColumn.setCellValueFactory(param ->
                new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setPrefWidth(105);
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<ClientFileInfo, Long>(){
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item==null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item== -1L) text = "\t[DIR]";
                        setText(text);
                    }
                }
            };
        });

        TableColumn<ClientFileInfo, String> fileDataColumn = new TableColumn<>("Date");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        fileDataColumn.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getLastTimeModified().format(dtf)));
        fileDataColumn.setPrefWidth(120);



        clientFilesTable.getColumns().addAll(fileTypeColumn,fileNameColumn, fileSizeColumn, fileDataColumn);
        clientFilesTable.getSortOrder().add(fileTypeColumn);

        clientFilesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    if (clientFilesTable.getSelectionModel().getSelectedItem()!=null) {
                        Path path = Paths.get(pathField.getText()).resolve(clientFilesTable.getSelectionModel().getSelectedItem().getFilename());
                        if (Files.isDirectory(path)) {
                            updateClientList(path);
                        }
                    }
                }
            }
        });
    }

    /**
     * подготавливает таблицу файлов на сервере
     */
    private void prepareServerTable(){
        TableColumn<ServerFile,String> fileNameColumn = new TableColumn<>("Name");
        fileNameColumn.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getFileName()));
        fileNameColumn.setPrefWidth(305);


        TableColumn<ServerFile, Long> fileSizeColumn = new TableColumn<>("Size");
        fileSizeColumn.setCellValueFactory(param ->
                new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setPrefWidth(110);
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<ServerFile, Long>(){
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item==null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        setText(text);
                    }
                }
            };
        });

        TableColumn<ServerFile, String> fileDataColumn = new TableColumn<>("Date");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        fileDataColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastUpdate().format(dtf)));
        fileDataColumn.setPrefWidth(120);



        serverFilesTable.getColumns().addAll(fileNameColumn, fileSizeColumn, fileDataColumn);
        serverFilesTable.getSortOrder().add(fileNameColumn);
    }

    /**
     * подготавливает список дирректорий
     */
    private void prepareDisksBox(){
        disksBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            disksBox.getItems().add(p.toString().trim());
        }
        disksBox.getSelectionModel().select(0);

    }

    /**
    * обновляет таблицу файлов на клиенте данными из указанной директории
    */
    private void updateClientList(Path path){

        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            clientFilesTable.getItems().clear();
            clientFilesTable.getItems().addAll(Files.list(path).map(ClientFileInfo::new).toList());
            clientFilesTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список", ButtonType.OK);
            alert.showAndWait();
        }
    }

    /**
     * обработчик события выхода из программы
     */
    @FXML
    private void btnExitAction(ActionEvent actionEvent){

        //connection.closeConnection();

        Platform.exit();

        closeTerminalConnection();

    }

    /**
     * обработчик события поднятия вверх по каталогу (в родительский каталог)
     */
    @FXML
    private void btnPathUpAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if(upperPath != null) {
            updateClientList(upperPath);
        }
    }

    /**
     * обработчик события выбора новой дирректории
     */
    @FXML
    private void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateClientList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    /**
     * возвращает имя выбранного файла на стороне клиента
     * @return
     */
    private String getSelectedFileName() {
        return clientFilesTable.getSelectionModel().getSelectedItem().getFilename();
    }

    /**
     * возвращает текущую дирректорию на стороне клиента
     * @return
     */
    private String getCurrentPath() {
        return pathField.getText();
    }

    /**
     * обработчик нажатия кнопки отправки файла на сервер
     * @param actionEvent
     */
    @FXML
    public void sendFileToServer(ActionEvent actionEvent) {

        if (clientFilesTable.getSelectionModel().getSelectedItem()!=null) {
            String fileName = clientFilesTable.getSelectionModel().getSelectedItem().getFilename();
            Path fileFullPath = Paths.get(pathField.getText()).resolve(fileName);

            if (Files.isDirectory(fileFullPath)) {
                setInfoText("Can't send directory. Please, choose a file.");
            } else {
                setInfoText("File was transfered");
                //connection.fileSendToServer(fileFullPath, fileName);
                //connection.updateServersFilesList();
            }
        }
    }

    @FXML
    public void cmdTerminal(ActionEvent actionEvent) {
        switchTerminal();
    }

    private void switchTerminal() {

        if (terminalClient == null) {
            try {
                terminalClient = new TerminalClient(this);
            } catch (IOException e) {
                setInfoText("ERROR INIT TERMINAL INTERFACE");
            }
        }

        this.terminalIsRunning = !terminalIsRunning;

        ObservableList<Node> layers = this.serverHalf.getChildren();
        if (layers.size()>1) {
            Node topLayer = layers.get(layers.size()-1);

            Node newTop = layers.get(layers.size()-2);

            topLayer.setVisible(false);
            topLayer.toBack();

            newTop.setVisible(true);

            terminalDisplay.clear();
        }



        try {
            terminalClient.start();
        } catch (IOException e) {
            setInfoText("ERROR START CONNECTION WITH SERVER");
        }



        if (terminalIsRunning) {
            terminalDisplay.appendText("\n");
            terminalDisplay.appendText("Welcome to terminal interface.\n");
            terminalDisplay.appendText("To get list of commands type \"help\".\n");
            terminalDisplay.appendText("\n");
            //printTerminalCommands();
        }

        setInfoText("terminal is "+ (terminalIsRunning ? "running" : "off"));
    }

    /**
     * обработчик ввода строки командной панели терминала
     * @param actionEvent
     */
    @FXML
    public void cmndTerminate(ActionEvent actionEvent) {

        String textInCmd = terminalCmndLine.getText();

        terminalCmndLine.clear();

        terminalClient.sendMsgToServer(textInCmd);

    }


    private void closeTerminalConnection() {
        try {
            if (terminalClient!=null && terminalClient.isRunning()) {
                terminalClient.stop();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}