/**
 * Manage fractions (German: Fraktionen)
 * @author: Gabriele
 */
package parliament;

import interfaces.Fraction_Interface;

public class Fraction implements Fraction_Interface {
    String name;

    public Fraction(String name){
        this.name = this.clean(name);
    }

    /**
     * Clean the fraction name from inconsistencies.
     */
    String clean(String name){
        /**
         * When meaning the same fraction, the XML documents use different strings.
         * Clean them all up to a common format for comparison and listing purposes.
         */
        if (name == null || name.equals("null")) return null;

        name = name.replace("\u00a0","");
        name = name.replace(" ", "");
        return switch (name) {
            case "BÜNDNIS90/DIEGRÜNEN", "BÜNDNIS90/" -> "Bündnis 90 / Die Grünen";
            case "DIELINKE" -> "Die Linke";
            case "Fraktionslos", "fraktionslos", "Erklärungnach§30GO", "zurGeschäftsordnung" -> null;
            default -> name;
        };
    }

    /**
     * Get the name of the fraction.
     */
    public String getName(){
        return this.name;
    }
}