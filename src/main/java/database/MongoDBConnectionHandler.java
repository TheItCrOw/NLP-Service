/**
 * This class handles everything around connecting to mongo db, updating records, deleting records
 * and getting records.
 * @author: Gabriele
 */
package database;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.MongoClient;
import org.bson.Document;
import org.codehaus.jettison.json.JSONObject;
import org.xml.sax.SAXException;
import parliament.PlenarySessionProtocol;
import org.codehaus.jettison.json.JSONException;
import relations.Speech;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;


public class MongoDBConnectionHandler {
    String host;
    String database;
    String password;
    MongoDatabase db;

    public MongoDBConnectionHandler(){
        this.db = this.getMongoDb();
    }

    public MongoDatabase getDb(){
        return this.db;
    }

    MongoDatabase getMongoDb(){
        /**
         * Return a MongoDatabase object based on the credentials in the database.properties file.
         */
        try{
            ConnectionString connectionString = new ConnectionString("mongodb+srv://TheItCrOW:w{KbL*GmWJ}a=N6iykUyGi;{L0I!s7j^@cluster0.bjqsv.mongodb.net/?retryWrites=true&w=majority");
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .build();
            MongoClient mongoClient = MongoClients.create(settings);
            MongoDatabase database = mongoClient.getDatabase("bundestagMining");
            return database;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Utility function for checking whether a collection exists.
     */
    public boolean collectionDoesNotExist(String name){
        return !this.db.listCollectionNames().into(new ArrayList<String>()).contains(name);
    }

    /**
     * Insert all the extracted XML files into mongodb.
     *
     * For this, we first create accompanying ..._Mongo.java objects, which implement the following methods for us:
     * - toJSONObject()
     * - toCAS()
     *
     * the former can be used to create nested structures to store them into mongodb.
     */
    public void insertProtocols(PlenarySessionProtocol[] protocols, ArrayList<Speech> speeches, ArrayList<Deputy_Mongo> deputies) throws ParserConfigurationException, IOException, SAXException, ParseException, JSONException, IllegalAccessException {
        if (collectionDoesNotExist("Protocols")){
            db.createCollection("Protocols");
            MongoCollection<Document> protocolCol = db.getCollection("Protocols");
            ArrayList<Document> documentList = new ArrayList<Document>();
            for (PlenarySessionProtocol p : protocols){
                documentList.add(Document.parse(new PlenarySessionProtocol_Mongo(p).toJSONObject().toString()));
            }
            protocolCol.insertMany(documentList);
        }

        if (collectionDoesNotExist("Speeches")){
            db.createCollection("Speeches");
            MongoCollection<Document> speechesCol = db.getCollection("Speeches");
            ArrayList<Document> documentList = new ArrayList<Document>();
            int speechCount = speeches.size();
            for (int i = 0; i < speechCount; i++){
                Speech s = speeches.get(i);
                speechesCol.insertOne(Document.parse(new Speech_Mongo(s).toJSONObject().toString()));
                System.out.println("Progress: " + (i + 1) + " / " + speechCount);
            }
        }

        if (collectionDoesNotExist("Deputies")){
            db.createCollection("Deputies");
            MongoCollection<Document> deputyCol = db.getCollection("Deputies");
            ArrayList<Document> documentList = new ArrayList<Document>();
            for (Deputy_Mongo d : deputies){
                documentList.add(Document.parse(d.toJSONObject().toString()));
            }
            deputyCol.insertMany(documentList);
        }
    }

    /**
     * Counts the amount of items that already are in a collection
     *
     * @param collectionName name of collection as String
     * @return count of items in collection as integer
     */
    public int getCountItemsInCollection(String collectionName) {
        long count = 0L;
        try {
            if (collectionDoesNotExist(collectionName)) {
                System.out.println("Collection has not been found");
            } else {
                MongoCollection<Document> col = db.getCollection(collectionName);
                count = col.countDocuments();
            }
        } catch (MongoSocketWriteException e) {
            e.printStackTrace();
        }
        return (int) count;
    }

    /**
     * Pushes a speech with NLP-results to the db
     *
     * @param jo the processed speech as a JSONObject
     */
    public void pushNLPResultToDB(JSONObject jo) {
        if (collectionDoesNotExist("SpeechesNLP")){
            db.createCollection("SpeechesNLP");
        } else {
            MongoCollection<Document> speechesNLP = db.getCollection("SpeechesNLP");
            try {
                Document speechNLP = Document.parse(jo.toString());
                speechesNLP.insertOne(speechNLP);
            } catch (MongoSocketWriteException e) {
                e.printStackTrace();
            }
        }
    }
}
