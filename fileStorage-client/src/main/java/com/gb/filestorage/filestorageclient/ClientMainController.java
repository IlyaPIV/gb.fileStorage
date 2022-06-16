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
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import messages.AuthRegAnswer;
import messages.AuthRegRequest;
import serverFiles.ServerFile;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@Slf4j
public class ClientMainController implements Initializable {

    private Stage mainWindow;
    private AuthRegWindow logRegWindow;
    private NettyConnection nettyConnection;
    private Thread inChannelListener;
    private boolean isConnected;

    private NetworkConnection connection; //old and not used
    private boolean terminalIsRunning;  //old and not used
    private TerminalClient terminalClient;  //old and not used

    @FXML
    public VBox terminalWorkingArea; //old and not used
    @FXML
    public TextField terminalCmndLine;  //old and not used
    public StackPane serverHalf;
    @FXML
    public VBox serverWorkingArea;
    @FXML
    public TextArea terminalDisplay; //old and not used
    @FXML
    private TextField infoField;
    @FXML
    public Button btnTerminal; //old and not used
    @FXML
    public Button button_newDir;
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
    public Button button_connect;
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
//        updateClientList(Paths.get(disksBox.getSelectionModel().getSelectedItem()));
        updateClientList(Paths.get(System.getProperty("user.home")));
        prepareServerTable();

        Platform.runLater(()->{
            mainWindow = (Stage) infoField.getScene().getWindow();
            mainWindow.setOnCloseRequest(windowEvent -> {
                closeNettyConnection();
            });
        });

        setDisableButtonsUI();

    }

    /*
     * ======================== ОБРАБОТЧИКИ UI ФОРМЫ ==============================
     *
     */

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
                        String text = String.format("%,.2f KB", item/1024.0);
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

        TableColumn<ServerFile,Integer> pozColumn = new TableColumn<>("position");
        pozColumn.setCellValueFactory(param ->
                new SimpleObjectProperty<>(param.getValue().getPoz()));
        pozColumn.setVisible(false);

        TableColumn<ServerFile,String> fileNameColumn = new TableColumn<>("Name");
        fileNameColumn.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getFileName()));
        fileNameColumn.setPrefWidth(305);
        fileNameColumn.setSortable(false);


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
                        String text = String.format("%,.2f KB", item/1024.0);
                        if (item == -1L) text ="\t[DIR]";
                        setText(text);
                    }
                }
            };
        });
        fileSizeColumn.setSortable(false);

        TableColumn<ServerFile, String> fileDataColumn = new TableColumn<>("Date");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        fileDataColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastUpdate().format(dtf)));
        fileDataColumn.setPrefWidth(120);
        fileDataColumn.setSortable(false);


        serverFilesTable.getColumns().addAll(fileNameColumn, fileSizeColumn, fileDataColumn);
        serverFilesTable.getSortOrder().add(pozColumn);


        serverFilesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    if (serverFilesTable.getSelectionModel().getSelectedItem()!=null) {
                        ServerFile sf = serverFilesTable.getSelectionModel().getSelectedItem();
                        if (sf.isDir()) {
                            try {
                                nettyConnection.sendPathChangeRequest(sf);
                            } catch (IOException e) {
                                setInfoText(e.getMessage());
                            }
                        }
                    }
                }
            }
        });
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
     * обработчик события выбора новой дирректории
     */
    @FXML
    private void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateClientList(Paths.get(element.getSelectionModel().getSelectedItem()));
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
     * возвращает имя выбранного файла на стороне клиента
     * @return - строка с именем файла
     */
    private String getSelectedFileName() {
        return clientFilesTable.getSelectionModel().getSelectedItem().getFilename();
    }

    /**
     * возвращает текущую дирректорию на стороне клиента
     * @return - строка с адресом директории
     */
    public String getCurrentPath() {
        return pathField.getText();
    }

    /**
     * обновляет табличную часть файлов пользователя на сервере
     * @param files - список файлов, полученный от сервера
     */
    public void updateServerList(List<ServerFile> files){
        log.debug("Количество файлов на сервере = "+files.size());
        serverFilesTable.getItems().clear();
        serverFilesTable.getItems().addAll(files);
        serverFilesTable.sort();
    }

    /**
     * обновляет таблицу файлов на клиенте данными из указанной директории
     */
    public void updateClientList(Path path){

        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            clientFilesTable.getItems().clear();
            clientFilesTable.getItems().addAll(Files.list(path).map(ClientFileInfo::new).toList());
            clientFilesTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список", ButtonType.OK);
            alert.showAndWait();
            log.error("Не удалось обновить список файлов на стороне клиента");
        }
    }

    /**
     * устанавливает доступность кнопок в зависимости от статуса соединения с сервером
     */
    private void setDisableButtonsUI() {
        button_addLink.setDisable(!isConnected);
        button_delete.setDisable(!isConnected);
        button_download.setDisable(!isConnected);
        button_share.setDisable(!isConnected);
        button_rename.setDisable(!isConnected);
        button_upload.setDisable(!isConnected);
        button_newDir.setDisable(!isConnected);
        serverFilesTable.setDisable(!isConnected);
        button_connect.setStyle(isConnected ? "-fx-background-color: green" : "-fx-background-color: red");
    }

    /**
     * выводит текст серверного сообщения в инфо поле
     * @param text текст сообщения
     */
    public void setInfoText(String text){
        infoField.clear();
        infoField.setText(text.toUpperCase());
        infoField.setStyle("-fx-background-color: lightgray");
    }

    /**
     * выводит текст серверного сообщения в инфо поле
     * @param text текст сообщения
     */
    public void setInfoText(String text, boolean fail){
        infoField.clear();
        infoField.setText(text.toUpperCase());
        infoField.setStyle(fail ? "-fx-background-color: pink" : "-fx-background-color: lightgreen");
    }

    /**
     * обработчик события выхода из программы
     */
    @FXML
    private void btnExitAction(ActionEvent actionEvent){

        Platform.exit();

    }

    /**
     * подготавливает и открывает окно авторизации/регистрации
     */
    private void openLogRegWindow() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("authreg-view.fxml"));
        mainWindow = (Stage) infoField.getScene().getWindow();
        try {

            Parent root = fxmlLoader.load();

            Stage logRegStage = new Stage();

            logRegStage.setTitle("Log in to server");
            logRegStage.setScene(new Scene(root, 400, 150));

            logRegWindow = fxmlLoader.getController();
            logRegWindow.setMainController(this);

            logRegStage.getScene().getStylesheets().add(getClass().getResource("css/StyleRegLoginWindow.css").toExternalForm());
            logRegStage.initStyle(StageStyle.UTILITY);
            logRegStage.initModality(Modality.WINDOW_MODAL);
            logRegStage.initOwner(mainWindow);

            logRegStage.show();

        } catch (IOException e) {
            log.error("Failed to open log/reg window");
        }
    }

    /**
     * обработчик нажатия кнопки "Добавить папку"
     * @param actionEvent - событие нажатия на кнопку
     */
    @FXML
    public void cmdAddDir(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog("new directory name");

        dialog.setTitle("Fill the field");
        dialog.setHeaderText("Enter new folder name:");
        dialog.setContentText("DIR name:");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(this::tryToCreateNewFolderOnServer);
    }

    /*
     *
     * ======================== МЕТОДЫ РАБОТЫ С СЕРВЕРОМ ==========================
     *
     */

    /**
     * создаёт подключение к серверу через Netty
     */
    private void createConnectionToServer() throws IOException {

        if (nettyConnection==null) {
            this.nettyConnection = new NettyConnection(this);
            log.debug("Created NETTY connection with server");
        }

        if (inChannelListener == null || inChannelListener.getState()== Thread.State.TERMINATED) {
            inChannelListener = new Thread(nettyConnection::messageReader);
            inChannelListener.setDaemon(true);
            inChannelListener.start();
        }

    }

    /**
     * если соединения с сервером нет - открывает окно регистрации/авторизации
     * если соединение с сервером подключено - отключает его
     * @param actionEvent - не виляет
     */
    public void switchConnect(ActionEvent actionEvent) {
        if (!isConnected) {
            try {
                createConnectionToServer();
                openLogRegWindow();

                log.debug("Opening auth/reg window");
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        } else {
            closeNettyConnection();
            this.isConnected = false;
            setDisableButtonsUI();

            log.debug("Connection with server turned off");
            setInfoText("Connection with server turned off");
        }
    }

    /**
     * включение функционала работы с сервером
     */
    public void switchOnConnection(){
        log.debug("Connection with server turned on");
        this.isConnected = true;
        setDisableButtonsUI(); //временно - отключить потом

        updateServerFilesList();

        setInfoText("Connection with server turned on");
    }

    /**
     * принудительно закрывает соединение с сервером
     */
    private void closeNettyConnection() {
        /*
         * тут будет код
         */
        log.debug("Closed NETTY connection");
        inChannelListener.interrupt();

        serverFilesTable.getItems().clear();
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
     * обработчик нажатия кнопки отправки файла на сервер
     * @param actionEvent - не используется
     */
    @FXML
    public void sendFileToServer(ActionEvent actionEvent) {

        if (clientFilesTable.getSelectionModel().getSelectedItem()!=null) {
            String fileName = clientFilesTable.getSelectionModel().getSelectedItem().getFilename();
            Path fileFullPath = Paths.get(pathField.getText()).resolve(fileName);

            if (Files.isDirectory(fileFullPath)) {
                setInfoText("Can't send directory. Please, choose a file.");
            } else {
                try {
                    nettyConnection.sendFileToServer(fileFullPath);
                    setInfoText("File was transferred");
                } catch (IOException e) {
                    log.error("Error in file transfer to server!");
                    setInfoText("Error in file transfer to server!");
                }

                //отправляем запрос на обновление списка файлов на сервере
                try {
                    nettyConnection.sendFilesListRequest();
                } catch (IOException e) {
                    log.error("Failed to update servers side files list!");
                }
            }
        }
    }

    /**
     * обработчик нажатия кнопки получения файла с сервера
     * @param actionEvent - ни на что не влияет
     */
    public void getFileFromServer(ActionEvent actionEvent) {
        ServerFile selectedFile = serverFilesTable.getSelectionModel().getSelectedItem();
        if (selectedFile.isDir()) {
            setInfoText("Can't download directory. Please, choose a file.");
        } else {
            try {
                nettyConnection.getFileFromServer(selectedFile.getFileName(), selectedFile.getServerID());
                log.debug(String.format("Отправлен запрос на скачивание файла с сервера: %s", selectedFile.getFileName()));
            } catch (IOException e) {
                log.error("Failed to send downloading request.");
            }
        }
    }

    /**
     * отправляет через Netty запрос на авторизацию/регистрацию
     * @param request - подготовленный запрос на форме авторизации
     * @return - true если сообщение отправлено на сервер
     * false - если сообщение не было отправлено на сервер
     */
    public boolean SendAuthRegRequest(AuthRegRequest request) {
        try {
            nettyConnection.sendAuthRegRequest(request);
            return true;
        } catch (IOException e) {
            log.error("Ошибка отправки запроса на авторизацию/регистрацию");
            return false;
        }
    }

    /**
     * перенаправляет на окно регистрации/авторизации сообщение от сервера
     * @param answer - ответ сервера
     */
    public void GetAuthRegAnswer(AuthRegAnswer answer) {
        log.debug("Получен ответ от сервера на запрос авторизации/регистрации: "+answer.isResult());
        logRegWindow.getAnswerFromServer(answer);
    }


    /**
     * попытка создать новую папку в текущей директории на сервере
     * @param newName - имя новой папки
     */
    public void tryToCreateNewFolderOnServer(String newName) {
        if (checkServersTableForDuplicateDirName(newName)) {
            setInfoText("New DIR was created!", false);
            try {
                nettyConnection.createNewDir(newName);
            } catch (IOException e) {
                setInfoText(e.getMessage(), true);
                log.error("Ошибка отправки запроса на сервер для создания нового каталога");
            }
        } else setInfoText("This name is already used!", true);
    }

    /**
     * проверяет текущую директорию на серверной стороне на наличие дубликата по имени папки
     * @param newName - имя новой папки
     * @return - true если имя доступно
     * false если папка с таким именем уже существует
     */
    private boolean checkServersTableForDuplicateDirName(String newName) {

        for (ServerFile sf:
             serverFilesTable.getItems()) {

            if (sf.getFileName().equals(newName) && sf.isDir()) return false;
        }

        return true;
    }

    /*
    * ========================= НЕ ИСПОЛЬЗУЕМЫЕ БОЛЕЕ МЕТОДЫ ======================
    *
     */

    /**
     * обработчик нажатия кнопки терминала
     * @param actionEvent - ни на что не влияет
     */
    @FXML
    public void cmdTerminal(ActionEvent actionEvent) {
        switchTerminal();
    }

    /**
     * переключает режим окна сервера с табличного вида на терминальное соединение и обратно
     */
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
     * @param actionEvent - ни на что не влияет
     */
    @FXML
    public void cmndTerminate(ActionEvent actionEvent) {

        String textInCmd = terminalCmndLine.getText();

        terminalCmndLine.clear();

        terminalClient.sendMsgToServer(textInCmd);

    }

    /**
     * закрывает терминальное соединение с сервером - не работает корректно
     */
    private void closeTerminalConnection() {
        try {
            if (terminalClient!=null && terminalClient.isRunning()) {
                terminalClient.stop();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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


}