/**
 * Manage Restful-API routes
 * @author Linus
 */
package api;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

import spark.Filter;

import static spark.Spark.after;
import static spark.Spark.get;



public class RestfulAPI {

    private static MongoDatabase db;

    /**
     * Constructor
     */
    public RestfulAPI(MongoDatabase db) {
        this.db = db;
    }

    /**
     * Get the MongoDatabase
     */
    public static MongoDatabase getDb() {
        return db;
    }

    /**
     * Create the Routes
     */
    public void createRoutes() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        JSONObject error_result = new JSONObject();
        error_result.put("message: ", "Leider konnte zu der angefragten URL kein Ergebnis gefunden werden.");
        error_result.put("success", false);

        after((Filter) (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET");
        });

        //localhost:4567/speakers?limit=20&from=12-01-2020&to=12-01-2021&party=cdu&fraction=cdu/csu

        get("/speakers", (req, res)-> {
            res.type("application/json");
            String id = req.queryParams("id");
            if (id != null) {
                return RestfulSpeakers.getSpeakerById(id);
            }
            else {
                String party = req.queryParams("party");
                String fraction = req.queryParams("fraction");
                try {
                    Integer limit = Integer.valueOf(req.queryParams("limit"));
                    Date from = sdf.parse(req.queryParams("from"));
                    Date to = sdf.parse(req.queryParams("to"));
                    //return error_result;
                    return RestfulSpeakers.getLimitSpeakers(limit, from, to, party, fraction);
                }
                catch (Exception e){
                    System.out.println("ok");
                    return RestfulSpeakers.getAllSpeakers();
                }
            }
        });

        get("/fractions", (req, res)-> {
            res.type("application/json");
            return RestfulGeneral.getFractions();
        });

        get("/parties", (req, res)-> {
            res.type("application/json");
            return RestfulGeneral.getParties();
        });

        get("/protocols", (req, res)-> {
            res.type("application/json");
            return RestfulGeneral.getProtocols();
        });


        get("/commentNetworkData", (req, res)-> {
            res.type("application/json");
            try{
                var data = getDb().getCollection("NetworkData").find().first().toJson();
                return data;
            } catch(Exception ex){
                var data = RestfulNetworks.getCommentNetworkData();
                return data;
            }
        });

        get("/speechesofagendaitem", (req, res)-> {
            res.type("application/json");
            try {
                Integer period = Integer.valueOf(req.queryParams("period"));
                Integer protocol = Integer.valueOf(req.queryParams("protocol"));
                Integer number = Integer.valueOf(req.queryParams("number"));
                return RestfulSpeeches.getSpeechesOfAgendaItem(period, protocol, number);
            }
            catch (Exception e) {
                return error_result;
            }
        });

        get("/nlpspeechbyid", (req, res)-> {
            res.type("application/json");
            try {
                return RestfulSpeeches.getNLPSpeechById(req.queryParams("id"));
            }
            catch (Exception e) {
                return error_result;
            }
        });

        get("/speeches", (req, res)-> {
            res.type("application/json");
            return RestfulSpeeches.getSpeechBySearchterm(req.queryParams("searchterm"));
        });

        get("/speakerportait", (req, res)-> {
            res.type("image/jpeg");
            return RestfulSpeakers.getSpeakerPortrait(req.queryParams("speakerid"));
        });


        //wenn party oder fraction nicht übergeben werden soll, dann einfach nicht in die URL schreiben
        //"localhost:4567/nlp?limit=20&from=2021-11-05&to=2021-12-01&nlptype=pos&party=null&fraction=null&speakerid=12345678"
        get("/nlp", (req, res)-> {
            res.type("application/json");
            int limit;
            try {
                limit = Integer.valueOf(req.queryParams("limit"));
            }
            catch (Exception e) {
                limit = 20;
            }
            return RestfulNLP.getNLP(limit, sdf.parse(req.queryParams("from")), sdf.parse(req.queryParams("to")), req.queryParams("nlptype"), req.queryParams("party"), req.queryParams("fraction"), req.queryParams("speakerid"));
        });

    }


}
