package server;

/**
 * Ошибка выполнения работы с БД на сервере, обобщенная
 */
public class ServerCloudException extends Exception{


    public ServerCloudException() {
    }

    public ServerCloudException(String s) {
        super(s);
    }
}
