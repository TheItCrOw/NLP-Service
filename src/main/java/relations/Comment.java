/**
 * Holds a single comment (German: Kommentar).
 *
 * Currently only useful for holding shouts, as applauses are not used in any program functionality.
 * @author: Gabriele
 */
package relations;

import interfaces.Comment_Interface;
import parliament.AgendaItem;
import people.Deputy;

public class Comment implements Comment_Interface {
    String text;
    Deputy by;
    AgendaItem agendaItem;

    public Comment(String text){
        this.text = text;
    }

    public Comment(String text, Deputy by){
        this.text = text;
        this.by = by;
    }

    public Comment(String text, Deputy by, AgendaItem agendaItem){
        this.text = text;
        this.by = by;
        this.agendaItem = agendaItem;
    }

    /**
     * Get the full text of comment
     */
    public String getText(){
        return this.text;
    }

    /**
     * Get the deputy a comment belongs to
     */
    public Deputy getDeputy() { return this.by; }

    /**
     * Get the agenda item a comment belongs to
     */
    public AgendaItem getAgendaItem() { return this.agendaItem; }
}
