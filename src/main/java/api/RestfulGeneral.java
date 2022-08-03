/**
 * Manage content for general routes
 * @author: Linus
 */
package api;
import api.RestfulAPI;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import database.MongoDBConnectionHandler;
import org.bson.Document;
import org.json.JSONArray;
import org.json.simple.JSONObject;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.util.HashMap;

import static api.RestfulHelpers.getResultJson;

public class RestfulGeneral {

    /**
     * Get fractions
     */
    public static JSONObject getFractions() {

        MongoCursor<Document> cursor_speakers = RestfulAPI.getDb().getCollection("Deputies").find().iterator();

        HashMap<String, Integer> fraction_count = new HashMap<String, Integer>();
        while (cursor_speakers.hasNext()) {
            String current_fraction_name = cursor_speakers.next().getString("fraction");
            if (current_fraction_name == null) {
                current_fraction_name = "fraktionslos";
            }
            fraction_count.putIfAbsent(current_fraction_name, 0);
            fraction_count.put(current_fraction_name, fraction_count.get(current_fraction_name) + 1);
        }
        String string_fraction = "[";
        for (Map.Entry<String, Integer> entry : fraction_count.entrySet()) {
            String current_fraction = "{" + "\"members\": " + entry.getValue() + ", " + "\"id\": " + "\"" + entry.getKey() + "\"" + "}, ";
            string_fraction = string_fraction + current_fraction;
        }
        string_fraction = string_fraction + "]";
        return getResultJson(string_fraction);

    }

    /**
     * Get parties
     */
    public static JSONObject getParties() {

        MongoCursor<Document> cursor_speakers = RestfulAPI.getDb().getCollection("Deputies").find().iterator();

        HashMap<String, Integer> party_count = new HashMap<String, Integer>();
        while (cursor_speakers.hasNext()) {
            String current_party_name = cursor_speakers.next().getString("party");
            if (current_party_name == null || current_party_name.length() == 0) {
                current_party_name = "Plos";
            }
            party_count.putIfAbsent(current_party_name, 0);
            party_count.put(current_party_name, party_count.get(current_party_name) + 1);
        }
        String string_party = "[";
        for (Map.Entry<String, Integer> entry : party_count.entrySet()) {
            String current_fraction = "{" + "\"members\": " + entry.getValue() + ", " + "\"id\": " + "\"" + entry.getKey() + "\"" + "}, ";
            string_party = string_party + current_fraction;
        }
        string_party = string_party + "]";
        return getResultJson(string_party);

    }

    /**
     * Get protocols
     */
    public static JSONObject getProtocols() {
        String protocols_string = "[";
        MongoCursor<Document> cursor_protocols = RestfulAPI.getDb().getCollection("Protocols").find().iterator();
        while (cursor_protocols.hasNext()) {
            var protocol = cursor_protocols.next();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("E MMM d H:m:s O u", Locale.ENGLISH);
            OffsetDateTime odt = OffsetDateTime.parse(protocol.getString("date"), dtf);
            var parsedDate = odt.toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            protocol.put("date", parsedDate);
            protocols_string = protocols_string + protocol.toJson() + ", ";
        }
        protocols_string = protocols_string + "]";
        return getResultJson(protocols_string);
    }
}
