/**
 * Manage the plenary session protocol (German: Plenarsitzungsprotokoll)
 * @author: Gabriele
 */
package parliament;

import interfaces.PlenarySessionProtocol_Interface;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;

public class PlenarySessionProtocol implements PlenarySessionProtocol_Interface {
    Date date;
    int legislaturePeriod;
    int number;
    String title;
    int agendaItemsCount;
    ArrayList<org.codehaus.jettison.json.JSONObject> nlpSpeeches;

    public PlenarySessionProtocol(Date date,
                                  Integer legislaturePeriod,
                                  Integer number,
                                  String title,
                                  int agendaItemsCount){
        this.date = date;
        this.legislaturePeriod = legislaturePeriod;
        this.number = number;
        this.title = this.clean(title);
        this.agendaItemsCount = agendaItemsCount;
    }

    /**
     * Clean the title of the protocol from inconsistencies.
     */
    String clean(String title){
        return title
                .replace("\t", "")
                .replace("\n", "")
                .replace(" (neu)", "");
    }

    public void setNLPSpeeches(ArrayList<org.codehaus.jettison.json.JSONObject> nlpSpeeches) { this.nlpSpeeches = nlpSpeeches;}
    public ArrayList<org.codehaus.jettison.json.JSONObject> getNLPSpeeches() { return nlpSpeeches;}

    /**
     * Get the protocol date.
     */
    public Date getDate(){
        return this.date;
    }

    /**
     * Get the protocol legislature period.
     */
    public int getLegislaturePeriod() { return this.legislaturePeriod; }

    /**
     * Get the protocol number
     */
    public int getNumber(){
        return this.number;
    }

    /**
     * Get the protocol of the title.
     */
    public String getTitle(){
        return this.title;
    }

    /**
     * Get the protocol of the title.
     */
    public int getAgendaItemsCount(){
        return this.agendaItemsCount;
    }
}