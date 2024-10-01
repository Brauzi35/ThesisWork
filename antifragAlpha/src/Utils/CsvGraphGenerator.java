package Utils;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class CsvGraphGenerator {

    public static void main(String[] args) {
        // Cartella contenente i file CSV aggregati
        String folderPath = "SimulationAFDataC1";  // Modifica in base alla tua struttura

        // Trova tutti i file che iniziano con "neighbour" e finiscono con "_average.csv"
        try {
            List<File> aggregatedFiles = Files.list(Paths.get(folderPath))
                    .map(Path::toFile)
                    .filter(file -> file.getName().startsWith("run_") && file.getName().endsWith("_average.csv"))
                    .collect(Collectors.toList());

            // Genera grafici per ogni CSV
            for (File file : aggregatedFiles) {
                createGraphFromCsv(file, folderPath);
            }

            System.out.println("Grafici creati con successo!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Crea il grafico a partire dal CSV aggregato
    private static void createGraphFromCsv(File csvFile, String folderPath) throws IOException {
        // Crea un dataset per il grafico
        DefaultCategoryDataset packetLossDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset energyConsumptionDataset = new DefaultCategoryDataset();

        // Leggi il file CSV
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;  // Salta l'header
                    continue;
                }
                // Split dei valori nel CSV
                String[] values = line.split(",");
                String run = values[0];  // Colonna Run
                double packetLoss = Double.parseDouble(values[1]);  // Colonna PacketLoss
                double energyConsumption = Double.parseDouble(values[2]);  // Colonna EnergyConsumption

                // Aggiungi i valori al dataset
                packetLossDataset.addValue(packetLoss, "Packet Loss", run);
                energyConsumptionDataset.addValue(energyConsumption, "Energy Consumption", run);
            }
        }

        // Crea il grafico per Packet Loss
        JFreeChart packetLossChart = ChartFactory.createLineChart(
                "Packet Loss per Run - " + csvFile.getName(),
                "Run",
                "Packet Loss",
                packetLossDataset
        );

        // Crea il grafico per Energy Consumption
        JFreeChart energyConsumptionChart = ChartFactory.createLineChart(
                "Energy Consumption per Run - " + csvFile.getName(),
                "Run",
                "Energy Consumption",
                energyConsumptionDataset
        );

        // Salva il grafico come immagine PNG
        saveChartAsPNG(packetLossChart, folderPath + "/" + csvFile.getName().replace(".csv", "_PacketLoss.png"));
        saveChartAsPNG(energyConsumptionChart, folderPath + "/" + csvFile.getName().replace(".csv", "_EnergyConsumption.png"));
    }

    // Salva il grafico come file PNG
    private static void saveChartAsPNG(JFreeChart chart, String filePath) {
        try {
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 800, 600);
            System.out.println("Grafico salvato: " + filePath);
        } catch (IOException e) {
            System.err.println("Errore durante il salvataggio del grafico: " + e.getMessage());
        }
    }
}
