/**
 * Hold a single speech (German: Rede) with the accompanying text, speaker, agenda item and shouts.
 * @author: Gabriele
 */
package relations;

import interfaces.SpeechSegment_Interface;

import java.util.ArrayList;

public class SpeechSegment implements SpeechSegment_Interface {
    String text;
    ArrayList<Comment> shouts;

    public SpeechSegment(String text, ArrayList<Comment> shouts){
        this.text = text;
        this.shouts = shouts;
    }

    /**
     * Get the text of the speech segment
     */
    public String getText(){
        return this.text;
    }

    /**
     * Get all comments of a speech segment
     */
    public ArrayList<Comment> getShouts() { return this.shouts; }
}
