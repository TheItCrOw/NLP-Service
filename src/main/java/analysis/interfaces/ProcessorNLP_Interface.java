package analysis.interfaces;
/**
 * Author: Elias Wahl
 */

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public interface ProcessorNLP_Interface {
    public JSONObject processSpeech(int index, AnalysisEngine ae) throws JSONException, UIMAException;
    public void processAll() throws JSONException, UIMAException ;
    public JCas toCAS(String text) throws UIMAException ;
    public JSONArray processSegment(JSONArray segment, AnalysisEngine ae) throws JSONException, UIMAException ;
    public JSONObject processText(JCas text, AnalysisEngine ae, Boolean covered) throws JSONException, UIMAException;
}
