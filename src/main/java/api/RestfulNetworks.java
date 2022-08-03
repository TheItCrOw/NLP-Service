/**
 * Provide the data we need for the comment network graph as json
 * @author: Kevin
 */

package api;

import api.datamodels.CommentLink_REST;
import api.datamodels.CommentNetwork_REST;
import api.datamodels.DeputyNode_REST;
import com.google.gson.Gson;
import org.bson.Document;

import javax.print.Doc;

import static com.mongodb.client.model.Filters.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class RestfulNetworks {

    private static boolean writeDataToJsonFile = false;
    /***
     * Gets the data we need for the comment network graph as json
     * @return
     */
    public static String getCommentNetworkData(){
        var db = RestfulAPI.getDb();
        // We go through each speech. Check the speaker. Then we check the shouts
        // Who is shouting at who? An whats the sentiment.
        var cursorNlpSpeeches = db.getCollection("SpeechesNLP").find().limit(1000).iterator();
        var network = new CommentNetwork_REST();

        while(cursorNlpSpeeches.hasNext()){
            try{
                // Get the current speech
                var curSpeech = cursorNlpSpeeches.next();
                // The speaker of the speech
                var speaker = db.getCollection("Deputies").find(eq("id", curSpeech.getString("speakerId"))).first();
                if(speaker == null) continue;

                var speakerId = speaker.getString("id");
                // Add the speaker to the network if he doesnt exist already
                if(!network.getNodes().stream().anyMatch(n -> n.getId().equals(speakerId))) {
                    var deputyNode = new DeputyNode_REST();
                    deputyNode.setId(speakerId);
                    deputyNode.setName(speaker.getString("firstName") + ' ' + speaker.getString("lastName"));
                    deputyNode.setParty(speaker.getString("party"));
                    network.getNodes().add(deputyNode);
                }

                // Check the shouts of the speech.
                // Check the segments
                var segments = curSpeech.get("segments", Document.class);

                for(int i = 0; i < segments.size(); i++){
                    // Get the segments
                    var segment = (Document)segments.get(Integer.toString(i), ArrayList.class).get(0);
                    // Get the shouts of each segment
                    var shouts = segment.get("shouts", Document.class);

                    for(var k = 0; k < shouts.size(); k++){
                        var shout = (Document)shouts.get(Integer.toString(k), ArrayList.class).get(0);
                        var by = shout.get("by", Document.class);

                        // Not all shouts are with the shouting speaker.
                        if(by == null) continue;

                        var shoutingSpeakerId = by.getString("id");
                        // This is the shouting Deputy
                        var shouter = db.getCollection("Deputies").find(eq("id", shoutingSpeakerId)).first();

                        if(shouter == null) continue;
                        double sentimentScore = 0.0;
                        // Get the sentiment values of this shout
                        var sentimentsList = ((Document)segment.get("result", ArrayList.class).get(0))
                                                .get("sentiments", ArrayList.class);
                        var sentiments = (ArrayList)sentimentsList.get(0);
                        // Add the total score
                        for(var s = 0; s < sentiments.size(); s++){
                            var sentiment = (Document)sentiments.get(s);
                            try{
                                var score = sentiment.getString("sentimentSingleScore");
                                sentimentScore = sentimentScore + Double.parseDouble(score);
                            } catch(Exception ex){
                                System.out.println("Couldn't parse sentiment score: " + sentiment.toJson());
                            }
                        }
                        // Here we have the total sentiment score of that one shout.
                        // We now know who shouted at whom at which score. Add this link to the network
                        // Does this shouter has already shouten at this exact speaker before?
                        var existingLinkList = network.getLinks().stream().filter(
                                l -> l.getSource().equals(shoutingSpeakerId) && l.getTarget().equals(speakerId)).toList();

                        // If a link already exists, then just update the existing link
                        if(existingLinkList != null && existingLinkList.size() > 0){
                            var existingLink = existingLinkList.get(0);
                            var newScore = existingLink.getSentiment() + sentimentScore;
                            existingLink.setSentiment(newScore);
                            var newBinding = existingLink.getValue() + 1;
                            existingLink.setValue(newBinding);
                        } else {
                            // Check if the shouter already has a node. If not, we must create it
                            if(!network.getNodes().stream().anyMatch(n -> n.getId().equals(shoutingSpeakerId))){
                                var shouterNode = new DeputyNode_REST();
                                shouterNode.setId(shoutingSpeakerId);
                                shouterNode.setName(shouter.getString("firstName") + ' ' + shouter.getString("lastName"));
                                shouterNode.setParty(shouter.getString("party"));
                                network.getNodes().add(shouterNode);
                            }

                            // Else create the link
                            var link = new CommentLink_REST();
                            link.setSentiment(sentimentScore);
                            link.setSource(shoutingSpeakerId);
                            link.setTarget(speakerId);
                            link.setValue(1);
                            network.getLinks().add(link);
                        }
                    }
                }
            } catch(Exception ex){
                System.out.println(ex);
            }
        }

        var gson = new Gson();
        // Lets store the json to file
        if(writeDataToJsonFile){
            try{
                var fileWriter = new FileWriter("E:\\Informatik B.Sc\\3. Semester\\Programmierpraktikum\\Übung 4\\commentNetworkFull.json");
                var printWriter = new PrintWriter(fileWriter);
                printWriter.printf(gson.toJson(network));
                printWriter.close();
            } catch(Exception ex){

            }
        }
        return gson.toJson(network);
    }
}
