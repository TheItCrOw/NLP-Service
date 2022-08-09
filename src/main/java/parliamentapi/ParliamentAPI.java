/**
 * Retrieve the protocols of plenary session
 * @author: Gabriele
 */
package parliamentapi;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;
import parliamentapi.utils.Unzip;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class ParliamentAPI {
    ArrayList<String> urlsTwenty;
    ArrayList<String> urlsNineteen;

    public ParliamentAPI() throws IOException, ParserConfigurationException, SAXException {
        this.urlsNineteen = this.getHrefs("https://www.bundestag.de/ajax/filterlist/de/services/opendata/543410-543410?limit=10&noFilterSet=true");
        this.urlsTwenty = this.getHrefs("https://www.bundestag.de/ajax/filterlist/de/services/opendata/866354-866354?limit=10&noFilterSet=true");
    }

    /**
     * Gets all hrefs we need for extracing the XML files
     * by parsing the AJAX Url and scraping using Jsoup.
     *
     * @param baseAjaxUrl the AJAX Url that's called by the parliament website
     */
    public ArrayList<String> getHrefs(String baseAjaxUrl) throws IOException {
        var relevantHrefs = new ArrayList<String>();
        Elements elements = null;
        int offset = 0;
        do {
            Document listDoc = Jsoup.connect(baseAjaxUrl + "&offset=" + offset).get();
            elements = listDoc.getElementsByClass("bt-link-dokument");
            for (Element e : elements){
                String base = "https://www.bundestag.de";
                relevantHrefs.add(base + e.attr("href"));
            }
            offset += 10;
        }
        while (elements.size() > 0);

        return relevantHrefs;
    }

    /**
     * Parse an InputStream to a Document
     * @param stream The InputStream
     */
    org.w3c.dom.Document parse(InputStream stream) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        return docBuilder.parse(stream);
    }

    /**
     * Get all the 19th and 20th legislature period documents.
     */
    public ArrayList<org.w3c.dom.Document> fetchNewProtocols(int newestStoredProtocol) throws ParserConfigurationException, IOException, SAXException, InterruptedException {
        ArrayList<org.w3c.dom.Document> documents = new ArrayList<>();
        System.out.println("Getting 20th Legislature Period Documents:");
        int counter = 1;
        for (String url : this.urlsTwenty){
            if(counter <= this.urlsTwenty.size() - newestStoredProtocol){
                System.out.println(counter + " / " + this.urlsTwenty.size());
                Thread.sleep(500);
                InputStream stream = new URL(url).openStream();
                documents.add(parse(stream));
                stream.close();
            }
            counter++;
        }
        // For now, we dont need 19th documents.
        /*
        System.out.println("Getting 19th Legislature Period Documents:");
        counter = 0;
        for (String url : this.urlsNineteen){
            System.out.println(counter + " / " + this.urlsNineteen.size());
            Thread.sleep(500);
            InputStream stream = new URL(url).openStream();
            documents.add(parse(stream));
            stream.close();
            counter++;
        }
        */
        return documents;
    }

    /**
     * Construct the image url from the parliaments image database to make a request
     * later on.
     * @param name The Name of the deputy
     */
    public String getImageUrl(String name) throws IOException, ParserConfigurationException, SAXException {
        name = name.replace(" ", "+");
        String url = "https://bilddatenbank.bundestag.de/search/picture-result?query=" + name + "&filterQuery%5Bereignis%5D%5B%5D=Portr%C3%A4t%2FPortrait&sortVal=3";
        Document imagesOverview = Jsoup.connect(url).get();
        Element container = imagesOverview.getElementsByClass("rowGridContainer").get(0);
        Elements elements = container.getElementsByClass("item");
        if (elements.size() == 0){
            return null;
        }
        Element imgTag = elements.get(0).getElementsByTag("img").get(0);
        return "https://bilddatenbank.bundestag.de" + imgTag.attr("src");
    }

    /**
     * Get the metadata XML file for the deputies.
     */
    public org.w3c.dom.Document getMetaData() throws IOException, ParserConfigurationException, SAXException {
        String zipUrl = "https://www.bundestag.de/resource/blob/472878/d5743e6ffabe14af60d0c9ddd9a3a516/MdB-Stammdaten-data.zip";
        String fileName = "MDB_STAMMDATEN.XML";

        downloadZip(zipUrl);
        Unzip util = new Unzip();
        util.unzip("metadata.zip", "unzipped-metadata");
        File file = new File("unzipped-metadata/MDB_STAMMDATEN.XML");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = documentBuilder.parse(file);
        util.deleteDirectory(new File("unzipped-metadata"));
        new File("metadata.zip").delete();
        return doc;
    }

    /**
     * Download a zip file from a specified url
     * @param url the ZIP url
     */
    void downloadZip(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        InputStream in = connection.getInputStream();
        FileOutputStream out = new FileOutputStream("metadata.zip");
        copy(in, out);
        out.close();
        connection.disconnect();
    }

    /**
     * Utility method used in downloadZip
     * @param input the Input
     * @param output the Output
     */
    void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buf = new byte[1024];
        int n = input.read(buf);
        while (n >= 0) {
            output.write(buf, 0, n);
            n = input.read(buf);
        }
        output.flush();
    }
}
