package analysis.pipeline;
/**
 * Author: Elias Wahl
 */


import analysis.interfaces.Engine_Interface;
import com.hazelcast.nio.ClassLoaderUtil;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;
import org.hucompute.textimager.uima.gervader.GerVaderSentiment;
import org.hucompute.textimager.uima.spacy.SpaCyMultiTagger3;
import org.hucompute.textimager.fasttext.labelannotator.LabelAnnotatorDocker;

import java.net.URL;
import java.util.Objects;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

/**
 * Initializes an AnalysisEngine
 */
public class Engine implements Engine_Interface {
    /**
     * Configures and returns an analysisEngine
     *
     * @return AnalysisEngine
     */
    public AnalysisEngine initEngine() {
        //  creating a Pipeline
        AggregateBuilder pipeline = new AggregateBuilder();

        // add different Engines to the Pipeline
        try {
            // "http://spacy.prg2021.texttechnologylab.org"
            pipeline.add(createEngineDescription(SpaCyMultiTagger3.class,
                    SpaCyMultiTagger3.PARAM_REST_ENDPOINT, "http://127.0.0.1:8101"
            ));

            //String sPOSMapFile = Engine.class.getClassLoader().getResource("am_posmap.txt").getPath();
            String sPOSMapFile = "/home/bm-nlp-pipeline/Desktop/Bundestags-Mine/BundestagMine/JAVA/src/resources/am_posmap.txt";
            /*pipeline.add(createEngineDescription(LabelAnnotatorDocker.class,
                    LabelAnnotatorDocker.PARAM_FASTTEXT_K, 100,
                    LabelAnnotatorDocker.PARAM_CUTOFF, false,
                    LabelAnnotatorDocker.PARAM_SELECTION, "text",
                    LabelAnnotatorDocker.PARAM_TAGS, "ddc3",
                    LabelAnnotatorDocker.PARAM_USE_LEMMA, true,
                    LabelAnnotatorDocker.PARAM_ADD_POS, true,
                    LabelAnnotatorDocker.PARAM_POSMAP_LOCATION, sPOSMapFile,
                    LabelAnnotatorDocker.PARAM_REMOVE_FUNCTIONWORDS, true,
                    LabelAnnotatorDocker.PARAM_REMOVE_PUNCT, true,
                    LabelAnnotatorDocker.PARAM_REST_ENDPOINT, "http://127.0.0.1:8303"
            ));*/
            // http://ddc.prg2021.texttechnologylab.org

            pipeline.add(createEngineDescription(GerVaderSentiment.class,
                    GerVaderSentiment.PARAM_REST_ENDPOINT, "http://127.0.0.1:8202",
                    GerVaderSentiment.PARAM_SELECTION, "text,de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence"
            ));
            //http://gervader.prg2021.texttechnologylab.org

            // create an AnalysisEngine for running the Pipeline.
            return pipeline.createAggregate();
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }
        return null;
    }
}
