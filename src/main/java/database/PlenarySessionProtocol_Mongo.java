/**
 * Manage the plenary session protocol (German: Plenarsitzungsprotokoll)
 * @author: Gabriele
 */
package database;
import interfaces.PlenarySessionProtocol_Interface;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import parliament.PlenarySessionProtocol;

public class PlenarySessionProtocol_Mongo extends PlenarySessionProtocol implements PlenarySessionProtocol_Interface {

    public PlenarySessionProtocol_Mongo(PlenarySessionProtocol protocol){
        super(protocol.getDate(), protocol.getLegislaturePeriod(), protocol.getNumber(), protocol.getTitle(), protocol.getAgendaItemsCount());
    }

    /**
     * Serialize a plenary session protocol into a JSONObject.
     */
    public JSONObject toJSONObject() throws JSONException {
        return new JSONObject()
                .put("date", this.getDate())
                .put("legislaturePeriod", this.getLegislaturePeriod())
                .put("number", this.getNumber())
                .put("title", this.getTitle())
                .put("agendaItemsCount", this.getAgendaItemsCount());
    }
}