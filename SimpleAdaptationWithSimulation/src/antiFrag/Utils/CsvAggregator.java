package antiFrag.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CsvAggregator {

    public static void main(String[] args) {
        // Cartella con i file CSV
        String folderPath = "SimulationAFDataC1";  // Modifica il percorso in base alla tua struttura

        // Trova tutti i file CSV che iniziano con "motes_data" nella cartella
        try {
            Map<Integer, List<File>> neighbourFiles = Files.list(Paths.get(folderPath))
                    .map(Path::toFile)
                    .filter(file -> file.getName().startsWith("motes_data") && file.getName().endsWith(".csv"))
                    .collect(Collectors.groupingBy(CsvAggregator::extractNeighbourId));

            // Ora per ogni neighbour, unisci i file e calcola la media
            for (Map.Entry<Integer, List<File>> entry : neighbourFiles.entrySet()) {
                int neighbourId = entry.getKey();
                List<File> files = entry.getValue();

                // Unisci i file e calcola la media
                List<String[]> aggregatedData = aggregateCsvFiles(files);

                // Scrivi il nuovo file CSV
                writeAggregatedCsv(aggregatedData, folderPath, neighbourId);

                // Cancella i file originali usati per l'aggregazione
                deleteOriginalFiles(files);
            }

            System.out.println("Media dei CSV calcolata con successo e file originali cancellati!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Estrai l'ID del neighbour dal nome del file
    private static int extractNeighbourId(File file) {
        String fileName = file.getName();
        String[] parts = fileName.split("_neighbour");
        return Integer.parseInt(parts[1].replace(".csv", "").trim());
    }

    // Unisce i CSV e calcola la media riga per riga (lasciando invariato il valore di "parents")
    private static List<String[]> aggregateCsvFiles(List<File> files) throws IOException {
        List<String[]> aggregatedData = new ArrayList<>();
        int rowCount = 0;
        int countFiles = files.size();  // Numero di file CSV per il neighbour

        // Lista che memorizza le righe di ciascun file
        List<List<String[]>> allFileRows = new ArrayList<>();

        // Leggi tutte le righe dei file CSV
        for (File file : files) {
            List<String[]> rows = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                boolean firstLine = true;

                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;  // Salta l'header
                        continue;
                    }
                    String[] values = line.split(",");
                    rows.add(values);
                }
            }
            allFileRows.add(rows);
        }

        // Ora sappiamo che ogni file ha lo stesso numero di righe
        rowCount = allFileRows.get(0).size();

        // Iteriamo riga per riga per calcolare la media
        for (int i = 0; i < rowCount; i++) {
            String[] averagedRow = new String[5];  // id, parents, battery, load, dataProbability
            double batterySum = 0.0;
            double loadSum = 0.0;
            double dataProbabilitySum = 0.0;

            String id = null;
            String parents = null;

            // Per ogni file, prendiamo la riga corrispondente
            for (List<String[]> rows : allFileRows) {
                String[] row = rows.get(i);
                if (id == null) {
                    id = row[0];  // Mantieni l'id della prima riga
                }
                if (parents == null) {
                    parents = row[1];  // Mantieni il valore di "parents" della prima riga
                }
                batterySum += Double.parseDouble(row[2]);  // Somma "battery"
                loadSum += Double.parseDouble(row[3]);  // Somma "load"
                dataProbabilitySum += Double.parseDouble(row[4]);  // Somma "dataProbability"
            }

            // Calcola la media per battery, load, dataProbability
            averagedRow[0] = id;
            averagedRow[1] = parents;
            averagedRow[2] = String.valueOf(batterySum / countFiles);
            averagedRow[3] = String.valueOf(loadSum / countFiles);
            averagedRow[4] = String.valueOf(dataProbabilitySum / countFiles);

            // Aggiungi la riga mediata all'elenco delle righe aggregate
            aggregatedData.add(averagedRow);
        }

        return aggregatedData;
    }

    // Scrivi il CSV aggregato
    private static void writeAggregatedCsv(List<String[]> aggregatedData, String folderPath, int neighbourId) {
        String outputFileName = folderPath + "/neighbour_" + neighbourId + "_average.csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFileName))) {
            // Scrivi l'header
            writer.println("id,parents,battery,load,dataProbability");

            for (String[] data : aggregatedData) {
                writer.println(String.join(",", data));
            }

            System.out.println("File CSV scritto: " + outputFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Cancella i file originali
    private static void deleteOriginalFiles(List<File> files) {
        for (File file : files) {
            if (file.delete()) {
                System.out.println("File cancellato: " + file.getName());
            } else {
                System.err.println("Impossibile cancellare il file: " + file.getName());
            }
        }
    }
}

