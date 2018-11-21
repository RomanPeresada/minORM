package config;

//import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class ConnectionWithDb {
    private static final String CREATE_DB = "CREATE DATABASE IF NOT EXISTS ";
    private static final String CONNECT_TO_DB = "jdbc:mysql://localhost:3306/";
    private static String dbName = null;
    private static String loginForDb = null;
    private static String passwordForDb = null;
    private static String dbHost = null;
    //private final static Logger log = Logger.getLogger(ConnectionWithDb.class);


    static {
        try {
            getInitialConnectionWithDB();
            // log.debug("Connection to database completed successful");
        } catch (SQLException | IOException e) {
            // log.error(e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(CONNECT_TO_DB + dbName, loginForDb, passwordForDb);
    }

    private static void getInitialConnectionWithDB() throws SQLException, IOException {
        getInfoFromProperties();
        try (Connection connectionForCreateDbIfNotExists = DriverManager.getConnection(String.valueOf(
                new StringBuilder(dbHost + "/?user=").append(loginForDb)
                        .append("&password=").append(passwordForDb)))) {

            Statement statement = connectionForCreateDbIfNotExists.createStatement();
            statement.executeUpdate(CREATE_DB + dbName);
        }
    }

    private static void getInfoFromProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(new File("src/main/resources/db.properties")));
        loginForDb = properties.get("login").toString();
        passwordForDb = properties.get("password").toString();
        dbName = properties.get("dbName").toString();
        dbHost = properties.get("dbHost").toString();
    }
}
