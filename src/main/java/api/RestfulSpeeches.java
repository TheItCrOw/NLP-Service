/**
 * Provide content that has to do with /speeches or /nlpspeeches
 */

package api;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import static api.RestfulHelpers.getResultJson;
import static com.mongodb.client.model.Filters.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;


public class RestfulSpeeches {

    /**
     * Get Speeches as JSONObject of an agendaitem
     */
    public static org.json.simple.JSONObject getSpeechesOfAgendaItem(Integer period, Integer protocol, Integer number) {
        MongoCursor<Document> cursor_speeches = RestfulAPI.getDb().getCollection("Speeches").find(and(eq("agendaItem.protocol", protocol), eq("agendaItem.legislaturePeriod", period), eq("agendaItem.number", number))).iterator();
        String speeches_string = "[";
        //add speeches into String that can be converted to JSONObject
        while (cursor_speeches.hasNext()) {
            var speech = cursor_speeches.next();
            var date = getProtocolDateBySpeech(
                    speech.get("agendaItem", Document.class).getInteger("legislaturePeriod"),
                    speech.get("agendaItem", Document.class).getInteger("protocol"));
            speech.put("date", date);
            speeches_string = speeches_string + speech.toJson() + ", ";
        }
        speeches_string = speeches_string + "]";
        return getResultJson(speeches_string);
    }

    /**
     * Get a NLPSpeech by speech-id
     */
    public static org.json.simple.JSONObject getNLPSpeechById(String id) {
        ObjectId speechid = new ObjectId(id);
        Document nlpspeech = RestfulAPI.getDb().getCollection("SpeechesNLP").find(eq("_id", speechid)).first();
        var date = getProtocolDateBySpeech(
                nlpspeech.get("agendaItem", Document.class).getInteger("legislaturePeriod"),
                nlpspeech.get("agendaItem", Document.class).getInteger("protocol"));
        nlpspeech.put("date", date);
        String nlpspeech_string = "[" + nlpspeech.toJson() + "]";
        return getResultJson(nlpspeech_string);
    }

    //http://localhost:4567/speeches?q=ksdfjhh

    /**
     * Get all speeches that contain a searchterm (text or speaker)
     */
    public static org.json.simple.JSONObject getSpeechBySearchterm(String searchterm) {
        // Gather the ids of the speaker
        MongoCursor<Document> valid_speakers = RestfulAPI.getDb().getCollection("Deputies")
                .find(or(
                        in("firstName", searchterm),
                        in("lastName", searchterm),
                        in("firstName" + "lastName", searchterm.replaceAll(" ", ""))
                        )).iterator();

        List<String> valid_speakerids = new ArrayList<>();
        while (valid_speakers.hasNext()) {
            var speakerId = valid_speakers.next().getString("id");
            valid_speakerids.add(speakerId);
        }

        //get all speeches from the db where the text contains the searchterm or the speaker
        MongoCursor<Document> cursor_speeches = RestfulAPI.getDb().getCollection("Speeches")
                .find(or(
                        regex("text", ".*" + Pattern.quote(searchterm) + ".*", "i"),
                        new BasicDBObject("speakerId", new BasicDBObject("$in", valid_speakerids)))
                ).iterator();
        String speeches_string = "[";
        //String mit allen speeches erstellen, der als JSONObject für die Route convertiert werden kann
        while (cursor_speeches.hasNext()) {
            try {
                var speech = cursor_speeches.next();
                var date = getProtocolDateBySpeech(
                        speech.get("agendaItem", Document.class).getInteger("legislaturePeriod"),
                        speech.get("agendaItem", Document.class).getInteger("protocol"));
                speech.put("date", date);
                speeches_string += speech.toJson() + ", ";
            }
            catch (Exception e) {
                assert false;
            }
        }
        speeches_string = speeches_string + "]";
        return getResultJson(speeches_string);
    }

    /**
     * Get the Date of a Protocol
     */
    private static String getProtocolDateBySpeech(int period, int prot){
        try{
            var protocol = RestfulAPI.getDb().getCollection("Protocols").find(
                    (and(
                            eq("legislaturePeriod", period),
                            eq("number", prot)
                    ))).iterator();
            if(protocol.hasNext()){
                var date = protocol.next().getString("date");
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("E MMM d H:m:s O u", Locale.ENGLISH);
                OffsetDateTime odt = OffsetDateTime.parse(date, dtf);
                return odt.toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            }
            return null;
        } catch(Exception ex){
            return null;
        }
    }
}