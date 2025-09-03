package org.to.telegramfinalproject.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionDb {
    private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/telegram_dev";
    private static final String USERNAME = "telegram_user";
    private static final String PASSWORD = "@Shima138424";

    public ConnectionDb() {
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
    }

}