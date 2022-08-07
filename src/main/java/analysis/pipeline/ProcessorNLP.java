package analysis.pipeline;
/**
 * Author: Elias Wahl
 */


import analysis.interfaces.ProcessorNLP_Interface;
import com.mongodb.MongoSocketWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import database.MongoDBConnectionHandler;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.bson.Document;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hucompute.textimager.uima.type.category.CategoryCoveredTagged;
import org.hucompute.textimager.uima.type.Sentiment;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Processes all speeches and pushes the result to the DB
 */
public class ProcessorNLP {
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RESET = "\u001B[0m";
    ArrayList<JSONObject> col = new ArrayList<>();

    /**
     * Processes a Speech and injects the result into the given JSONObject
     *
     * @param ae as AnalysisEngine
     * @return speechJSON as JSONObject
     * @throws JSONException .
     * @throws UIMAException .
     */
    public JSONObject processSpeech(JSONObject speechJSON, AnalysisEngine ae) throws JSONException, UIMAException {
        //text to json
        JSONObject s =  speechJSON.getJSONObject("segments");
        JSONArray segments = s.toJSONArray(s.names());

        if (segments != null) {
            for (int i = 0; i < segments.length(); i++) {
                JSONArray resultsShout = processSegment(segments.getJSONArray(i), ae);
                speechJSON.getJSONObject("segments").getJSONArray(String.valueOf(i)).getJSONObject(0).put("result", resultsShout);
            }
        }

        //process the whole text
        JCas textCAS = toCAS((String) speechJSON.get("text"));
        JSONObject resultsText = processText(textCAS, ae, true);
        speechJSON.put("result", resultsText);
        return speechJSON;
    }

    /**
     * Gets all speeches from db and iterated over all of them.
     * Each speech and its result is then being pushed to the db
     *
     * @throws JSONException .
     * @throws UIMAException .
     */
    public void processAll() throws JSONException, UIMAException {
        try {
            MongoDBConnectionHandler handler = new MongoDBConnectionHandler();

            System.out.println(ANSI_GREEN + "Fetching documents..." + ANSI_RESET);
            MongoDatabase db = handler.getDb();
            MongoCollection<Document> d = db.getCollection("Speeches");

            ArrayList<Document> docs = new ArrayList<>();
            d.find().into(docs);

            for (Document doc : docs) {
                col.add(new JSONObject(doc.toJson()));
            }

            if (handler.collectionDoesNotExist("SpeechesNLP")) {
                db.createCollection("SpeechesNLP");
            }
            //get count of processed speeches already in collection
            int resumeCount = handler.getCountItemsInCollection("SpeechesNLP");

            System.out.println(ANSI_GREEN + "Creating engine..." + ANSI_RESET);
            Engine engine = new Engine();
            AnalysisEngine ae = engine.initEngine();

            for (int i = resumeCount; i < this.col.size(); i++) {
                System.out.println(ANSI_GREEN + "Processing Speech: " + i + " from " + this.col.size() + ANSI_RESET);
                // Process here
                //JSONObject jo = processSpeech(i, ae);
                //handler.pushNLPResultToDB(jo);
            }

        } catch (MongoSocketWriteException| JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a JCas object from given text
     *
     * @param text as String
     * @return jCas as JCas
     * @throws UIMAException .
     */
    public JCas toCAS(String text) throws UIMAException {
        JCas jCas = JCasFactory.createJCas();
        jCas.setDocumentLanguage("de");
        jCas.setDocumentText(text);
        return jCas;
    }

    /**
     * Processes the shouts within a segment.
     *
     * @param segment as JSONArray
     * @param ae as AnalysisEngine
     * @return result as JSONArray
     * @throws JSONException .
     * @throws UIMAException .
     */
    public JSONArray processSegment(JSONArray segment, AnalysisEngine ae) throws JSONException, UIMAException {
        JSONArray resultShouts = new JSONArray();
        try {
            if (segment.getJSONObject(0).getJSONObject("shouts").length() != 0) {
                JSONObject sh = segment.getJSONObject(0).getJSONObject("shouts");
                JSONArray shouts = sh.toJSONArray(sh.names());

                for (int i=0; i < shouts.length(); i++) {
                    JSONObject shO = shouts.getJSONArray(i).getJSONObject(0);
                    JCas textShout = toCAS((String) shO.get("text"));
                    JSONObject resultShout = processText(textShout, ae, false);
                    resultShouts.put(i, resultShout);
                }
            }
        } catch (AnalysisEngineProcessException e) {
            e.printStackTrace();
        }
        return resultShouts;
    }

    /**
     * Processes a string such as the text of a shout or text of an actual speech.
     *
     * @param text as JCas
     * @param ae as AnalysisEngine
     * @param covered as Boolean
     * @return result as JSONObject
     * @throws JSONException .
     * @throws UIMAException .
     */
    public JSONObject processText(JCas text, AnalysisEngine ae, Boolean covered) throws JSONException, UIMAException{
        JSONObject nlp = new JSONObject();
        //process text
        SimplePipeline.runPipeline(text, ae);

        //fill containers with results
        Collection<Sentence> sentences = JCasUtil.select(text, Sentence.class);
        ArrayList<ArrayList<Sentiment>> sentiments = new ArrayList<>();
        Collection<Token> tokens = JCasUtil.select(text, Token.class);
        Collection<NamedEntity> entities = JCasUtil.select(text, NamedEntity.class);

        for (Sentence s : sentences) {
            sentiments.add(new ArrayList<>(JCasUtil.selectCovered(Sentiment.class, s)));
        }
        Converter converter = new Converter();

        if (covered) {
            //getCategory
            Collection<CategoryCoveredTagged> categories = JCasUtil.select(text, CategoryCoveredTagged.class);
            //Category to JSON:
            ArrayList<CategoryCoveredTagged> c = new ArrayList<>(categories);
            JSONArray jsonCategories = new JSONArray();
            for (CategoryCoveredTagged category : c) {
                JSONObject jo = converter.categoryToJSON(category);
                jsonCategories.put(jo);
            }
            nlp.put("categoryCoveredTagged", jsonCategories);
        }

        //convert all objects to JSON for storing later
        //Token to JSON:
        ArrayList<Token> t = new ArrayList<>(tokens);
        JSONArray jsonTokens = new JSONArray();
        for (Token token : t) {
            JSONObject jo = converter.tokenToJSON(token);
            jsonTokens.put(jo);
        }

        //NamedEntity to JSON:
        ArrayList<NamedEntity> n = new ArrayList<>(entities);
        JSONArray jsonEntities = new JSONArray();
        for (NamedEntity entity : n) {
            JSONObject jo = converter.entitiesToJSON(entity);
            jsonEntities.put(jo);
        }

        //Sentiment to JSON:
        JSONArray jsonSentiments = new JSONArray();
        for (ArrayList<Sentiment> s : sentiments) {
            JSONArray jsonArray = new JSONArray();
            for (Sentiment sent : s) {
                JSONObject jo = converter.sentimentToJSON(sent);
                jsonArray.put(jo);
            }
            jsonSentiments.put(jsonArray);
        }
        nlp.put("namedEntities", jsonEntities);
        nlp.put("tokens", jsonTokens);
        nlp.put("sentiments", jsonSentiments);

        return nlp;
    }

    /**
     * To execute the nlp process.
     * @param args .
     * @throws JSONException .
     * @throws UIMAException .
     */
    public static void main(String[]args) throws JSONException, UIMAException {
        ProcessorNLP processor = new ProcessorNLP();
        processor.processAll();
    }
}
