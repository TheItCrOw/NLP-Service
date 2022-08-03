/**
 * Hold a single speech (German: Rede) with the accompanying text, speaker, agenda item and shouts.
 * @author: Gabriele
 */
package database;

import analysis.NLPSerializable;
import interfaces.SpeechSegment_Interface;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import relations.Comment;
import relations.Speech;
import relations.SpeechSegment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


public class SpeechSegment_Mongo extends SpeechSegment implements NLPSerializable, SpeechSegment_Interface {

    public SpeechSegment_Mongo(SpeechSegment segment){
        super(segment.getText(), segment.getShouts());
    }

    /**
     * Serialize a speech segment into a JSONObject.
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject cumulatedCommentsJSON = new JSONObject();
        ArrayList<Comment> comments = this.getShouts();
        for (int i = 0; i < comments.size(); i++){
            cumulatedCommentsJSON.append(Integer.toString(i), new Comment_Mongo(comments.get(i)).toJSONObject());
        }

        return new JSONObject()
                .put("text", this.getText())
                .put("shouts", cumulatedCommentsJSON);
    }
}
