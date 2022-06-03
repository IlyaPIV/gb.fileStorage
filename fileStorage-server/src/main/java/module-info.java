module fileStorage.server {
    requires fileStorage.connectionAPI;
    requires java.logging;
    requires lombok;
    requires io.netty.transport;
    requires io.netty.codec;
    requires org.slf4j;
}