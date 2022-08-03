/**
 * Holds a single Deputy (German: Redner/Abgeordneter; treated as synonyms).
 * @author: Gabriele
 */
package people;

import interfaces.Deputy_Interface;
import parliament.Fraction;

public class Deputy implements Deputy_Interface {
    String firstName;
    String lastName;
    Fraction fraction;
    String id;

    public Deputy(String firstName, String lastName, Fraction fraction, String id){
        this.firstName = firstName;
        this.lastName = lastName;
        this.fraction = fraction;
        this.id = id;
    }

    /**
     * Get the fraction of the deputy
     */
    public Fraction getFraction(){
        return this.fraction;
    }

    /**
     * Get the first name of the deputy
     */
    public String getFirstName() { return this.firstName; }

    /**
     * Get the last name of the deputy
     */
    public String getLastName() { return this.lastName; }

    /**
     * Get the full name of the deputy (first name + last name)
     */
    public String getFullName() { return this.firstName + " " + this.lastName; }

    /**
     * Get the ID of the deputy. This is used for making API requests to a specific deputy.
     */
    public String getId() { return this.id; }

    /**
     * Make a string representation of the deputy
     */
    public String toString(){
        String str = this.firstName + " " + this.lastName;
        if (this.getFraction() != null){
            str += " (" + fraction.getName() + ")";
        }
        return str;
    }

    /**
     * Check if two deputies are the same deputy.
     * Required for Sets to work properly.
     */
    @Override
    public boolean equals(Object obj) {
        /**
         * Implement equality checking for getting rid of duplicates.
         */
        if (!(obj instanceof Deputy)) return false;

        return this.toString().equals(obj.toString());
    }

    /**
     * Required for Sets to work properly (implemented for getting rid of duplicates).
     */
    @Override
    public int hashCode() {

        int base = (this.firstName.hashCode() + this.lastName.hashCode());
        if (this.fraction.getName() != null){
            base += this.fraction.getName().hashCode();
        }
        return base;
    }
}
