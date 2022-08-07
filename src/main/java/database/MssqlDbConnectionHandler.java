package database;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import parliament.PlenarySessionProtocol;

import java.io.*;
import java.sql.*;
import java.time.ZonedDateTime;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;


public class MssqlDbConnectionHandler {

    private static final String dbPropertiesFilePath = "src/main/database.properties";
    private static Connection db;

    public Connection getDb() {return db;}

    public boolean init(){
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
            return true;
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
            e.printStackTrace();
            return false;
        }
    }

    /***
     * Returns the latest number of protocols which is stored in the database.
     * @return
     */
    public int getLatestProtocolNumber(int legislaturePeriod) throws SQLException {
        Statement st = db.createStatement();
        String sqlStr = "select max(Number) as Number from Protocols where LegislaturePeriod = " + legislaturePeriod;
        ResultSet rs = st.executeQuery(sqlStr);
        while (rs.next()) {
            return rs.getInt("Number");
        }
        return 0;
    }

    public void insertImportedProtocol(PlenarySessionProtocol protocol) throws JSONException, SQLException {
        var asJson = new PlenarySessionProtocol_Mongo(protocol).toJSONObject().toString();
        var curDate = ZonedDateTime.now();
        var st = db.createStatement();
        var sqlStr = "insert into ImportedProtocols (Id, ImportedDate, ProtocolJson) values ("
            + UUID.randomUUID() + "," + curDate + "," + asJson + ");";
        var rs = st.executeQuery(sqlStr);
        var xd = "";
    }
}
