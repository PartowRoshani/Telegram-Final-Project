package org.to.telegramfinalproject.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionDb {
    private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/Telegramdb";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "Partow@1384";

    public ConnectionDb() {
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
    }
}