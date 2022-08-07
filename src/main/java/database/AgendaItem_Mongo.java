/**
 * Manage agenda items (German: Tagesordnungspunkte)
 * @author: Gabriele
 */
package database;

import interfaces.AgendaItem_Interface;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import parliament.AgendaItem;

public class AgendaItem_Mongo extends AgendaItem implements AgendaItem_Interface {

    public AgendaItem_Mongo(AgendaItem item){
        super(item.getProtocol(), item.getNumber(), item.getName());
    }

    /**
     * Serialize an agenda item into a JSONObject.
     */
    public JSONObject toJSONObject() throws JSONException {
        return new JSONObject()
                .put("protocol", this.getProtocol().getNumber())
                .put("name", this.getName())
                .put("legislaturePeriod", this.getLegislaturePeriod())
                .put("number", this.getNumber());
    }
}
