/**
 * Provide content for /speaker-route of RestfulAPI
 */
package api;

import com.mongodb.DB;
import com.mongodb.MongoGridFSException;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import org.bson.Document;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import spark.Request;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static api.RestfulHelpers.getResultJson;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Filters.eq;

public class RestfulSpeakers {

    /**
     * Get speaker-portrait of given speakerid
     */
    public static byte[] getSpeakerPortrait(String speakerid) throws IOException {
        GridFSBucket gridBucket = GridFSBuckets.create(RestfulAPI.getDb());
        FileOutputStream fileOutputStream = new FileOutputStream(speakerid + ".jpeg");
        try {
            gridBucket.downloadToStream(speakerid + ".jpeg", fileOutputStream);
        } catch (MongoGridFSException exc) {
            File f = new File(speakerid + ".jpeg");
            f.delete();
            return new byte[0];
        } finally {
            fileOutputStream.close();
        }

        File f = new File(speakerid + ".jpeg");
        BufferedImage image = ImageIO.read(f);
        // https://stackoverflow.com/questions/49090627/how-to-return-image-using-a-get-route-on-spark-java-so-it-displays-on-the-webpa
        byte[] rawImage = null;
        try(ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            ImageIO.write( image, "jpg", stream );
            stream.flush();
            rawImage = stream.toByteArray();
        }
        f.delete();
        return rawImage;
    }

    /**
     * Get speaker by id
     */
    public static JSONObject getSpeakerById(String speakerid) {
        Document speaker = RestfulAPI.getDb().getCollection("Deputies").find(eq("id", speakerid)).first();
        String speaker_string = "[" + speaker.toJson() + "]";
        JSONArray speaker_array = new JSONArray(speaker_string);
        JSONObject speaker_result = new JSONObject();
        speaker_result.put("result", speaker_array);
        speaker_result.put("success", true);
        return speaker_result;
    }

    /**
     * Get all speakers
     */
    public static JSONObject getAllSpeakers() {
        String speakers_string = "[";
        MongoCursor<Document> cursor_speakers = RestfulAPI.getDb().getCollection("Deputies").find().iterator();
        while (cursor_speakers.hasNext()) {
            speakers_string = speakers_string + cursor_speakers.next().toJson() + ", ";
        }
        speakers_string = speakers_string + "]";
        JSONArray speakers_array = new JSONArray(speakers_string);
        JSONObject speakers_result = new JSONObject();
        speakers_result.put("result", speakers_array);
        speakers_result.put("success", true);
        return speakers_result;
    }

    /**
     * Get only limit speakers with the largest number of speeches
     */
    public static JSONObject getLimitSpeakers(Integer limit, Date from, Date to, String party, String fraction) throws ParseException {

        HashMap<String, Integer> speaker_count = RestfulHelpers.getSpeakercount(RestfulHelpers.getProtocolBorder(from, to), party, fraction);
        List<String> keys_list = new ArrayList<>(speaker_count.keySet());
        List<Integer> values_list = new ArrayList<>(speaker_count.values());
        String total_response_string = "[";

        //für maximal limit Durchläufe das max-Element der HashMap bestimmen und key-value-pair in total-response-string einfügen
        //total-response-string kann dann als JSONObject für routen-Ergebnis bereitgestellt werden
        for (int i = 0; i < limit; i++) {
            if (speaker_count.size() == 0) {
                break;
            }
            Integer maxValueInMap = (Collections.max(speaker_count.values()));  // This will return max value in the Hashmap
            try {
                for (Map.Entry<String, Integer> entry : speaker_count.entrySet()) {  // Itrate through hashmap
                    if (entry.getValue() == maxValueInMap) {
                        Integer current_index = values_list.indexOf(entry.getValue());
                        String current_key = keys_list.get(current_index);
                        //Ausnahme für null-speakerId, sodass diese übersprungen wird
                        try{
                            Document speaker = RestfulAPI.getDb().getCollection("Deputies").find(eq("id", current_key)).first();
                            String current_speaker = speaker.toJson();
                            current_speaker = current_speaker.replaceFirst(".$","");
                            current_speaker = current_speaker + ", " + "\"speakerCount\": " + entry.getValue() + "}, ";
                            total_response_string = total_response_string + current_speaker;
                        }
                        catch (Exception e){
                            i -= 1;
                        }
                        speaker_count.remove(entry.getKey());
                        break;
                    }
                }
            }
            catch (Exception e) {
                System.out.println(e);
                break;
            }
        }
        total_response_string = total_response_string + "]";
        return getResultJson(total_response_string);
    }
}
