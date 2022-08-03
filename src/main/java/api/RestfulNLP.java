/**
 * Provide content of Restful-NLP routes
 * @author: Linus
 */
package api;

import com.mongodb.client.model.Projections;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.bson.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.mongodb.client.MongoCursor;

import static api.RestfulHelpers.getResultJson;
import static com.mongodb.client.model.Filters.*;


public class RestfulNLP {

    /**
     * Get the result-content of a NLP-request
     */
    public static JSONObject getNLP(Integer limit, Date from, Date to, String nlp_type, String party, String fraction, String speakerid) throws ParseException {
        if(fraction != null && fraction.equalsIgnoreCase("Die Linke")) fraction = "DIE LINKE";
        HashMap<String, Integer> nlp_count = new HashMap<String, Integer>();
        HashMap<ArrayList<String>, Integer> entity_count= new HashMap<ArrayList<String>, Integer>();
        //entscheiden ob Ergebnis für einen einzelnen speaker oder für mehrere bereitgestellt werden soll
        if (speakerid == null) {
            HashMap[] not_speaker_maps = getCountNotSpeaker(nlp_count, entity_count, from, to, party, fraction, nlp_type);
            nlp_count = not_speaker_maps[0];
            entity_count = not_speaker_maps [1];
        }
        else {
            HashMap[] speaker_maps = getCountSpeaker(nlp_count, entity_count, from, to, nlp_type, speakerid);
            nlp_count = speaker_maps[0];
            entity_count = speaker_maps [1];
        }
        String total_response_string = "";
        if (nlp_type.equalsIgnoreCase("namedentities")) {
            total_response_string = getEntityResponseString(entity_count, limit);
        }
        else {
            total_response_string = getNLPResponseString(nlp_count, limit);
        }
        return getResultJson(total_response_string);
    }

    /**
     * Get all requested nlp-elements (key) and count (value) between to and from as a HashMap for a single-speaker-request
     */
    public static HashMap[] getCountSpeaker(HashMap nlp_count, HashMap entity_count, Date from, Date to, String nlp_type, String speakerid) throws ParseException {
        int[] protocol_borders = RestfulHelpers.getProtocolBorder(from, to);
        Integer min_lp = protocol_borders[0];
        Integer min_protocol = protocol_borders[1];
        Integer max_lp = protocol_borders[2];
        Integer max_protocol = protocol_borders[3];

        //richtigen Cursor anhand von protocol-border erstellen
        MongoCursor<Document> speechesnlp = null;
        if (min_lp == max_lp) {
            speechesnlp = RestfulAPI.getDb().getCollection("SpeechesNLP").find(and(eq("agendaItem.legislaturePeriod", min_lp), gte("agendaItem.protocol", min_protocol), lte("agendaItem.protocol", max_protocol), eq("speakerId", speakerid))).iterator();
        }
        else {
            speechesnlp = RestfulAPI.getDb().getCollection("SpeechesNLP").find(or(and(eq("agendaItem.legislaturePeriod", min_lp), gte("agendaItem.protocol", min_protocol)), and(eq("agendaItem.legislaturePeriod", max_lp), lte("agendaItem.protocol", max_protocol), eq("speakerId", speakerid)))).iterator();
        }
        //schauen, was nlp_type ist und davon abhängig die HashMap mit value count erstellen
        while (speechesnlp.hasNext()) {
            Document current_speechnlp = speechesnlp.next();
            Document current_result = (Document) current_speechnlp.get("result");
            if (nlp_type.equalsIgnoreCase("tokens") || nlp_type.equalsIgnoreCase("pos")) {
                nlp_count = getTokensCount(nlp_count, current_result, nlp_type);
            }
            else if (nlp_type.equalsIgnoreCase("sentiments")) {
                nlp_count = getSentimentsCount(nlp_count, current_result);
            }
            else if (nlp_type.equalsIgnoreCase("namedEntities")) {
                entity_count = getSpeakerEntitiesCount(entity_count, current_result);
            }
        }
        return new HashMap[] {nlp_count, entity_count};
    }

    /**
     * Get all requested nlp-elements (key) and count (value) between to and from as a HashMap for a multiple-speaker-request
     */
    public static HashMap[] getCountNotSpeaker(HashMap nlp_count, HashMap entity_count, Date from, Date to, String party, String fraction, String nlp_type) throws ParseException {
        MongoCursor<Document> all_annotations = RestfulAPI.getDb().getCollection("AnnotationEvaluation")
                .find()
                .projection(Projections.fields(Projections.include("date"), Projections.excludeId()))
                .iterator();
        if(party != null){
            party = party.replace(".", "\\U002e");
        }
        //aktuelles Document "current_annotation" wird abhängig ov party, fraction und nlp_type erstellt und anschließend mit diesem Objekt getEntitiyCount bzw. getNLPCount aufgerufen
        while (all_annotations.hasNext()) {
            Document current_annotation = all_annotations.next();
            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
            var rawDate = current_annotation.getString("date");
            Date current_date = sdf.parse(rawDate);
            if (from == null || ((current_date.after(from) || current_date.equals(from)) && (current_date.before(to) || current_date.equals(to)))) {
                 current_annotation = new Document();
                if(fraction != null){
                    current_annotation = RestfulAPI.getDb().getCollection("AnnotationEvaluation")
                            .find(eq("date", rawDate))
                            .projection(Projections.fields(Projections.include(nlp_type + ".fraction." + fraction), Projections.excludeId()))
                            .first();
                } else if(party != null){
                    current_annotation = RestfulAPI.getDb().getCollection("AnnotationEvaluation")
                            .find(eq("date", rawDate))
                            .projection(Projections.fields(Projections.include(nlp_type + ".party." + party), Projections.excludeId()))
                            .first();
                } else{
                    current_annotation = RestfulAPI.getDb().getCollection("AnnotationEvaluation")
                            .find(eq("date", rawDate))
                            .projection(Projections.fields(Projections.include(nlp_type + ".total"), Projections.excludeId()))
                            .first();
                }
                Document current_result = current_annotation.get(nlp_type, Document.class);
                ArrayList current_request_content = getCurrentRequestContent(current_result, party, fraction);
                if (nlp_type.equalsIgnoreCase("namedentities")) {
                    entity_count = getEntityCount(current_request_content, entity_count);
                }
                else {
                    nlp_count = getNLPCount(current_request_content, nlp_count);
                }
            }
        }
        return new HashMap[] {nlp_count, entity_count};
    }

    /**
     * Get the current requested ArrayList of the result-document
     */
    public static ArrayList getCurrentRequestContent(Document current_result, String party, String fraction) {
        Document total_fraction_party_layer = null;
        ArrayList current_request_content = null;
        if (party != null) {
            total_fraction_party_layer = (Document) current_result.get("party");
            current_request_content = total_fraction_party_layer.get(party, ArrayList.class);
        }
        else if (fraction != null) {
            total_fraction_party_layer = (Document) current_result.get("fraction");
            current_request_content = total_fraction_party_layer.get(fraction, ArrayList.class);
        }
        else {
            total_fraction_party_layer = (Document) current_result.get("total");
            current_request_content = total_fraction_party_layer.get("all", ArrayList.class);
        }
        return current_request_content;
    }

    /**
     * Update nlp-count HashMap by adding new key-value-pairs or increasing count-value
     */
    public static HashMap getNLPCount(ArrayList current_request_content, HashMap<String, Integer> nlp_count) {
        for (int i = 0; i < current_request_content.size(); i++) {
            Document current_array_element = (Document) current_request_content.get(i);
            String current_element = current_array_element.getString("element");
            int current_count = current_array_element.getInteger("count");
            nlp_count.putIfAbsent(current_element, 0);
            nlp_count.put(current_element, nlp_count.get(current_element) + current_count);
        }
        return nlp_count;

    }

    /**
     * Update entity-count HashMap by adding new key-value-pairs or increasing count-value
     */
    public static HashMap getEntityCount(ArrayList current_request_content, HashMap<ArrayList<String>, Integer> entity_count) {
        for (int i = 0; i < current_request_content.size(); i++) {
            Document current_entity_element = (Document) current_request_content.get(i);
            String current_element_name = current_entity_element.getString("element");
            int current_count = current_entity_element.getInteger("count");
            String current_type = current_entity_element.getString("type");
            ArrayList current_element_and_type = new ArrayList<>();
            current_element_and_type.add(current_element_name);
            current_element_and_type.add(current_type);
            entity_count.putIfAbsent(current_element_and_type, 0);
            entity_count.put(current_element_and_type, entity_count.get(current_element_and_type) + current_count);
        }

        return entity_count;
    }

    /**
     * Convert entity_count-HashMap into a response-string that can be converted into a json-object
     */
    public static String getEntityResponseString(HashMap<ArrayList<String>, Integer> entities, Integer limit) {
        String total_response_string = "[";

        for (int i = 0; i < limit; i++) {
            if (entities.size() == 0) {
                break;
            }
            Integer maxValueInMap = (Collections.max(entities.values()));  // This will return max value in the Hashmap
            //für HashMap einen response-String erstellen, der als JSONObject für die Route umgewandelt werden kann
            for (Map.Entry<ArrayList<String>, Integer> entry : entities.entrySet()) {  // Itrate through hashmap
                if (entry.getValue() == maxValueInMap) {
                    ArrayList<String> current_array = entry.getKey();
                    String current_element = current_array.get(0);
                    String current_type = current_array.get(1);
                    total_response_string = total_response_string + "{\"element\": " + "\"" + current_element + "\"" + ", \"count\": " + entry.getValue() + ", \"type\": " + "\"" + current_type + "\"" + "}, ";
                    entities.remove(entry.getKey());
                    break;
                }
            }
        }
        total_response_string += "]";
        return total_response_string;
    }

    /**
     * Convert nlp_count-HashMap into a response-string that can be converted into a json-object
     */
    public static String getNLPResponseString(HashMap<String, Integer> nlp_count, Integer limit) {
        String total_response_string = "[";
        for (int i = 0; i < limit; i++) {
            if (nlp_count.size() == 0) {
                break;
            }
            Integer maxValueInMap = (Collections.max(nlp_count.values()));  // This will return max value in the Hashmap
            //für HashMap einen response-String erstellen, der als JSONObject für die Route umgewandelt werden kann
            for (Map.Entry<String, Integer> entry : nlp_count.entrySet()) {  // Itrate through hashmap
                if (entry.getValue() == maxValueInMap) {
                    total_response_string = total_response_string + "{\"element\": " + "\"" + entry.getKey() + "\"" + ", \"count\": " + entry.getValue() + "}, ";
                    nlp_count.remove(entry.getKey());
                    break;
                }
            }
        }
        total_response_string += "]";
        return total_response_string;
    }


    /**
     * Get count of tokens as HashMap
     */
    public static HashMap getTokensCount(HashMap<String, Integer> nlp_count, Document current_result, String nlp_type) {
        ArrayList current_request = current_result.get("tokens", ArrayList.class);
        //tokens in nlp_count hinzufügen oder count inkrementieren
        for (int i = 0; i < current_request.size(); i++) {
            Document current_request_element = (Document) current_request.get(i);
            String current_element = "";
            if (nlp_type.equalsIgnoreCase("tokens")) {
                current_element = current_request_element.getString("lemmaValue");
            }
            else {
                current_element = current_request_element.getString("posValue");
            }
            nlp_count.putIfAbsent(current_element, 0);
            nlp_count.put(current_element, nlp_count.get(current_element) + 1);
        }
        return nlp_count;
    }

    /**
     * Get count of sentiments as HashMap
     */
    public static HashMap getSentimentsCount(HashMap<String, Integer> nlp_count, Document current_result) {
        ArrayList current_request = current_result.get("sentiments", ArrayList.class);
        //sentiment hinzufügen oder inkrementieren
        for (int i = 0; i < current_request.size(); i++) {
            ArrayList current_request_element = (ArrayList) current_request.get(i);
            Document current_request_sentiment = (Document) current_request_element.get(0);
            Double current_sentiment_dbl = Double.valueOf(current_request_sentiment.getString("sentimentSingleScore"));
            String current_sentiment = "";
            if (current_sentiment_dbl < 0) {
                current_sentiment = "negativ";
            }
            else if (current_sentiment_dbl > 0) {
                current_sentiment = "positiv";
            }
            else {
                current_sentiment = "neutral";
            }
            nlp_count.putIfAbsent(current_sentiment, 0);
            nlp_count.put(current_sentiment, nlp_count.get(current_sentiment) + 1);
        }
        return nlp_count;
    }

    /**
     * Get count of speaker-entities as HashMap
     */
    public static HashMap getSpeakerEntitiesCount(HashMap<ArrayList<String>, Integer> entity_count, Document current_result) {
        ArrayList current_entities = current_result.get("namedEntities", ArrayList.class);
        ArrayList current_tokens = current_result.get("tokens", ArrayList.class);
        //entity hinzufügen, falls noch nicht enthalten, sonst inkrementieren
        //dafür vorher alls Tokens herausfinden, die zur entity gehören und alle zusammen als ein String abspeicheren
        for (int i = 0; i < current_entities.size(); i++) {
            Document current_entity_element = (Document) current_entities.get(i);
            Integer current_begin = current_entity_element.getInteger("begin");
            Integer current_end = current_entity_element.getInteger("end");
            String total_token_values = "";
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
            entity_count.putIfAbsent(token_entity, 0);
            entity_count.put(token_entity, entity_count.get(token_entity) + 1);
        }
        return entity_count;
    }
}