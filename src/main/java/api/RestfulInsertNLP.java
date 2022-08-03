/**
 * Insert NLP-Speeches-result per Protocol in MongoDB ("AnnotationEvaluation") in order to make requests faster
 * @author: Linus
 */

package api;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.types.ObjectId;
import spark.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.*;

import static com.mongodb.client.model.Filters.*;

public class RestfulInsertNLP {

    public static MongoCursor<Document> getCurrentSpeechesNLP(Integer lp, Integer number) {
        return RestfulAPI.getDb().getCollection("SpeechesNLP").find(and(eq("agendaItem.legislaturePeriod", lp), eq("agendaItem.protocol", number))).iterator();
    }


    public static void insertNLP() {
        MongoCollection<Document> annotationevaluationCol = RestfulAPI.getDb().getCollection("AnnotationEvaluation");
        ArrayList<Document> documentList = new ArrayList<Document>();
        MongoCursor<Document> cursor_protocols = RestfulAPI.getDb().getCollection("Protocols").find().batchSize(500).iterator();
        System.out.println("Start inserting NLP into Collection AnnotationEvaluation");

        //alle bereits in die MongoDB eingefügten Protokoll-Ids abspeichern, damit neue Protokolle in die MongoDB eingefügt werden, ohne, dass alte nochmal eingefügt werden
        List already_added_documents = new ArrayList<ObjectId>();
        MongoCursor<Document> added_annotation = RestfulAPI.getDb().getCollection("AnnotationEvaluation").find().iterator();
        while (added_annotation.hasNext()) {
            Document current_annotation = added_annotation.next();
            already_added_documents.add(current_annotation.getObjectId("protid"));
        }

        //Protokolle in "AnnotationEvaluation" einfügen
        while (cursor_protocols.hasNext()) {
            Document current_protocol = cursor_protocols.next();
            if (!already_added_documents.contains(current_protocol.getObjectId("_id"))) {
                Integer current_legislatureperiod = current_protocol.getInteger("legislaturePeriod");
                Integer current_number = current_protocol.getInteger("number");

                //nulltes Layer Document
                Document result_protocol = new Document();
                result_protocol.put("protid", current_protocol.getObjectId("_id"));
                result_protocol.put("date", current_protocol.getString("date"));
                ArrayList<String> content = new ArrayList<String>(Arrays.asList("pos", "tokens", "sentiments", "namedentities"));
                for (int i = 0; i < content.size(); i++) {

                    //erstes layer "nlp" (z.B. pos, tokens, sentiments oder namedentities)
                    DBObject nlp_all = new BasicDBObject();
                    //zweites layer getrennt nach party, fraction und total
                    DBObject nlp_party = getPartyFractionLayer(current_legislatureperiod, current_number, "party", content.get(i));
                    DBObject nlp_fraction = getPartyFractionLayer(current_legislatureperiod, current_number, "fraction", content.get(i));
                    DBObject nlp_total = new BasicDBObject();
                    ArrayList<DBObject> nlp_count_all = getDBContent(current_legislatureperiod, current_number, RestfulHelpers.getValidSpeakerids(null, null, null), content.get(i));
                    nlp_total.put("all", nlp_count_all);
                    //in das Datenbank-Objekt nlp_all die entsprechenden Teil-Objekte einfügen
                    nlp_all.put("total", nlp_total);
                    nlp_all.put("party", nlp_party);
                    nlp_all.put("fraction", nlp_fraction);
                    result_protocol.put(content.get(i), nlp_all);
                }
                annotationevaluationCol.insertOne(result_protocol);
            }
        }
        System.out.println("Finished inserting POS into Collection AnnotationEvaluation");
    }

    /**
     * Creates the party-fraction layer
     * Example: AnnotationEvaluation -> Object -> pos -> party
     */
    public static DBObject getPartyFractionLayer(Integer lp, Integer number, String mode, String content) {
        //MongoCursor<Document> current_speechesnlp_jz = getCurrentSpeechesNLP(lp, number);
        DBObject nlp_party_fraction = new BasicDBObject();
        ArrayList<String> parties = new ArrayList<String>(Arrays.asList("LKR", "FDP", "CSU", "DIE LINKE.", "CDU", "Parteilos", "SPD", "Die PARTEI", "BÜNDNIS 90/DIE GRÜNEN", "AfD"));
        ArrayList<String> fractions = new ArrayList<String>(Arrays.asList("DIE LINKE", "Bündnis 90 / Die Grünen", "FDP", "fraktionslos", "SPD", "AfD", "CDU/CSU"));
        //je nachdem, ob mode party oder fraction ist, wird party-layer bzw. fraction-layer erstellt
        if (mode.equalsIgnoreCase("party")) {
            for (int i = 0; i < parties.size(); i++) {
                ArrayList<DBObject> pos_count_party = getDBContent(lp, number, RestfulHelpers.getValidSpeakerids(parties.get(i), null, null), content);
                nlp_party_fraction.put(parties.get(i).replace(".", "\\u002e"), pos_count_party);
            }
        }
        else if (mode.equalsIgnoreCase("fraction")) {
            for (int i = 0; i < fractions.size(); i++) {
                ArrayList<DBObject> pos_count_fraction = getDBContent(lp, number, RestfulHelpers.getValidSpeakerids(null, fractions.get(i), null), content);
                nlp_party_fraction.put(fractions.get(i), pos_count_fraction);
            }
        }
        return nlp_party_fraction;
    }

    /**
     * Creates the "all"-layer
     * Structure: AnnotationEvaluation -> Object -> pos -> total -> all
     */
    public static ArrayList<DBObject> getDBContent(Integer lp, Integer number, List valid_speakerids, String content) {
        MongoCursor<Document> current_speechesnlp = getCurrentSpeechesNLP(lp, number);

        HashMap<String, Integer> content_count = new HashMap<String, Integer>();
        HashMap<ArrayList<String>, Integer> namedentities_count = new HashMap<ArrayList<String>, Integer>();
        //um total-content zu erstellen wird je nach "content" das entsprechende layer erstellt
        while (current_speechesnlp.hasNext()) {
            Document current_speechnlp = current_speechesnlp.next();
            String current_speakerid = current_speechnlp.getString("speakerId");
            if (valid_speakerids.contains(current_speakerid)) {
                Document current_result = current_speechnlp.get("result", Document.class);
                if (content.equalsIgnoreCase("pos")) {
                    content_count = updateContentCountPos(content_count, current_result);
                }
                else if (content.equalsIgnoreCase("tokens")) {
                    content_count = updateContentCountTokens(content_count, current_result);
                }
                else if (content.equalsIgnoreCase("namedentities")) {
                    namedentities_count = updateNamedEntitiesCount(namedentities_count, current_result);
                }
                else if (content.equalsIgnoreCase("sentiments")) {
                    content_count = updateContentCountSentiments(content_count, current_result);
                }
            }
        }

        ArrayList<DBObject> nlp_result = new ArrayList<DBObject>();
        //durch HashMap von namedentities iterieren um ArrayList von Datenbank-Objekten zu erstellen, die in die MongoDB eingefügt werden kann
        if (content.equalsIgnoreCase("namedentities")) {
            nlp_result = getNamedEntitiesResult(namedentities_count);
        }
        //durch HashMap von content_count iterieren um ArrayList von Datenbank-Objekten zu erstellen, die in die MongoDB eingefügt werden kann
        else {
            nlp_result = getNLPResult(content_count);
        }
        return nlp_result;
    }

    /**
     * Get the result as ArrayList for a namedentities-layer
     * Example: AnnotationEvaluation -> Object -> namedentities -> total -> all
     */
    public static ArrayList<DBObject> getNamedEntitiesResult(HashMap<ArrayList<String>, Integer> namedentities_count) {
        ArrayList<DBObject> nlp_result = new ArrayList<DBObject>();
        for (Map.Entry<ArrayList<String>, Integer> entry : namedentities_count.entrySet()) {  // Itrate through hashmap
            ArrayList<String> current_key = entry.getKey();
            if (!StringUtils.isBlank(current_key.get(0))) {
                DBObject current_nlp = new BasicDBObject();
                current_nlp.put("element", current_key.get(0));
                current_nlp.put("count", entry.getValue());
                current_nlp.put("type", current_key.get(1));
                nlp_result.add(current_nlp);
            }
        }
        return nlp_result;
    }

    /**
     * Get the result as ArrayList for a nlp-layer
     * Example: AnnotationEvaluation -> Object -> pos -> total -> all
     * Necessary because ArrayList must contain different values than getNamedEntitiesResult
     */
    public static ArrayList<DBObject> getNLPResult(HashMap<String, Integer> content_count) {
        ArrayList<DBObject> nlp_result = new ArrayList<DBObject>();
        for (Map.Entry<String, Integer> entry : content_count.entrySet()) {  // Itrate through hashmap

            if (!StringUtils.isBlank(entry.getKey())) {
                DBObject current_nlp = new BasicDBObject();
                current_nlp.put("element", entry.getKey());
                current_nlp.put("count", entry.getValue());
                nlp_result.add(current_nlp);
            }
        }
        return nlp_result;
    }

    /**
     * Update the content_count-HashMap by adding sentiment-content of current_result-Document
     */
    public static HashMap<String, Integer> updateContentCountSentiments(HashMap<String, Integer> content_count, Document current_result) {
        ArrayList current_sentiments = current_result.get("sentiments", ArrayList.class);
        for (int i = 0; i < current_sentiments.size(); i++) {

            ArrayList current_sentiment_element = (ArrayList) current_sentiments.get(i);

            Document current_sentiment_element_sentiment = (Document) current_sentiment_element.get(0);
            String current_value = current_sentiment_element_sentiment.getString("sentimentSingleScore");
            String current_compare = "neutral";
            //sentiment hinzufügen bzw. inkrementieren
            try{
                float current_value_nb = Float.parseFloat(current_value);
                if (current_value_nb < 0) {
                    current_compare = "negativ";
                }
                else if (current_value_nb > 0) {
                    current_compare = "positiv";
                }
            }
            catch (Exception e) {
                System.out.println("Sentiment Score ist ungültig.");
            }
            content_count.putIfAbsent(current_compare, 0);
            content_count.put(current_compare, content_count.get(current_compare) + 1);
        }
        return content_count;
    }

    /**
     * Update the namedentities_count-HashMap by adding entity-content of current_result-Document
     */
    public static HashMap<ArrayList<String>, Integer> updateNamedEntitiesCount(HashMap<ArrayList<String>, Integer> namedentities_count, Document current_result) {
        ArrayList current_entities = current_result.get("namedEntities", ArrayList.class);
        ArrayList current_tokens = current_result.get("tokens", ArrayList.class);
        for (int i = 0; i < current_entities.size(); i++) {
            Document current_entity_element = (Document) current_entities.get(i);
            Integer current_begin = current_entity_element.getInteger("begin");
            Integer current_end = current_entity_element.getInteger("end");
            String total_token_values = "";
            //namedentity jeweils hinzufügen bzw. inkrementieren
            for (int i2 = 0; i2 < current_tokens.size(); i2++) {
                Document current_token = (Document) current_tokens.get(i2);
                Integer current_token_begin = current_token.getInteger("begin");
                Integer current_token_end = current_token.getInteger("end");
                if (current_token_begin >= current_begin) {
                    if (current_token_end > current_end) {
                        break;
                    }
                    else {
                        String current_token_value = current_token.getString("lemmaValue");
                        total_token_values = total_token_values + current_token_value + " ";
                    }
                }
                else {
                    current_tokens.remove(i2);
                }
            }
            ArrayList<String> token_entity = new ArrayList<String>();
            token_entity.add(total_token_values);
            token_entity.add(current_entity_element.getString("value"));
            namedentities_count.putIfAbsent(token_entity, 0);
            namedentities_count.put(token_entity, namedentities_count.get(token_entity) + 1);
        }
        return namedentities_count;
    }

    /**
     * Update the namedentities_count-HashMap by adding token-content of current_result-Document
     */
    public static HashMap<String, Integer> updateContentCountTokens(HashMap<String, Integer> content_count, Document current_result) {
        ArrayList current_tokens = current_result.get("tokens", ArrayList.class);
        for (int i = 0; i < current_tokens.size(); i++) {
            Document current_token_element = (Document) current_tokens.get(i);
            String current_lemmavalue = current_token_element.getString("lemmaValue");
            content_count.putIfAbsent(current_lemmavalue, 0);
            content_count.put(current_lemmavalue, content_count.get(current_lemmavalue) + 1);
        }
        return content_count;
    }

    /**
     * Update the content_count-HashMap by adding pos-content of current_result-Document
     */
    public static HashMap<String, Integer> updateContentCountPos(HashMap<String, Integer> content_count, Document current_result) {
        ArrayList current_tokens = current_result.get("tokens", ArrayList.class);
        for (int i = 0; i < current_tokens.size(); i++) {
            Document current_token_element = (Document) current_tokens.get(i);
            String current_posvalue = current_token_element.getString("posValue");
            content_count.putIfAbsent(current_posvalue, 0);
            content_count.put(current_posvalue, content_count.get(current_posvalue) + 1);
        }
        return content_count;
    }
}
