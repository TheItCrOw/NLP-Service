/**
 * Hold a single speech (German: Rede) with the accompanying text, speaker, agenda item and shouts.
 * @author: Gabriele
 */
package database;

import analysis.NLPSerializable;
import interfaces.Speech_Interface;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import relations.Speech;
import relations.SpeechSegment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


public class Speech_Mongo extends Speech implements NLPSerializable, Speech_Interface {

    public Speech_Mongo(Speech speech){
        super(speech.getSegments(), speech.getSpeakerId(), speech.getAgendaItem());
    }

    /**
     * Serialize a speech into a JSONObject.
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject segmentsJSONObject = new JSONObject();
        ArrayList<SpeechSegment> segments = this.getSegments();
        for (int i = 0; i < segments.size(); i++){
            segmentsJSONObject.append(Integer.toString(i), new SpeechSegment_Mongo(segments.get(i)).toJSONObject());
        }

        return new JSONObject()
                .put("text", this.getText())
                .put("agendaItem", new AgendaItem_Mongo(this.getAgendaItem()).toJSONObject())
                .put("speakerId", this.getSpeakerId())
                .put("segments", segmentsJSONObject);
    }
}
