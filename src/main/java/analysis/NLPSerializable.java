package analysis;
/**
 * Author: Gabriele Marcantonio
 */

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public interface NLPSerializable {
    /**
     * In order to be NLPSerializable, an object must implement the following methods:
     * - toJSONObject(): this is because, prior to being serializable, the objects are put into the mongodb
     * - toCAS(): we use JCas objects for extracting all the annotations
     */
    public JSONObject toJSONObject() throws JSONException;

}
