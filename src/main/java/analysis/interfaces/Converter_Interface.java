package analysis.interfaces;
/**
 * Author: Elias Wahl
 */

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.codehaus.jettison.json.JSONObject;
import org.hucompute.textimager.uima.type.category.CategoryCoveredTagged;
import org.hucompute.textimager.uima.type.Sentiment;

public interface Converter_Interface {
    public JSONObject tokenToJSON(Token token);
    public JSONObject entitiesToJSON(NamedEntity entity);
    public JSONObject categoryToJSON(CategoryCoveredTagged category);
    public JSONObject sentimentToJSON(Sentiment sentiment);
}
