package interfaces;

import parliament.Fraction;
import people.Deputy;

public interface Deputy_Interface {

    public Fraction getFraction();

    public String getFirstName();

    public String getLastName();

    public String getFullName();

    public String getId();

    public String toString();

    @Override
    public boolean equals(Object obj);

    @Override
    public int hashCode();
}
