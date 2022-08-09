package database;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.format.DateTimeFormat;
import parliament.PlenarySessionProtocol;

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    /***
     * Inserts the deputies into the imported table.
     */
    public void insertImportedDeputies(ArrayList<Deputy_Mongo> deputies) throws SQLException, JSONException {
        var statement = db.createStatement();
        var query = "";
        var dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        var date = dtf.format(LocalDateTime.now());

        for(int i = 0; i < deputies.size(); i++){
            var cur = deputies.get(i);
            // First, add the protocol to the statement.
            query += buildImportedEntityInsertString(
                    UUID.randomUUID().toString(),
                    date,
                    cur.toJSONObject().toString(),
                    2, // Deputy = 0
                    "00000000-0000-0000-0000-000000000000"
            );
        }
        // Save them.
        statement.execute(query);
    }

    /***
     * Adds a protocol along with its nlp speeches into the database.
     * @param protocol
     * @param nlpSpeeches
     * @throws JSONException
     * @throws SQLException
     */
    public void insertImportedProtocol(PlenarySessionProtocol protocol,
                                       ArrayList<JSONObject> nlpSpeeches) throws JSONException, SQLException {
        // Generate some properties beforehand
        var protocolJson = new PlenarySessionProtocol_Mongo(protocol).toJSONObject().toString();
        var dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        var date = dtf.format(LocalDateTime.now());
        var protocolId = UUID.randomUUID();

        var st = db.createStatement();
        // First, add the protocol to the statement.
        var sqlStr = buildImportedEntityInsertString(
                protocolId.toString(),
                date,
                protocolJson,
                0, // Protocol = 0
                "00000000-0000-0000-0000-000000000000"
        );

        // Now add the speeches
        for(int i = 0; i < nlpSpeeches.size(); i++){
            var cur = nlpSpeeches.get(i);
            var speechStr = buildImportedEntityInsertString(
                    UUID.randomUUID().toString(),
                    date,
                    cur.toString(),
                    1, // NLPSpeech = 1
                    protocolId.toString()
            );
            // Append the insert into the command.
            sqlStr += speechStr;
        }

        // Execute the query
        st.execute(sqlStr);
    }

    private String buildImportedEntityInsertString(String id, String date, String json, int type, String protocolId){
        var statement = "insert into ImportedEntities (Id, ImportedDate, ModelJson, Type, ProtocolId) values (" +
                "'" + id+ "'," +
                "'" + date + "'," +
                "'" + json + "'," +
                "'" + type + "'," +
                "'" + protocolId + "'" +
                ");";
        return statement;
    }
}
