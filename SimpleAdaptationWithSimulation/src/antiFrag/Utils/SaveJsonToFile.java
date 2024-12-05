package antiFrag.Utils;

import java.io.FileWriter;
import java.io.IOException;

public class SaveJsonToFile {

    public void save(String json, String fileName){
        try (FileWriter file = new FileWriter(fileName)) {
            // write json
            file.write(json);
            System.out.println("JSON succesfully saved on " + fileName);
        } catch (IOException e) {
            // errors
            e.printStackTrace();
        }
    }

}
