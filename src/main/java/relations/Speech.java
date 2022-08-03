/**
 * Hold a single speech (German: Rede) with the accompanying text, speaker, agenda item and shouts.
 * @author: Gabriele
 */
package relations;

import interfaces.Speech_Interface;
import parliament.AgendaItem;
import people.Deputy;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Speech implements Speech_Interface {
    ArrayList<SpeechSegment> segments;
    String speakerId;
    AgendaItem agendaItem;

    public Speech(ArrayList<SpeechSegment> segments, String speakerId, AgendaItem agendaItem){
        this.segments = segments;
        this.speakerId = speakerId;
        this.agendaItem = agendaItem;
    }

    /**
     * Get the individual speech segments. This is used to map certain comments to parts
     * of the speech.
     */
    public ArrayList<SpeechSegment> getSegments(){
        return this.segments;
    }

    /**
     * Get the full text of a speech, which is assembled from
     * all segments.
     */
    public String getText(){
        StringBuilder fullText = new StringBuilder();
        for (SpeechSegment segment : this.segments){
            fullText.append(segment.getText());
        }
        return fullText.toString();
    }

    /**
     * Get the agenda item a speech belongs to.
     */
    public AgendaItem getAgendaItem(){
        return this.agendaItem;
    }

    /**
     * Get the speaker id.
     */
    public String getSpeakerId(){
        return this.speakerId;
    }
}


