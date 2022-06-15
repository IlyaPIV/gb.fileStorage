module com.gb.filestorage.filestorageclient {
    requires javafx.controls;
    requires javafx.fxml;
    requires fileStorage.connectionAPI;
    requires lombok;
    requires io.netty.transport;
    requires io.netty.codec;
    requires org.slf4j;


    opens com.gb.filestorage.filestorageclient to javafx.fxml;
    exports com.gb.filestorage.filestorageclient;
}