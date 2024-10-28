package antiFrag.Utils;

import java.io.FileWriter;
import java.io.IOException;

public class SaveJsonToFile {

    public void save(String json, String fileName){
        try (FileWriter file = new FileWriter(fileName)) {
            // Scrivi il JSON nel file
            file.write(json);
            System.out.println("JSON salvato con successo su " + fileName);
        } catch (IOException e) {
            // Gestione degli errori di scrittura
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        // Supponiamo che il tuo JSON sia una stringa, ad esempio:
        String json = "{\"actionSelection\":\"epsilon=0.1;prototype=com.github.chen0040.rl.actionselection.EpsilonGreedyActionSelectionStrategy\",\"model\":{\"actionCount\":25,\"alphaMatrix\":{\"columnCount\":25,\"defaultValue\":0.1,\"rowCount\":2500,...}}";

        // Specifica il nome del file di output
        String fileName = "output.json";

        try (FileWriter file = new FileWriter(fileName)) {
            // Scrivi il JSON nel file
            file.write(json);
            System.out.println("JSON salvato con successo su " + fileName);
        } catch (IOException e) {
            // Gestione degli errori di scrittura
            e.printStackTrace();
        }
    }
}
