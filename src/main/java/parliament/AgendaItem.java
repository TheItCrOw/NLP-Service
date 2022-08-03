/**
 * Manage agenda items (German: Tagesordnungspunkte)
 * @author: Gabriele
 */
package parliament;

import interfaces.AgendaItem_Interface;

public class AgendaItem implements AgendaItem_Interface {
    PlenarySessionProtocol protocol;
    int legislaturePeriod;
    int number;

    public AgendaItem(PlenarySessionProtocol protocol, int number){
        this.legislaturePeriod = protocol.getLegislaturePeriod();
        this.protocol = protocol;
        this.number = number;
    }

    /**
     * Get the protocol of the agenda item.
     */
    public PlenarySessionProtocol getProtocol(){
        return this.protocol;
    }

    /**
     * Get the agenda item number.
     */
    public int getNumber(){
        return this.number;
    }

    /**
     * Get the legislature period of the agenda item.
     */
    public int getLegislaturePeriod() {
        return this.legislaturePeriod;
    }
}
