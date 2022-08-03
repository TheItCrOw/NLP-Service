package interfaces;

import parliament.AgendaItem;
import people.Deputy;

public interface Comment_Interface {

    public String getText();

    public Deputy getDeputy();

    public AgendaItem getAgendaItem();
}
