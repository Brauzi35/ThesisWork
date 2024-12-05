package antiFrag.Utils;

import deltaiot.client.SimulationClient;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import simulator.QoS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * calculates a score to better understand network's conditions
 */
public class WealthScore {

    private double score = 0.0;

    private static final double weight = 1.0;
    private static Map<String, double[]> minMaxValues = new HashMap<>(); // min e max for each QoS



    private static double normalize(double value, double min, double max) {
        if (max == min) {
            return 0.0; // Evita divisione per zero se tutti i valori sono uguali
        }
        return (value - min) / (max - min);
    }


    public double calculateScore(ArrayList<QoS> qosList) {
        if (qosList.isEmpty()) {
            throw new IllegalArgumentException("QoS list cannot be empty.");
        }

        // max and min to achieve proper normalization
        double minPacketLoss = Double.MAX_VALUE, maxPacketLoss = Double.MIN_VALUE;
        double minEnergyConsumption = Double.MAX_VALUE, maxEnergyConsumption = Double.MIN_VALUE;
        double minNumNodesEnergy = Double.MAX_VALUE, maxNumNodesEnergy = Double.MIN_VALUE;
        double minNumNodesLoss = Double.MAX_VALUE, maxNumNodesLoss = Double.MIN_VALUE;
        double minFairnessIndex = Double.MAX_VALUE, maxFairnessIndex = Double.MIN_VALUE;

        for (QoS qos : qosList) {
            minPacketLoss = Math.min(minPacketLoss, qos.getPacketLoss());
            maxPacketLoss = Math.max(maxPacketLoss, qos.getPacketLoss());
            minEnergyConsumption = Math.min(minEnergyConsumption, qos.getEnergyConsumption());
            maxEnergyConsumption = Math.max(maxEnergyConsumption, qos.getEnergyConsumption());
            minNumNodesEnergy = Math.min(minNumNodesEnergy, qos.getNumNodesEnergy());
            maxNumNodesEnergy = Math.max(maxNumNodesEnergy, qos.getNumNodesEnergy());
            minNumNodesLoss = Math.min(minNumNodesLoss, qos.getNumNodesLoss());
            maxNumNodesLoss = Math.max(maxNumNodesLoss, qos.getNumNodesLoss());
            minFairnessIndex = Math.min(minFairnessIndex, qos.getFairnessIndex());
            maxFairnessIndex = Math.max(maxFairnessIndex, qos.getFairnessIndex());
        }

        // get score
        double score = 0.0;

        for (QoS qos : qosList) {
            // Norm
            //double normalizedPacketLoss = normalize(qos.getPacketLoss(), minPacketLoss, maxPacketLoss);
            //double normalizedEnergyConsumption = normalize(qos.getEnergyConsumption(), minEnergyConsumption, maxEnergyConsumption);
            double normalizedNumNodesEnergy = normalize(qos.getNumNodesEnergy(), minNumNodesEnergy, maxNumNodesEnergy);
            double normalizedNumNodesLoss = normalize(qos.getNumNodesLoss(), minNumNodesLoss, maxNumNodesLoss);
            double normalizedFairnessIndex = normalize(qos.getFairnessIndex(), minFairnessIndex, maxFairnessIndex);

            // the smaller, the better
            score += (1 - normalizedNumNodesEnergy); // NumNodesEnergy
            score += (1 - normalizedNumNodesLoss); // NumNodesLoss

            // the bigger, the better
            score += normalizedFairnessIndex; // FairnessIndex
        }

        return score;
    }




    public static double calculateScoreFromCSV(String directoryPath) throws IOException {
        double totalScore = 0.0;


        calculateMinMaxValues(directoryPath);

        File folder = new File(directoryPath);
        File[] files = folder.listFiles((dir, name) -> name.startsWith("qos_") && name.endsWith(".csv"));

        if (files == null || files.length == 0) {
            System.out.println("No files found in the directory starting with 'qos_'.");
            return totalScore;
        }


        for (File file : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                boolean isHeader = true;

                while ((line = br.readLine()) != null) {
                    if (isHeader) { // Skip the header
                        isHeader = false;
                        continue;
                    }

                    String[] values = line.split(",");
                    if (values.length < 6) {
                        System.out.println("Skipping malformed line in file: " + file.getName());
                        continue;
                    }



                    int numNodesEnergy = Integer.parseInt(values[3]);
                    int numNodesLoss = Integer.parseInt(values[4]);
                    double fairnessIndex = Double.parseDouble(values[5]);


                    double normalizedNumNodesEnergy = normalizeWithMinMax(numNodesEnergy, "NumNodesEnergy");
                    double normalizedNumNodesLoss = normalizeWithMinMax(numNodesLoss, "NumNodesLoss");
                    double normalizedFairnessIndex = normalizeWithMinMax(fairnessIndex, "FairnessIndex");

                    // get score
                    double rowScore = 0.0;
                    rowScore += (1 - normalizedNumNodesEnergy); // Lower is better
                    rowScore += (1 - normalizedNumNodesLoss); // Lower is better
                    rowScore += normalizedFairnessIndex; // Higher is better

                    totalScore += rowScore;
                }
            }
        }

        return totalScore;
    }

    private static void calculateMinMaxValues(String directoryPath) throws IOException {
        File folder = new File(directoryPath);
        File[] files = folder.listFiles((dir, name) -> name.startsWith("qos_") && name.endsWith(".csv"));

        if (files == null || files.length == 0) {
            throw new IOException("No QoS files found in the directory for min-max calculation.");
        }

        for (File file : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                boolean isHeader = true;

                while ((line = br.readLine()) != null) {
                    if (isHeader) { // Skip the header
                        isHeader = false;
                        continue;
                    }

                    String[] values = line.split(",");
                    if (values.length < 6) continue;


                    updateMinMax("NumNodesEnergy", Integer.parseInt(values[3]));
                    updateMinMax("NumNodesLoss", Integer.parseInt(values[4]));
                    updateMinMax("FairnessIndex", Double.parseDouble(values[5]));
                }
            }
        }
    }

    private static void updateMinMax(String key, double value) {
        minMaxValues.putIfAbsent(key, new double[]{Double.MAX_VALUE, Double.MIN_VALUE});
        double[] minMax = minMaxValues.get(key);

        minMax[0] = Math.min(minMax[0], value); // Update min
        minMax[1] = Math.max(minMax[1], value); // Update max
    }

    private static double normalizeWithMinMax(double value, String key) {
        double[] minMax = minMaxValues.get(key);
        if (minMax == null) {
            throw new IllegalArgumentException("MinMax values not found for key: " + key);
        }

        double min = minMax[0];
        double max = minMax[1];
        if (min == max) return 0.0; // avoid x/0
        return (value - min) / (max - min);
    }



    public static void main(String[] args) throws IOException {
        String scoresDir = "Scores";
        String antifragFile = "Scores/antifrag.csv";
        String challengerFile = "Scores/challenger.csv";

        String directoryPath = "AnomalydetectionFiles";
        double stdWealth = calculateScoreFromCSV(directoryPath)/100;


        new File(scoresDir).mkdirs();

        // read CSV
        List<Double> antifragScores = readScoresFromCSV(antifragFile);
        List<Double> challengerScores = readScoresFromCSV(challengerFile);

        // files should have same rows number
        if (antifragScores.size() != challengerScores.size()) {
            System.err.println("files should have same rows number");
            return;
        }

        for (int i = 0; i < antifragScores.size(); i++) {
            // get values in a row
            System.out.println("progression: " + i + "/100");
            double antifragScore = antifragScores.get(i);
            double challengerScore = challengerScores.get(i);



            // create histogram
            createHistogram(i, antifragScore, challengerScore, stdWealth, scoresDir);
        }

        System.out.println("histograms successfully generated.");
    }

    private static List<Double> readScoresFromCSV(String filePath) throws IOException {
        List<Double> scores = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean skipHeader = true;
            while ((line = reader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false; // skip header
                    continue;
                }
                String[] parts = line.split(",");
                scores.add(Double.parseDouble(parts[1]));
            }
        }
        return scores;
    }


    private static void createHistogram(int rowIndex, double antifragScore, double challengerScore, double stdWealth, String outputDir) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // adding columns
        dataset.addValue(antifragScore, "Antifrag", "Antifrag");
        dataset.addValue(challengerScore, "Challenger", "Challenger");
        dataset.addValue(stdWealth, "stdWealth", "stdWealth");

        // create chart
        JFreeChart chart = ChartFactory.createBarChart(
                "Comparison for Run " + (rowIndex), // Title
                "Category", // X label
                "Score", // Y label
                dataset
        );

        // save
        try {
            String outputFileName = outputDir + "/Histogram_Run_" + (rowIndex) + ".png";
            ChartUtils.saveChartAsPNG(new File(outputFileName), chart, 800, 600);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}
