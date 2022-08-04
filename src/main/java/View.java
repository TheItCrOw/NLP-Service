import java.io.*;
import java.text.ParseException;
import java.util.*;

//import api.RestfulAPI;
//import api.RestfulGeneral;
import database.Deputy_Mongo;
//import org.apache.commons.lang.StringEscapeUtils;
import database.MssqlDbConnectionHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import parliament.PlenarySessionProtocol;
import parsing.XMLParser;
import people.Deputy;
import parliamentapi.ParliamentAPI;


public class View {
    static Set<Deputy> speakers;
    static boolean exportDatabase = false;

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, ParseException, InterruptedException, org.json.simple.parser.ParseException {
        try {
            // Establish connection to mssql database
            System.out.println("Trying to connect to mssql database...");
            var db = new MssqlDbConnectionHandler();
            if (!db.init()) {
                System.out.println("Couldn't connect to database. Aborting.");
                return;
            }
            System.out.println("Connected!");

            // Fetch new protocols.
            System.out.println("Looking for new protocols...");
            var newestProtocol = db.getLatestProtocolNumber(20);
            System.out.println("Newest stored protocol is Protocol " + newestProtocol);
            var api = new ParliamentAPI();
            var documents = api.fetchNewProtocols(newestProtocol);
            System.out.println("Found " + documents.size() + " new protocols.");
            if (documents.size() == 0) {
                System.out.println("Found no new protocols. Aborting.");
                return;
            }

            // Now parse them
            System.out.println("Trying to parse the new protocols...");
            var parser = new XMLParser(documents);
            var protocols = new PlenarySessionProtocol[documents.size()];
            for (int i = 0; i < documents.size(); i++) {
                protocols[i] = parser.populatePlenarySessionProtocol(documents.get(i));
            }

            ArrayList<Deputy_Mongo> deputies_mongo = new ArrayList<>();
            Set<Deputy> deputies = parser.getSpeakers();
            NodeList nodeList = api.getMetaData().getElementsByTagName("MDB");
            for (Deputy deputy : deputies) {
                String academicTitle = "";
                String historySince = "";
                String birthDate = "";
                String deathDate = "";
                String gender = "";
                String maritalStatus = "";
                String religion = "";
                String profession = "";
                String party = "";

                for (int i = 0; i < nodeList.getLength(); i++) {
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
                deputies_mongo.add(new Deputy_Mongo(deputy, academicTitle, historySince, birthDate, deathDate, gender, maritalStatus, religion, profession, party));
            }

            // Insert the protocol here
            // TODO: Check this. I dont know how the getAllSpeeches and deputies_mogno play in here.
            var speeches = parser.getAllSpeeches();
            //handler.insertProtocols(protocols, parser.getAllSpeeches(), deputies_mongo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}