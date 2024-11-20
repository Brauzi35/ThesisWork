package antiFrag.AnomalyDetection;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import org.apache.commons.math3.ml.distance.EuclideanDistance;

public class AnomalyDetection {

    // Classe per rappresentare un singolo dato (informazioni di una run) con coordinate multiple
    public static class DataPoint {
        private int runId;
        private double[] points;

        public DataPoint(int runId, double[] points) {
            this.runId = runId;
            this.points = points;
        }

        public double[] getPoints() {
            return this.points;
        }

        public int getRunId() {
            return this.runId;
        }
    }

    // Mappa che contiene le informazioni "normali" (modello) indicizzate per run
    private static Map<Integer, DataPoint> normalModel = new HashMap<>();
    private static int commonDimension = 9; // Dimensione fissa per ogni punto dati

    public static void main(String[] args) throws IOException{
        // Directory dei file
        String baseDir = "AnomalyDetectionFiles";

        // Carica i file qos_X.csv e stateY.txt
        List<File> qosFiles = Files.list(Paths.get(baseDir))
                .filter(p -> p.toString().contains("qos_"))
                .map(Path::toFile)
                .collect(Collectors.toList());

        List<File> stateFiles = Files.list(Paths.get(baseDir))
                .filter(p -> p.toString().contains("state"))
                .map(Path::toFile)
                .collect(Collectors.toList());

        // Costruzione del modello basato sui file normali
        buildNormalModel(qosFiles, stateFiles);

        // Supponiamo che a runtime riceviamo nuovi dati simili ai file processati
        // Eseguiamo il confronto con i dati normali
        double[] newRuntimeData = {0.14, 24.82968, 11874.9, 7, 7, 0.513, 15, 100, 15}; // Dati simulati ricevuti a runtime
        int receivedRunId = 2;  // Supponiamo che abbiamo ricevuto dati per la Run 2

        // Eseguiamo la rilevazione di anomalie
        checkForAnomaly(receivedRunId, newRuntimeData);
    }

    public void init(){
        // Directory dei file
        try {
            String baseDir = "AnomalyDetectionFiles";

            // Carica i file qos_X.csv e stateY.txt
            List<File> qosFiles = Files.list(Paths.get(baseDir))
                    .filter(p -> p.toString().contains("qos_"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            List<File> stateFiles = Files.list(Paths.get(baseDir))
                    .filter(p -> p.toString().contains("state"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            // Costruzione del modello basato sui file normali
            buildNormalModel(qosFiles, stateFiles);
        }catch (Exception e){
            System.err.println("exception");
        }

    }

    // Metodo per costruire il modello normale dai file iniziali
    public static void buildNormalModel(List<File> qosFiles, List<File> stateFiles) throws IOException {
        // Processa i file qos_X.csv
        for (File file : qosFiles) {
            try (CSVReader reader = new CSVReader(new FileReader(file))) {
                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line[0].equals("Run")) continue; // Salta l'intestazione

                    // Dati per qos_X.csv
                    int runId = Integer.parseInt(line[0]);
                    double packetLoss = Double.parseDouble(line[1]);
                    double energyConsumption = Double.parseDouble(line[2]);
                    double nodesExceedingEnergyUsage = Double.parseDouble(line[3]);
                    double nodesExceedingQueueSpace = Double.parseDouble(line[4]);
                    double fairnessIndex = Double.parseDouble(line[5]);

                    // Inizializza il punto con i dati di qos_X.csv
                    double[] point = new double[commonDimension];
                    point[0] = packetLoss;
                    point[1] = energyConsumption;
                    point[2] = nodesExceedingEnergyUsage;
                    point[3] = nodesExceedingQueueSpace;
                    point[4] = fairnessIndex;

                    // Aggiungi il punto al modello normale (provvisoriamente, verrà aggiornato con i dati stateY.txt)
                    normalModel.put(runId, new DataPoint(runId, point));
                }
            }
        }

        // Processa i file stateY.txt
        for (File file : stateFiles) {
            int runId = -1;
            double totalBattery = 0;
            double averagePower = 0;
            double totalDistribution = 0;
            int moteCount = 0;
            int linkCount = 0;

            // Regex per estrarre i valori di battery e link
            Pattern batteryPattern = Pattern.compile("battery=([\\d.,]+)/");
            Pattern linkPattern = Pattern.compile("Link \\[.*power=(\\d+), distribution=(\\d+)\\]");

            // Usa BufferedReader per leggere il file riga per riga
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("timestamp:")) {
                        // Ottieni il timestamp (=runId)
                        runId = Integer.parseInt(line.replace("timestamp:", "").trim());
                    }

                    Matcher batteryMatcher = batteryPattern.matcher(line);
                    if (batteryMatcher.find()) {
                        // Somma le batterie dei nodi
                        double battery = Double.parseDouble(batteryMatcher.group(1).replace(',', '.'));
                        totalBattery += battery;
                        moteCount++;
                    }

                    Matcher linkMatcher = linkPattern.matcher(line);
                    if (linkMatcher.find()) {
                        // Somma potenza e distribuzione dei link
                        double power = Double.parseDouble(linkMatcher.group(1));
                        double distribution = Double.parseDouble(linkMatcher.group(2));
                        averagePower += power;
                        totalDistribution += distribution;
                        linkCount++;
                    }
                }
            }

            // Completa il punto aggiungendo i dati di stateY.txt
            if (moteCount > 0 && linkCount > 0 && normalModel.containsKey(runId)) {
                double[] point = normalModel.get(runId).getPoints();
                point[5] = totalBattery / moteCount;       // Batteria media dei nodi
                point[6] = averagePower / linkCount;      // Potenza media sui link
                point[7] = totalDistribution / linkCount; // Distribuzione media sui link
                point[8] = moteCount;                      // Numero di nodi (fisso)
            }
        }
    }



    /**
     *
     * double[] point = new double[commonDimension];
     * point[0] = packetLoss;            Percentuale di pacchetti persi (dal file qos_X.csv)
     * point[1] = energyConsumption;     Consumo energetico (dal file qos_X.csv)
     * point[2] = totalBattery / moteCount;  Batteria media dei nodi (dal file stateY.txt)
     * point[3] = averagePower / linkCount;  Potenza media sui link (dal file stateY.txt)
     * point[4] = totalDistribution / linkCount;  Distribuzione media dei pacchetti sui link (dal file stateY.txt)
     */


    public static boolean checkForAnomaly(int runId, double[] newRuntimeData) { //newRuntimeData = point
        if (!normalModel.containsKey(runId)) {
            System.err.println("timestamp not found "+ runId); //TODO ora siamo in un ambiente simulato e quindi per id intendo timestamp, ma se avessi orari dovrei essere più flessibile
            return false;
        }

        // normal = no anomaly
        double[] normalData = normalModel.get(runId).getPoints();
        EuclideanDistance distanceCalculator = new EuclideanDistance();

        // Calcola la distanza tra i dati runtime e i dati normali
        double distance = distanceCalculator.compute(newRuntimeData, normalData);
        System.out.println("Distanza dai dati normali: " + distance);


        double anomalyThreshold = 300.0;  // TODO capire se la soglia è ok
        if (distance > anomalyThreshold) {
            System.out.println("Anomalia rilevata nella Run ID " + runId);
            return true;
        } else {
            System.out.println("Situazione normale per la Run ID " + runId);
            return false;
        }
    }

    public double getDistance(int runId, double[] newRuntimeData) {


        // normal = no anomaly
        double[] normalData = normalModel.get(runId).getPoints();
        EuclideanDistance distanceCalculator = new EuclideanDistance();

        // Calcola la distanza tra i dati runtime e i dati normali
        double distance = distanceCalculator.compute(newRuntimeData, normalData);
        return distance;
    }

    // Metodo statico per recuperare il modello normale
    public static Map<Integer, DataPoint> getNormalModel() {
        return normalModel;
    }
}
