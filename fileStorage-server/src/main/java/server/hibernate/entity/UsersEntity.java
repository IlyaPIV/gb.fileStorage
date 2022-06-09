package server.hibernate.entity;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "users", schema = "public", catalog = "de9oban5pa49j0")
public class UsersEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "user_id")
    private int userId;
    @Basic
    @Column(name = "login")
    private String login;
    @Basic
    @Column(name = "password")
    private String password;

    public UsersEntity() {
    }

    public UsersEntity(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Column(name="login")
    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsersEntity that = (UsersEntity) o;
        return userId == that.userId && Objects.equals(login, that.login) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, login, password);
    }

    @Override
    public String toString() {
        return "User: {" +
                "userId=" + userId +
                ", login='" + login + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

    /*
     * ================================= ЗАПРОСЫ К БД ===============================
     */

    /**
     * создание текста запроса на поиск пользователя по логину
     * @param login - логин пользователя
     * @return строку с текстом запроса к БД на PostgresSQL
     */
    public static String queryPostgresFindByLogin(String login){
        return String.format("SELECT u FROM users u WHERE u.login = '%s'", login);
    }
}
