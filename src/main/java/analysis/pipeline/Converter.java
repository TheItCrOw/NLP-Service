package analysis.pipeline;
/**
 * Author: Elias Wahl
 */


import analysis.interfaces.Converter_Interface;
import au.com.bytecode.opencsv.CSVReader;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hucompute.textimager.uima.type.Sentiment;
import org.hucompute.textimager.uima.type.category.CategoryCoveredTagged;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import java.util.Arrays;
import java.util.List;

/**
 * class to convert the NLP result to JSONObjects
 */
public class Converter implements Converter_Interface {
    /**
     * Converts tokens to JSONObject
     *
     * @param token as Token
     * @return jo a JSONObject
     */
    public JSONObject tokenToJSON(Token token) {
        JSONObject jo = new JSONObject();
        try {
            jo.put("begin", token.getBegin());
            jo.put("end", token.getEnd());
            jo.put("lemmaValue", token.getLemmaValue());
            if (token.getStem() != null) {
                jo.put("stem", token.getStem().getValue());
            } else {
                jo.put("stem", "<null>");
            }
            jo.put("posValue", token.getPosValue());

            MorphologicalFeatures morph = token.getMorph();
            if (morph != null) {
                jo.put("morph", "true");
                jo.put("gender", morph.getGender());
                jo.put("number", morph.getNumber());
                jo.put("case", morph.getCase());
                jo.put("degree", morph.getDegree());
                jo.put("verbForm", morph.getVerbForm());
                jo.put("tense", morph.getTense());
                jo.put("mood", morph.getMood());
                jo.put("voice", morph.getVoice());
                jo.put("definiteness",morph.getDefiniteness());
                jo.put("value", morph.getValue());
                jo.put("person", morph.getPerson());
                jo.put("aspect", morph.getAspect());
                jo.put("animacy", morph.getAnimacy());
                jo.put("negative", morph.getNegative());
                jo.put("possessive", morph.getPossessive());
                jo.put("pronType", morph.getPronType());
                jo.put("reflex", morph.getReflex());
                jo.put("transitivity", morph.getTransitivity());
            } else {
                jo.put("morph", "false");
            }

            jo.put("id", token.getId());
            jo.put("syntacticFunction", token.getSyntacticFunction());
        } catch(JSONException e) {
            e.printStackTrace();
        }
        return jo;
    }

    /**
     * Converts named entities to JSONObjects
     *
     * @param entity as NamedEntity
     * @return jo as JSONObject
     */
    public JSONObject entitiesToJSON(NamedEntity entity) {
        JSONObject jo = new JSONObject();
        try {
            jo.put("begin", entity.getBegin());
            jo.put("end", entity.getEnd());
            jo.put("value", entity.getValue());
        } catch(JSONException e) {
            e.printStackTrace();
        }
        return jo;
    }

    /**
     * Converts CategoryCoveredTagged to JSONObject
     *
     * @param category as CategoryCoveredTagged
     * @return jo as JSONObject
     */
    public JSONObject categoryToJSON(CategoryCoveredTagged category) {
        JSONObject jo = new JSONObject();
        try{
            if (category != null) {
                String value = "";

                URL url = Thread.currentThread().getContextClassLoader().getResource("ddc3-names-de.csv");
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                CSVReader reader = new CSVReader(in);


                int number = Integer.parseInt(category.getValue().substring(13));

                List<String[]> allRows = reader.readAll();
                for (String[] row : allRows) {
                    if (Arrays.toString(row).length() > 3) {
                        int index = Integer. parseInt(Arrays.toString(row).substring(1, 4));
                        if (index == number) {
                            value = (Arrays.toString(row).substring(5));
                            value = value.substring(0, value.length()-1);
                        }
                    }
                }

                jo.put("begin", category.getBegin());
                jo.put("end", category.getEnd());
                jo.put("value", value);
                jo.put("score", category.getScore());
            }

        } catch (IOException | NullPointerException | JSONException e) {
            e.printStackTrace();
        }
        return jo;
    }

    /**
     * Converts sentiment to JSONObject
     * @param sentiment as Sentiment
     * @return jo as JSONObject
     */
    public JSONObject sentimentToJSON(Sentiment sentiment) {
        JSONObject jo = new JSONObject();
        try {
            jo.put("begin", sentiment.getBegin());
            jo.put("end", sentiment.getEnd());
            jo.put("sentimentSingleScore", String.valueOf(sentiment.getSentiment()));
        } catch(JSONException e) {
            e.printStackTrace();
        }
        return jo;
    }
}
