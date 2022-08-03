/**
 * Holds a single Deputy (German: Redner/Abgeordneter; treated as synonyms).
 * @author: Gabriele
 */
package database;

import interfaces.Deputy_Interface;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import people.Deputy;

public class Deputy_Mongo extends Deputy implements Deputy_Interface {
    String academicTitle;
    String historySince;
    String birthDate;
    String deathDate;
    String gender;
    String maritalStatus;
    String religion;
    String profession;
    String party;

    public Deputy_Mongo(
            Deputy deputy,
            String academicTitle,
            String historySince,
            String birthDate,
            String deathDate,
            String gender,
            String maritalStatus,
            String religion,
            String profession,
            String party
        ){
        super(deputy.getFirstName(), deputy.getLastName(), deputy.getFraction(), deputy.getId());
        this.academicTitle = academicTitle;
        this.historySince = historySince;
        this.birthDate = birthDate;
        this.deathDate = deathDate;
        this.gender = gender;
        this.maritalStatus = maritalStatus;
        this.religion = religion;
        this.profession = profession;
        this.party = party;
    }

    public Deputy_Mongo(Deputy deputy){
        super(deputy.getFirstName(), deputy.getLastName(), deputy.getFraction(), deputy.getId());
    }

    /**
     * Serialize a deputy into a JSONObject.
     */
    public JSONObject toJSONObject() throws JSONException {
        String fraction = null;
        if (this.getFraction() != null){
            fraction = this.getFraction().getName();
        }
        return new JSONObject()
                .put("firstName", this.getFirstName())
                .put("lastName", this.getLastName())
                .put("fraction", fraction)
                .put("party", this.party)
                .put("academicTitle", this.academicTitle)
                .put("historySince", this.historySince)
                .put("birthDate", this.birthDate)
                .put("deathDate", this.deathDate)
                .put("gender", this.gender)
                .put("maritalStatus", this.maritalStatus)
                .put("religion", this.religion)
                .put("profession", this.profession)
                .put("id", this.getId());
    }
}
