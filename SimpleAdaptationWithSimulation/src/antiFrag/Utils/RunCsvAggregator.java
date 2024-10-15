package antiFrag.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RunCsvAggregator {

    public static void main(String[] args) {
        // Cartella con i file CSV
        String folderPath = "SimulationAFDataC1";  // Modifica il percorso in base alla tua struttura

        // Trova tutti i file che iniziano con "run" e contengono "neighbour"
        try {
            Map<Integer, List<File>> runFiles = Files.list(Paths.get(folderPath))
                    .map(Path::toFile)
                    .filter(file -> file.getName().startsWith("run") && file.getName().contains("neighbour") && file.getName().endsWith(".csv"))
                    .collect(Collectors.groupingBy(RunCsvAggregator::extractNeighbourIdFromRun));

            // Ora per ogni neighbour, unisci i file e calcola la media
            for (Map.Entry<Integer, List<File>> entry : runFiles.entrySet()) {
                int neighbourId = entry.getKey();
                List<File> files = entry.getValue();

                // Unisci i file e calcola la media
                List<String[]> aggregatedData = aggregateRunDataFiles(files);

                // Scrivi il nuovo file CSV
                writeAggregatedCsv(aggregatedData, folderPath, neighbourId, "Run,PacketLoss,EnergyConsumption");

                // Cancella i file originali usati per l'aggregazione
                deleteOriginalFiles(files);
            }

            System.out.println("Media dei CSV per 'run' calcolata con successo!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Estrai l'ID del neighbour dal nome del file (per i file "run")
    private static int extractNeighbourIdFromRun(File file) {
        String fileName = file.getName();
        String[] parts = fileName.split("_neighbour");
        return Integer.parseInt(parts[1].replace(".csv", "").trim());
    }

    // Unisci i file "run" e calcola la media riga per riga
    private static List<String[]> aggregateRunDataFiles(List<File> files) throws IOException {
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
            String[] averagedRow = new String[3];  // Run, PacketLoss, EnergyConsumption
            double packetLossSum = 0.0;
            double energyConsumptionSum = 0.0;

            String run = null;

            // Per ogni file, prendiamo la riga corrispondente
            for (List<String[]> rows : allFileRows) {
                String[] row = rows.get(i);
                if (run == null) {
                    run = row[0];  // Mantieni il "Run" della prima riga
                }
                packetLossSum += Double.parseDouble(row[1]);  // Somma "PacketLoss"
                energyConsumptionSum += Double.parseDouble(row[2]);  // Somma "EnergyConsumption"
            }

            // Calcola la media per PacketLoss e EnergyConsumption
            averagedRow[0] = run;
            averagedRow[1] = String.valueOf(packetLossSum / countFiles);
            averagedRow[2] = String.valueOf(energyConsumptionSum / countFiles);

            // Aggiungi la riga mediata all'elenco delle righe aggregate
            aggregatedData.add(averagedRow);
        }

        return aggregatedData;
    }

    // Scrivi il CSV aggregato
    private static void writeAggregatedCsv(List<String[]> aggregatedData, String folderPath, int neighbourId, String header) {
        String outputFileName = folderPath + "/run_neighbour_" + neighbourId + "_average.csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFileName))) {
            // Scrivi l'header
            writer.println(header);

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

