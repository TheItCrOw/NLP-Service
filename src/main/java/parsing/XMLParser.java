/**
 * Responsible for parsing the XML documents into our custom datastructures.
 *
 * The datastructures are found in the "parliament", "people" and "relations" packages.
 *
 * This class should *not* contain business logic responsible for printing/writing to files.
 *
 * @author: Gabriele
 */
package parsing;

import org.checkerframework.checker.units.qual.A;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import parliament.AgendaItem;
import parliament.Fraction;
import parliament.PlenarySessionProtocol;
import people.Deputy;
import relations.Comment;
import relations.Speech;
import relations.SpeechSegment;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class XMLParser {
    ArrayList<Document> documents;
    Set<Deputy> speakers; // internal copy for efficiency purposes

    public XMLParser(ArrayList<Document> documents) throws ParserConfigurationException, SAXException, IOException {
        this.documents = documents;
    }

    /**
     * Every XML document contains a <rednerliste> Tag. Use it to populate a list of Deputy objects
     * and return them.
     */
    public Set<Deputy> getSpeakers(){
        Set<Deputy> deputies = new HashSet<Deputy>();
        for (Document doc : this.documents){
            Element speakerList = (Element) doc.getElementsByTagName("rednerliste").item(0);
            NodeList speakers = speakerList.getElementsByTagName("redner");

            for (int i = 0; i < speakers.getLength(); i++){
                Element speaker = (Element) speakers.item(i);
                Element details = (Element) speaker.getElementsByTagName("name").item(0);
                String firstName = details.getElementsByTagName("vorname").item(0).getTextContent();
                String lastName = details.getElementsByTagName("nachname").item(0).getTextContent();

                String fractionName = null;
                NodeList fractions = details.getElementsByTagName("fraktion");
                if (fractions.getLength() > 0){
                    fractionName = fractions.item(0).getTextContent();
                }
                Fraction fraction = new Fraction(fractionName);
                Deputy deputy = new Deputy(
                        firstName=firstName,
                        lastName=lastName,
                        fraction=fraction,
                        speaker.getAttribute("id")
                );
                deputies.add(deputy);
            }
        }
        if (this.speakers == null){
            this.speakers = deputies;
        }
        return deputies;
    }

    /**
     * Every XML document contains a list of <rede> Tag. Use them to populate a list of Speech objects
     * and return them.
     */
    public ArrayList<Speech> getSpeeches(int documentId, int itemId) throws ParseException {
        Document doc = this.documents.get(documentId);
        PlenarySessionProtocol protocol = this.populatePlenarySessionProtocol(doc);
        AgendaItem item = new AgendaItem(protocol, itemId + 1);
        Element agendaItem = (Element) doc.getElementsByTagName("tagesordnungspunkt").item(itemId);
        if (agendaItem == null){
            return null;
        }

        NodeList docSpeeches = agendaItem.getElementsByTagName("rede");
        ArrayList<Speech> speeches = new ArrayList<Speech>();
        for (int i = 0; i < docSpeeches.getLength(); i++){
            NodeList children = docSpeeches.item(i).getChildNodes();
            ArrayList<Node> comments = new ArrayList<>();
            StringBuilder text = new StringBuilder();
            ArrayList<SpeechSegment> segments = new ArrayList<>();
            Deputy speaker = null;
            for (int x = 0; x < children.getLength(); x++){
                switch (children.item(x).getNodeName()) {
                    case "p" -> {
                        if (!text.isEmpty()) {
                            segments.add(new SpeechSegment(text.toString(), populateShouts(comments, item)));
                            text = new StringBuilder();
                            comments = new ArrayList<Node>();
                        }
                        Element pTag = (Element) children.item(x);
                        if (pTag.getAttribute("klasse").equals("redner")) {
                            Element speakerTag = (Element) pTag.getElementsByTagName("redner").item(0);
                            speaker = this.populateSpeakerFromId(speakerTag.getAttribute("id"));
                            continue;
                        }
                        text.append(pTag.getTextContent());
                        text.append("\n");
                    }
                    case "kommentar" -> comments.add(children.item(x));
                }
            }
            String id = null;
            if (speaker != null) id = speaker.getId();

            speeches.add(new Speech(segments, id, item));
        }
        return speeches;
    }

    /**
     * Populate a PlenarySessionProtocol object from a single XML file.
     * There's always going to be a 1:1 mapping between a single object and an XML file.
     */
    public PlenarySessionProtocol populatePlenarySessionProtocol(Document doc) throws ParseException {
        Element protocolRoot = (Element) doc.getElementsByTagName("dbtplenarprotokoll").item(0);
        Element metadata = (Element) ((Element) protocolRoot.getElementsByTagName("vorspann").item(0))
                .getElementsByTagName("kopfdaten").item(0);
        Element protocolNumber = (Element) metadata.getElementsByTagName("plenarprotokoll-nummer").item(0);
        int legislaturePeriod = Integer.parseInt(protocolNumber.getElementsByTagName("wahlperiode").item(0).getTextContent());
        int number = Integer.parseInt(protocolNumber.getElementsByTagName("sitzungsnr").item(0).getTextContent());
        String title = metadata.getElementsByTagName("sitzungstitel").item(0).getTextContent();

        Element about = (Element) metadata.getElementsByTagName("veranstaltungsdaten").item(0);
        String date = ((Element) about.getElementsByTagName("datum").item(0)).getAttribute("date");
        SimpleDateFormat sdtF = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
        Date dateFormatiert = sdtF.parse(date);
        return new PlenarySessionProtocol(
                dateFormatiert,
                legislaturePeriod,
                number,
                title,
                ((NodeList) doc.getElementsByTagName("tagesordnungspunkt")).getLength()
        );
    }

    /**
     * Utility function for looping through all objects and getting all speeches combined.
     * This is useful to avoid excessive business logic in the View class.
     */
    public ArrayList<Speech> getAllSpeeches() throws ParseException, ParserConfigurationException, IOException, SAXException {
        ArrayList<Speech> allSpeeches = new ArrayList<Speech>();
        ArrayList<Speech> currentSpeeches = new ArrayList<Speech>();
        int docNumber = 1;
        while (docNumber <= this.documents.size()){
            int itemNumber = 1;
            while (true){
                currentSpeeches = this.getSpeeches(docNumber - 1, itemNumber - 1);

                if (currentSpeeches == null) break;

                allSpeeches.addAll(currentSpeeches);
                itemNumber++;
            }
            docNumber++;
        }
        return allSpeeches;
    }

    /**
     * Given an ID of a speaker, return a speaker object.
     */
    Deputy populateSpeakerFromId(String id) {
        if (this.speakers == null){
            this.getSpeakers(); // we call this to populate the speakers attribute
        }
        for (Deputy speaker : this.speakers){
            if (speaker.getId().equals(id)) return speaker;
        }
        return null;
    }

    /**
     * Given an ID of a speaker, return a speaker object.
     */
    String populateIdFromSpeaker(String firstName, String lastName) {
        if (this.speakers == null){
            this.getSpeakers(); // we call this to populate the speakers attribute
        }
        for (Deputy speaker : this.speakers){
            if (speaker.getFirstName().equals(firstName) && speaker.getLastName().equals(lastName)) return speaker.getId();
        }
        return null;
    }

    /**
     * Utility method for distinguishing a shout from an applause in a <kommentar> tag.
     */
    boolean isShout(String text){
        return (text.contains(":") || text.contains("Zuruf") || text.contains("Gegenruf"));
    }

    /**
     * Return a list of fractions that speak in a given comment text.
     * The logic looks a lot because we need to distinguish between who talks and who is talked *about*:
     * the latter we don't want to capture.
     */
    ArrayList<Fraction> getFractionsInText(String text){
        ArrayList<Fraction> fractions = new ArrayList<Fraction>();
        // we check for the indexes of the fractions compared to the ':' in order to
        // avoid mentions of fractions in the text
        if (text.contains("SPD") && (text.indexOf("SPD") > text.indexOf(':'))){
            fractions.add(new Fraction("SPD"));
        }
        if (text.contains("CDU/CSU") && (text.indexOf("CDU/CSU") > text.indexOf(':'))){
            fractions.add(new Fraction("CDU/CSU"));
        }
        if (text.contains("GRÜNEN") && (text.indexOf("GRÜNEN") > text.indexOf(':'))){
            fractions.add(new Fraction("BÜNDNIS90/DIEGRÜNEN"));
        }
        if (text.contains("LINKE") && (text.indexOf("LINKE") > text.indexOf(':'))){
            fractions.add(new Fraction("DIELINKE"));
        }
        if (text.contains("AfD") && (text.indexOf("AfD") > text.indexOf(':'))){
            fractions.add(new Fraction("AfD"));
        }
        if (text.contains("FDP") && (text.indexOf("FDP") > text.indexOf(':'))){
            fractions.add(new Fraction("FDP"));
        }
        return fractions;
    }

    /**
     * Given a list of comments and an agenda item, return an ArrayList of comments
     * that contain all the shouts, with the accompanying speaker and his fraction.
     *
     * This will later be used to filter in the View.
     */
    ArrayList<Comment> populateShouts(ArrayList<Node> comments, AgendaItem agendaItem){
        ArrayList<Comment> shouts = new ArrayList<Comment>();
        for (Node node : comments) {
            String text = node.getTextContent();
            if (!isShout(text)) {
                Comment comment = new Comment(
                        text,
                        null,
                        agendaItem
                );
                shouts.add(comment);
                continue;
            }
            Deputy by = null;
            // For comments with multiple shouts, split by the " – " character
            text = text.replace(" ", " "); // fixes inconsistent spacing
            // deal with inconsistencies in the data
            text = text.replace(" –", " – ");
            text = text.replace("– ", " – ");
            text = text.replace("- ", " – ");
            text = text.replace(" -", " – ");
            for (String substr : text.split(" – ")) {
                if (!isShout(substr)) {
                    continue;
                }
                substr = substr.replace("([", "("); // fixes minor error in an XML file
                String[] substrSplit = substr.split("\\[");
                String name = substrSplit[0];
                String nameClean = "";
                try {
                    nameClean = name.substring(0, name.length() - 1); // -1 to leave out the last whitespace
                    if (nameClean.contains("Abg.")) {
                        nameClean = nameClean.split("Abg\\.")[1];
                        nameClean = nameClean.substring(1, nameClean.length()); // remove leading whitespace
                    }
                    nameClean = nameClean.replace("(", "");

                } catch (Exception ignored) {
                }

                // Something like "Zuruf von der AfD: Und verfassungswidrig!" was captured as the name...
                if (nameClean.contains("Zuruf")) {
                    nameClean = "";
                }
                if (nameClean.startsWith(" ")) {
                    nameClean = nameClean.substring(1, nameClean.length());
                }

                // get text
                String[] textSplit = substr.split(":");
                String commentText = null;
                if (textSplit.length > 1) {
                    commentText = textSplit[1].trim();
                    commentText = commentText.replace(")", "");
                }
                if (commentText == null) {
                    continue;
                }

                Fraction fraction = null;

                // Other attributes
                if (substrSplit.length > 1) {
                    ArrayList<Fraction> fractions = new ArrayList<Fraction>();
                    int x = 1;
                    while (x < substrSplit.length) {
                        fractions = getFractionsInText(substrSplit[x].split("\\]")[0]);
                        if (fractions.size() > 0) {
                            break;
                        }
                        x++;
                    }

                    if (fractions.size() == 1) { // it can't be > 1 because of string splitting!
                        fraction = fractions.get(0);
                    }
                }

                String firstName = "";
                String lastName = "";
                String[] res = nameClean.split(" ");
                if (res.length >= 2){
                    firstName = res[res.length - 2];
                    lastName = res[res.length - 1];
                }
                else {
                    firstName = nameClean;
                }
                Deputy deputy = new Deputy(firstName, lastName, fraction, populateIdFromSpeaker(firstName, lastName));
                Comment comment = new Comment(
                        commentText,
                        deputy,
                        agendaItem
                );
                shouts.add(comment);
            }
        }
        return shouts;
    }

    /**
     * Create a map of who lead which session how often.
     *
     * This will be displayed in the View.
     */
    public Map<String, Integer> populateChairpeopleCountMap(){
        Map<String, Integer> sumMap = new HashMap<String, Integer>();
        for (Document doc : this.documents){
            Map<String, Integer> singleProtocolMap = new HashMap<String, Integer>();
            Element start = (Element) doc.getElementsByTagName("sitzungsbeginn").item(0);
            NodeList pTags = start.getElementsByTagName("p");
            NodeList nameTags = start.getElementsByTagName("name");
            boolean foundInStart = false;

            // 1. Step: search all name tags in tag "sitzungsbeginn" that contain "präsident"
            for (int i = 0; i < nameTags.getLength(); i++){
                String content = nameTags.item(i).getTextContent();
                if (content.contains("Präsident") || content.contains("präsident")){
                    // fix inconsistencies
                    content = content.replace(":", "");
                    content = content.replace(" ", "");
                    content = content.replace("\t", "");
                    content = content.replace("\n", "");
                    content = content.trim();
                    singleProtocolMap.computeIfAbsent(content, k -> 1);
                    foundInStart = true;
                    break;
                }
            }

            if (!foundInStart){
                // 2. Step: search all p tags in tag "sitzungsbeginn" that contain "präsident"
                for (int i = 0; i < pTags.getLength(); i++){
                    String content = pTags.item(i).getTextContent();
                    if (content.contains("Präsident") || content.contains("präsident")){
                        // fix inconsistencies
                        content = content.replace(":", "");
                        content = content.replace(" ", "");
                        content = content.replace("\t", "");
                        content = content.replace("\n", "");
                        content = content.trim();
                        singleProtocolMap.computeIfAbsent(content, k -> 1);
                        break;
                    }
                }
            }

            // Execute this regardless of the boolean above, because there could be multiple chairpeople
            // 3. Step: search all <name> tags that contain "präsident"
            NodeList names = doc.getElementsByTagName("name");
            for (int i = 0; i < names.getLength(); i++){
                String text = names.item(i).getTextContent();
                if (text.contains("präsident")){
                    // fix inconsistencies
                    text = text.replace(":", "");
                    text = text.replace(" ", " ");
                    text = text.replace("\t", "");
                    text = text.replace("\n", " ");
                    text = text.trim();
                    singleProtocolMap.computeIfAbsent(text, k -> 1);
                }
            }

            // create the sum map of all protocols
            for (Map.Entry<String, Integer> entry : singleProtocolMap.entrySet()) {
                String key = entry.getKey();
                sumMap.computeIfAbsent(key, k -> 0);
                sumMap.put(key, sumMap.get(key) + 1);
            }
        }
        return sumMap;
    }
}
