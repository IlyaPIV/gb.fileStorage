package server;

public interface AuthService {

    /**
     * метод регистрации новых пользователей
     * @param login - имя пользователя
     * @param password - пароль пользователя
     */
    void registration(String login, String password);

    /**
     * метод для авторизации пользователей
     * @param login - имя пользоватя
     * @param password - пароль пользователя
     * @return true - в случае если такой пользователь зарегестрирован и указан правильный пароль
     * false - в случае неверных значений
     */
    int authentication(String login, String password);


}
