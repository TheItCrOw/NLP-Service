package interfaces;

import parliament.AgendaItem;
import relations.SpeechSegment;

import java.util.ArrayList;

public interface Speech_Interface {

    public ArrayList<SpeechSegment> getSegments();

    public String getText();

    public AgendaItem getAgendaItem();

    public String getSpeakerId();
}
