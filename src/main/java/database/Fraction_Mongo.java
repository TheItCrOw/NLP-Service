/**
 * Manage fractions (German: Fraktionen)
 * @author: Gabriele
 */
package database;

import interfaces.Fraction_Interface;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import parliament.Fraction;

public class Fraction_Mongo extends Fraction implements Fraction_Interface {

    public Fraction_Mongo(Fraction fraction){
        super(fraction.getName());
    }

    /**
     * Serialize a fraction into a JSONObject.
     */
    public JSONObject toJSONObject() throws JSONException {
        return new JSONObject().put("name", this.getName());
    }
}