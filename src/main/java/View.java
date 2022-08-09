import java.io.*;
import java.text.ParseException;
import java.util.*;

//import api.RestfulAPI;
//import api.RestfulGeneral;
import analysis.pipeline.Engine;
import analysis.pipeline.ProcessorNLP;
//import org.apache.commons.lang.StringEscapeUtils;
import database.MssqlDbConnectionHandler;
import database.Speech_Mongo;
import org.codehaus.jettison.json.JSONObject;
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
            //var newestProtocol = db.getLatestProtocolNumber(20);
            var newestProtocol = 47;
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

            // This returns the speakers of the 'rednerliste' of each protocol
            // We build the deputies with their metadata here of each new protocol. We decide later whether we want to import
            // the deputy or if its already imported.
            var deputies = parser.getSpeakers();
            var nodeList = api.getMetaData().getElementsByTagName("MDB");
            var deputies_mongo = parser.parseDeputiesToMongoDeputies(deputies, nodeList);

            // Parse the speeches to the protocols.
            var speeches = parser.getAllSpeeches();

            // Now run the protocols through the nlp pipeline.
            // Create the engine
            var engine = new Engine();
            var pipeline = engine.initEngine();
            var processor = new ProcessorNLP();
            // Process each protocol at a time
            for(int i = 0; i < protocols.length; i++){
                var curProtocol = protocols[i];
                // Get the speeches of the protocol
                var speechesOfProtocol = speeches.stream()
                        .filter(s -> s.getAgendaItem().getProtocol().getNumber() == curProtocol.getNumber()
                                && s.getAgendaItem().getProtocol().getLegislaturePeriod() == curProtocol.getLegislaturePeriod())
                        .toList();

                // analyse them
                ArrayList<JSONObject> nlpSpeechesOfProtocol = new ArrayList<>();
                for(int k =0; k < speechesOfProtocol.size(); k++){
                    var curSpeech = speechesOfProtocol.get(k);
                    var nlpSpeech = processor.processSpeech(new Speech_Mongo(curSpeech).toJSONObject(), pipeline);
                    nlpSpeechesOfProtocol.add(nlpSpeech);
                }
                // Store the imported protocol in the mssql db.
                db.insertImportedProtocol(curProtocol, nlpSpeechesOfProtocol);
                var xd = "";
            }

            // Insert the protocol here
            // TODO: Check this. I dont know how the getAllSpeeches and deputies_mogno play in here.
            //handler.insertProtocols(protocols, parser.getAllSpeeches(), deputies_mongo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}