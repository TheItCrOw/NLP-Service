package export;

import com.mongodb.client.MongoDatabase;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class DatabaseExporter {

    public static void writeStringToFile(String fileName, String content)
            throws IOException {
        FileOutputStream outputStream = new FileOutputStream(fileName);
        byte[] strToBytes = content.getBytes();
        outputStream.write(strToBytes);

        outputStream.close();
    }

    /**
     * Exporst the files in the collection to a given path
     * @param toPath
     * @param db
     */
    public static void exportCollection(String collection, String toPath, MongoDatabase db){
        var i = 0;
        // AnnotationEvaluation
        try{
            System.out.println("Exporting " + collection);
            var total = db.getCollection(collection).countDocuments();
            System.out.println("Found " + total + " documents.");
            var annotations = db.getCollection(collection).find().iterator();
            while(annotations.hasNext()){
                var cur = annotations.next();
                writeStringToFile(toPath + "\\" + collection + "\\" + cur.get("_id").toString() + ".json", cur.toJson());
                System.out.println(i + "/" + total + " " + collection);
                i++;
            }
        } catch(Exception ex){
            System.out.println("Error at collection at document " + i);
            ex.printStackTrace();
            System.out.println(ex.getMessage());
        }
    }
}
