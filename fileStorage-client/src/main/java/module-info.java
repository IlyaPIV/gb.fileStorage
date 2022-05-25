module com.gb.filestorage.filestorageclient {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.gb.filestorage.filestorageclient to javafx.fxml;
    exports com.gb.filestorage.filestorageclient;
}