import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.text.ParseException;
import java.util.*;

import api.*;
//import api.RestfulAPI;
//import api.RestfulGeneral;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.sleepycat.je.Database;
import database.Deputy_Mongo;
import database.MongoDBConnectionHandler;
//import org.apache.commons.lang.StringEscapeUtils;
import export.DatabaseExporter;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;

import parliament.PlenarySessionProtocol;
import parsing.XMLParser;
import people.Deputy;
import parliamentapi.ParliamentAPI;



public class View {
    static Set<Deputy> speakers;
    static boolean exportDatabase = false;
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, ParseException, InterruptedException, org.json.simple.parser.ParseException {

        MongoDBConnectionHandler handler = new MongoDBConnectionHandler("src/main/database.properties");

        if(exportDatabase){
            DatabaseExporter.exportCollection("annotation", "\\\\localnas\\home\\Drive\\Bundestag Mining\\19. Legislaturperiode Protokolle Data", handler.getDb());
            DatabaseExporter.exportCollection("answer", "\\\\localnas\\home\\Drive\\Bundestag Mining\\19. Legislaturperiode Protokolle Data", handler.getDb());
            DatabaseExporter.exportCollection("comment_cas", "\\\\localnas\\home\\Drive\\Bundestag Mining\\19. Legislaturperiode Protokolle Data", handler.getDb());
            DatabaseExporter.exportCollection("speech_cas", "\\\\localnas\\home\\Drive\\Bundestag Mining\\19. Legislaturperiode Protokolle Data", handler.getDb());
            DatabaseExporter.exportCollection("speeches_annotations", "\\\\localnas\\home\\Drive\\Bundestag Mining\\19. Legislaturperiode Protokolle Data", handler.getDb());
            return;
        }

        if (handler.collectionDoesNotExist("Protocols")){
            ParliamentAPI api = new ParliamentAPI();
            ArrayList<Document> documents = api.getAllDocuments();
            XMLParser parser = new XMLParser(documents);
            PlenarySessionProtocol[] protocols = new PlenarySessionProtocol[documents.size()];
            for (int i = 0; i < documents.size(); i++){
                protocols[i] = parser.populatePlenarySessionProtocol(documents.get(i));
            }

            try {
                ArrayList<Deputy_Mongo> deputies_mongo = new ArrayList<>();
                Set<Deputy> deputies = parser.getSpeakers();
                NodeList nodeList = api.getMetaData().getElementsByTagName("MDB");
                for (Deputy deputy : deputies){
                    String academicTitle = "";
                    String historySince = "";
                    String birthDate = "";
                    String deathDate = "";
                    String gender = "";
                    String maritalStatus = "";
                    String religion = "";
                    String profession = "";
                    String party = "";

                    for (int i = 0; i < nodeList.getLength(); i++){
                        Element mdb = (Element) nodeList.item(i);
                        String id = mdb.getElementsByTagName("ID").item(0).getTextContent();

                        if (!deputy.getId().equals(id)) continue;

                        Node names = mdb.getElementsByTagName("NAMEN").item(0);
                        Element name = (Element) ((Element) names).getElementsByTagName("NAME").item(0);
                        Element biography = (Element) mdb.getElementsByTagName("BIOGRAFISCHE_ANGABEN").item(0);
                        academicTitle = name.getElementsByTagName("AKAD_TITEL").item(0).getTextContent();
                        historySince = name.getElementsByTagName("HISTORIE_VON").item(0).getTextContent();
                        birthDate = biography.getElementsByTagName("GEBURTSDATUM").item(0).getTextContent();
                        deathDate = biography.getElementsByTagName("STERBEDATUM").item(0).getTextContent();
                        gender = biography.getElementsByTagName("GESCHLECHT").item(0).getTextContent();
                        maritalStatus = biography.getElementsByTagName("FAMILIENSTAND").item(0).getTextContent();
                        religion = biography.getElementsByTagName("RELIGION").item(0).getTextContent();
                        profession = biography.getElementsByTagName("BERUF").item(0).getTextContent();
                        party = biography.getElementsByTagName("PARTEI_KURZ").item(0).getTextContent();
                    }
                    deputies_mongo.add(
                            new Deputy_Mongo(
                                    deputy,
                                    academicTitle,
                                    historySince,
                                    birthDate,
                                    deathDate,
                                    gender,
                                    maritalStatus,
                                    religion,
                                    profession,
                                    party
                            )
                    );
                }

                // We dont need saving of images anymore
                /*
                if (handler.collectionDoesNotExist("Deputies")){
                    GridFSBucket gridBucket = GridFSBuckets.create(handler.getDb());
                    GridFSUploadOptions uploadOptions = new GridFSUploadOptions().chunkSizeBytes(1024).metadata(
                            new org.bson.Document("type", "image").append("content_type", "image/jpeg")
                    );


                    int counter = 0;
                    int deputies_count = deputies.size();
                    for (Deputy d : deputies){
                        System.out.println("Downloading image " + counter + " / " + deputies_count);
                        counter++;
                        Thread.sleep(500);
                        String strUrl = api.getImageUrl(d.getFullName());
                        if (strUrl == null) continue;

                        URL url = new URL(strUrl);
                        BufferedImage img = ImageIO.read(url);
                        File file = new File("downloaded.jpg");
                        ImageIO.write(img, "jpg", file);

                        InputStream inStream = new FileInputStream(file);
                        ObjectId fileId = gridBucket.uploadFromStream(d.getId() + ".jpeg", inStream);

                        file.delete();
                    }
                }
                 */

                handler.insertProtocols(protocols, parser.getAllSpeeches(), deputies_mongo);
            } catch (JSONException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        //RestfulInsertNLP.insertNLP();
        RestfulAPI restfulapi = new RestfulAPI(handler.getDb());
        restfulapi.createRoutes();

    }
}