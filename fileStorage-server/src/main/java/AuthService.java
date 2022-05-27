public interface AuthService {

    /**
     * метод регистрации новых пользователей
     * @param login
     * @param password
     * @return true - если новый пользователь успешно добавлен в базу
     * false - в случае ошибки
     */
    boolean registration(String login, String password);

    /**
     * метод для проверки существования такого пользователя
     * @param login
     * @return true - если пользователь есть в базе
     * false - если такого зарегестрированного пользователя нет
     */
    boolean userExist(String login);

    /**
     * метод для авторизации пользователей
     * @param login
     * @param password
     * @return true - в случае если такой пользователь зарегестрирован и указан правильный пароль
     * false - в случае неверных значений
     */
    boolean authentication(String login, String password);
}
