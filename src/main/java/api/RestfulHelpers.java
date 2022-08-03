/**
 * Manage important helper-methods
 * @author: Linus
 */
package api;
import com.mongodb.client.MongoCursor;
import org.bson.Document;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import com.google.common.base.Strings;
import org.json.JSONArray;
import org.json.simple.JSONObject;

import static com.mongodb.client.model.Filters.*;
public class RestfulHelpers {

    /**
     * Get Border of protocols (interval between min and max protocol)
     */
    public static int[] getProtocolBorder(Date from, Date to) throws ParseException {
        Integer min_lp = -1;
        Integer min_protocol = -1;
        Integer max_lp = -1;
        Integer max_protocol = -1;

        MongoCursor<Document> cursor_protocols = RestfulAPI.getDb().getCollection("Protocols").find().iterator();
        Boolean to_found = false;
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
        //fängt von hinten an (die neusten Dokumente werden zuerst bearbeitet)
        //wenn das aktuelle Datum bei from angekommen ist, dann wird abgebrochen
        //sonst wird in jedem Durchlauf min_lp und min_protocol auf das aktuelle Protokoll gesetzt
        while (cursor_protocols.hasNext()) {
            Document current_document = cursor_protocols.next();
            Date current_date = sdf.parse(String.valueOf(current_document.get("date")));
            if (to_found) {
                if (current_date.compareTo(from) < 0) {
                    break;
                }
            }
            else {
                if (current_date.compareTo(to) <= 0) {
                    max_lp = Integer.valueOf(String.valueOf(current_document.get("legislaturePeriod")));
                    max_protocol = Integer.valueOf(String.valueOf(current_document.get("number")));
                    to_found = true;
                }
            }
            min_lp = Integer.valueOf(String.valueOf(current_document.get("legislaturePeriod")));
            min_protocol = Integer.valueOf(String.valueOf(current_document.get("number")));
        }

        return new int[] {min_lp, min_protocol, max_lp, max_protocol};
    }

    /**
     * Get all Pos (key) and count (value) between borders as a HashMap
     */
    public static HashMap<String, Integer> getPoscount(int[] protocol_borders, String party, String fraction, String speakerid) {
        Integer min_lp = protocol_borders[0];
        Integer min_protocol = protocol_borders[1];
        Integer max_lp = protocol_borders[2];
        Integer max_protocol = protocol_borders[3];

        MongoCursor<Document> cursor_limit_pos = null;
        if (min_lp == max_lp) {
            cursor_limit_pos = RestfulAPI.getDb().getCollection("SpeechesNLP").find(and(eq("agendaItem.legislaturePeriod", min_lp), gte("agendaItem.protocol", min_protocol), lte("agendaItem.protocol", max_protocol))).iterator();
        }
        else {
            cursor_limit_pos = RestfulAPI.getDb().getCollection("SpeechesNLP").find(or(and(eq("agendaItem.legislaturePeriod", min_lp), gte("agendaItem.protocol", min_protocol)), and(eq("agendaItem.legislaturePeriod", max_lp), lte("agendaItem.protocol", max_protocol)))).iterator();
        }
        HashMap<String, Integer> pos_count = new HashMap<String, Integer>();
        List valid_speakerids = RestfulHelpers.getValidSpeakerids(party, fraction, speakerid);
        //wenn die spakerid von der aktuellen Rede gültig ist, dann wird das pos_count in die HashMap pos_count hinzugefügt oder count inkrementiert
        while (cursor_limit_pos.hasNext()) {
            Document current_document = cursor_limit_pos.next();
            String current_speakerid = current_document.getString("speakerId");
            if (valid_speakerids.contains(current_speakerid)) {
                Document current_result = current_document.get("result", Document.class);
                ArrayList current_pos = current_result.get("categoryCoveredTagged", ArrayList.class);
                for (int i = 0; i < current_pos.size(); i++) {
                    Document current_pos_element = (Document) current_pos.get(i);
                    String current_value = current_pos_element.getString("value");
                    pos_count.putIfAbsent(current_value, 0);
                    pos_count.put(current_value, pos_count.get(current_value) + 1);
                }
            }
        }
        return pos_count;
    }


    /**
     * Get all Speakers (key) and count (value) between borders as a HashMap
     */
    public static HashMap<String, Integer> getSpeakercount(int[] protocol_borders, String party, String fraction) {
        System.out.println("Start");
        Integer min_lp = protocol_borders[0];
        Integer min_protocol = protocol_borders[1];
        Integer max_lp = protocol_borders[2];
        Integer max_protocol = protocol_borders[3];

        //MongoCursor in Abhängigkeit der protocol_borders erstellen
        MongoCursor<Document> cursor_limit_speakers = null;
        if (min_lp == max_lp) {
            cursor_limit_speakers = RestfulAPI.getDb().getCollection("Speeches").find(and(eq("agendaItem.legislaturePeriod", min_lp), gte("agendaItem.protocol", min_protocol), lte("agendaItem.protocol", max_protocol))).iterator();
        }
        else {
            cursor_limit_speakers = RestfulAPI.getDb().getCollection("Speeches").find(or(and(eq("agendaItem.legislaturePeriod", min_lp), gte("agendaItem.protocol", min_protocol)), and(eq("agendaItem.legislaturePeriod", max_lp), lte("agendaItem.protocol", max_protocol)))).iterator();
        }
        HashMap<String, Integer> speaker_count = new HashMap<String, Integer>();
        List valid_speakerids = RestfulHelpers.getValidSpeakerids(party, fraction, null);
        //wenn die spakerid von der aktuellen Rede gültig ist, dann wird die speakerid in die HashMap speaker_count hinzugefügt oder der Wert inkrementiert
        while (cursor_limit_speakers.hasNext()) {
            Document current_document = cursor_limit_speakers.next();
            String current_speakerid = current_document.getString("speakerId");
            if (valid_speakerids.contains(current_speakerid)) {
                speaker_count.putIfAbsent(current_speakerid, 0);
                speaker_count.put(current_speakerid, speaker_count.get(current_speakerid) + 1);
            }
        }

        return speaker_count;
    }


    /**
     * Get all valid speaker-ids (valid party, valid fraction or valid speakerid)
     */
    public static List getValidSpeakerids(String party, String fraction, String speakerid) {
        MongoCursor<Document> cursor_deputies = RestfulAPI.getDb().getCollection("Deputies").find().iterator();
        List valid_speakerids = new ArrayList();
        while (cursor_deputies.hasNext()) {
            Document current_document = cursor_deputies.next();
            String current_speakerid = current_document.getString("id");
            //speakerid is valid if party of speaker equals party of fraction of speaker equals fraction or speakerid of speaker equals speakerid
            if (party != null) {
                String current_party = current_document.getString("party");
                if (Strings.isNullOrEmpty(current_party)) {
                    current_party = "parteilos";
                }
                if (current_party.equalsIgnoreCase(party)) {
                    valid_speakerids.add(current_speakerid);
                }
            }
            else if (fraction != null) {
                String current_fraction = "";
                current_fraction = current_document.getString("fraction");
                if (Strings.isNullOrEmpty(current_fraction)) {
                    current_fraction = "fraktionslos";
                }
                if (current_fraction.equalsIgnoreCase(fraction)) {
                    valid_speakerids.add(current_speakerid);
                }
            }
            else if (speakerid != null) {
                if (current_speakerid.equals(speakerid)) {
                    valid_speakerids.add(current_speakerid);
                }
            }
            else {
                valid_speakerids.add(current_speakerid);
            }
        }

        return valid_speakerids;
    }

    /**
     * Convert a String into JSONObject for the route
     */
    public static org.json.simple.JSONObject getResultJson(String total_response_string) {
        JSONArray total_response_array = new JSONArray(total_response_string);
        org.json.simple.JSONObject total_response = new JSONObject();
        total_response.put("result", total_response_array);
        total_response.put("success", true);
        return total_response;
    }
}
