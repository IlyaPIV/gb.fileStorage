module com.gb.filestorage.filestorageclient {
    requires javafx.controls;
    requires javafx.fxml;
    requires fileStorage.connectionAPI;


    opens com.gb.filestorage.filestorageclient to javafx.fxml;
    exports com.gb.filestorage.filestorageclient;
}