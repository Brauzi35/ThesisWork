package antiFrag.Utils;

import simulator.QoS;

import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class CsvWriter {

    public void writeQoSToCSV(ArrayList<QoS> result, String fileName) {

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat decimalFormat = new DecimalFormat("0.######", symbols);

        try (PrintWriter csvWriter = new PrintWriter(new FileWriter(fileName))) {
            // Scrivi l'header
            csvWriter.println("Run,PacketLoss,EnergyConsumption,NumNodesEnergy,FairnessIndex");


            for (int i = 0; i < result.size(); i++) {
                QoS qos = result.get(i);
                String formattedQoS = String.format("%d,%s,%s,%d,%d,%s",
                        i,  // Usa l'indice come "Run"
                        decimalFormat.format(qos.getPacketLoss()),
                        decimalFormat.format(qos.getEnergyConsumption()),
                        qos.getNumNodesEnergy(),
                        qos.getNumNodesLoss(),
                        decimalFormat.format(qos.getFairnessIndex())
                );


                csvWriter.println(formattedQoS);
            }

            System.out.println("data written on " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeOrUpdateScoreToCSV(int runID, double wealthScore, String fileName) {
        // directory Scores if not existent
        File scoresDir = new File("Scores");
        if (!scoresDir.exists()) {
            scoresDir.mkdirs();
        }


        File csvFile = new File(scoresDir, fileName);


        Map<Integer, Double> scoreMap = new LinkedHashMap<>();
        if (csvFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("runID")) continue; // skip header
                    String[] parts = line.split(",");
                    int existingRunID = Integer.parseInt(parts[0]);
                    double existingWealthScore = Double.parseDouble(parts[1]);
                    scoreMap.put(existingRunID, existingWealthScore);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        scoreMap.put(runID, wealthScore);

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            // write header
            writer.println("runID,WealthScore");

            // write all map elements
            for (Map.Entry<Integer, Double> entry : scoreMap.entrySet()) {
                writer.println(entry.getKey() + "," + String.format("%.6f", entry.getValue()));
            }

            System.out.println("updated score file with: runID=" + runID + ", WealthScore=" + wealthScore);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
