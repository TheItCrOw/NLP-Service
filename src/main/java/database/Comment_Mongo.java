/**
 * Holds a single comment (German: Kommentar).
 *
 * Currently only useful for holding shouts, as applauses are not used in any program functionality.
 *
 * @author: Gabriele
 */
package database;

import analysis.NLPSerializable;
import interfaces.Comment_Interface;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import relations.Comment;

import java.util.Collection;
import java.util.Iterator;

public class Comment_Mongo extends Comment implements NLPSerializable, Comment_Interface {

    public Comment_Mongo(Comment comment){
        super(comment.getText(), comment.getDeputy(), comment.getAgendaItem());
    }

    /**
     * Serialize a comment into a JSONObject.
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject byJSON = null;
        if (this.getDeputy() != null){
            byJSON = new Deputy_Mongo(this.getDeputy()).toJSONObject();
        }
        JSONObject agendaItemJSON = null;
        if (this.getAgendaItem() != null){
            agendaItemJSON = new AgendaItem_Mongo(this.getAgendaItem()).toJSONObject();
        }
        return new JSONObject()
                .put("text", this.getText())
                .put("by", byJSON)
                .put("agendaItem", agendaItemJSON);
    }
}
