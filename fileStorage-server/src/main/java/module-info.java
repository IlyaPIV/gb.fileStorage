module fileStorage.server {
    requires fileStorage.connectionAPI;
    requires java.logging;
    requires lombok;
    requires io.netty.transport;
    requires io.netty.codec;
    requires org.slf4j;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.naming;

    opens server.hibernate.entity;
}