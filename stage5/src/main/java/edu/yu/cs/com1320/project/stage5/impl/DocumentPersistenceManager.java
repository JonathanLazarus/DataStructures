package edu.yu.cs.com1320.project.stage5.impl;

import com.google.gson.*;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import static java.io.File.listRoots;

/**
 * created by the document store and given to the BTree via a call to BTree.setPersistenceManager
 */
public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {
    private File dir;

    public DocumentPersistenceManager() {
        //set default dir to user home directory
        this.dir = new File(System.getProperty("user.dir"));
    }

    //TODO
    public DocumentPersistenceManager(File baseDir){
        if (baseDir == null) throw new IllegalArgumentException();
        //set default dir to user home directory
       // this.dir = new File(System.getProperty("user.dir") + baseDir.toString());
        //TODO for now we will make dir under home dir, but this is the code (commented) to make baseDir stem from root.

        try {
            String path = listRoots()[0].toString() + baseDir;
            this.dir = new File(path);
            Files.createTempDirectory(dir.getPath(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    public void serialize(URI uri, Document val) throws IOException {
        GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(DocumentImpl.class, this.serializer());

        Gson customGson = gsonBuilder.create();
        String docAsJSON = customGson.toJson(val);

        try {
            //set @path to the full path with file name (///x.json)
            File path = new File(this.dir.toString() + uri.getSchemeSpecificPart() + ".json");
            //make the directories in which the JSON file will be written to
            path.getParentFile().mkdirs();
            //create the JSON file withing this new path
            path.createNewFile();
            // Constructs a FileWriter given a file name, using the platform's default charset
            FileWriter fileWriter = new FileWriter(path);
            //write the JSON information to the file.
            fileWriter.write(docAsJSON);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Document deserialize(URI uri) throws IOException {
        //set @path to the base dir + the uri + .json
        File path = new File(this.dir.toString() + uri.getSchemeSpecificPart() + ".json");
        //to be instantiated inside the try block
        JsonObject json = new JsonObject();
        //if the file does not exist, exception thrown, then null is returned.
        try (FileReader reader = new FileReader(path)) {
            //JSON parser object to parse read file
            JsonParser jsonParser = new JsonParser();
            //Read JSON file
            json = jsonParser.parse(reader).getAsJsonObject();
        } catch (FileNotFoundException e) {
            return null;
        } catch (JsonIOException | JsonSyntaxException e) {
            e.printStackTrace();
        }
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(DocumentImpl.class, this.deserializer());
        Gson customGson = gsonBuilder.create();
        DocumentImpl doc = customGson.fromJson(json.toString(), DocumentImpl.class);
        //clean up directories - delete the file which was deserialized, and all of its empty parent directories.
        this.cleanUp(path);
        return doc;
    }

    private void cleanUp(File path) {
        //delete the file that was passed in (the one which was just deserialized)
        path.delete();
        //change the dir we're working with to it's parent dir
        path = path.getParentFile();
        //while loop only executes if the current dir (path) is empty
        while (path.listFiles().length == 0) {
            //since the dir is empty, delete the current folder.
            path.delete();
            //now change the directory path to be the parent dir
            path = path.getParentFile();
        }
        //repeats until the dir is not empty
    }

    private JsonSerializer<DocumentImpl> serializer() {
        return (src, typeOfSrc, context) -> {
            JsonObject jsonDoc = new JsonObject();
            //get the word count map
            HashMap<String,Integer> wordMap = (HashMap<String, Integer>) src.getWordMap();
            //jsonArray to hold the hashmap keys
            JsonArray word = new JsonArray();
            //holds the values for each key
            JsonArray count = new JsonArray();
            for(String w : wordMap.keySet()) {
                word.add(w);
                count.add(wordMap.get(w));
            }

            jsonDoc.addProperty("text", src.getDocumentAsTxt());
            jsonDoc.addProperty("uri", src.getKey().toString());
            jsonDoc.addProperty("hashCode", src.hashCode());
            jsonDoc.add("words", word);
            jsonDoc.add("count", count);
            return jsonDoc;
        };
    }

    private JsonDeserializer<DocumentImpl> deserializer() {
        return (jsonFile, typeOfT, context) -> {
            JsonObject json = jsonFile.getAsJsonObject();
            //create and deserialize hashmap to be passed into new document
            HashMap<String, Integer> wordCount = new HashMap<>();
            //add the word->count mappings.
            JsonArray words = (JsonArray) json.get("words");
            JsonArray count = (JsonArray) json.get("count");
            for(int i = 0; i < words.size(); i++) {
                wordCount.put(words.get(i).getAsString(), count.get(i).getAsInt());
            }
            try {
                return new DocumentImpl(
                    new URI(json.get("uri").getAsString()),
                    json.get("text").getAsString(),
                    json.get("hashCode").getAsInt(),
                    wordCount
                );
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        return null;
        };
    }

}
