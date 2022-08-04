package database;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.InputStream;


public class MssqlDbConnectionHandler {

    private static final String dbPropertiesFilePath = "src/main/database.properties";
    private static Connection db;

    public Connection getDb() {return db;}

    public MssqlDbConnectionHandler(){
        try (InputStream input = new FileInputStream(dbPropertiesFilePath)) {
            Properties prop = new Properties();
            prop.load(input);

            //Loading the required JDBC Driver class
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            var connectionString = String.format(
                    "jdbc:sqlserver://"+ prop.getProperty("db.url") + ":" + prop.getProperty("db.port") +
                            ";databaseName="+ prop.getProperty("db.database") +
                            ";user="+ prop.getProperty("db.username") + ";password=" + prop.getProperty("db.password"));

            db = DriverManager.getConnection(connectionString);

            //Executing SQL query and fetching the result
            /*
            Statement st = db.createStatement();
            String sqlStr = "select * from Deputies";
            ResultSet rs = st.executeQuery(sqlStr);
            while (rs.next()) {
                System.out.println(rs.getString("FirstName"));
            }
            */
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
