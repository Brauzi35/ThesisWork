package antiFrag.Utils;

import com.opencsv.CSVReader;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ChartUtils;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

public class ComparePolicies {

    public static void main(String[] args) {
        String betterPolicyDir = "BetterPolicy";
        String standardPolicyDir = "StandardPolicy";
        String outputDir = "ComparisonCharts"; // Directory di output per i grafici

        File[] betterFiles = new File(betterPolicyDir).listFiles((dir, name) -> name.endsWith(".csv"));
        File[] standardFiles = new File(standardPolicyDir).listFiles((dir, name) -> name.endsWith(".csv"));

        if (betterFiles != null && standardFiles != null && betterFiles.length == standardFiles.length) {
            new File(outputDir).mkdirs(); // Crea la directory di output se non esiste

            for (int i = 0; i < betterFiles.length; i++) {
                System.out.println("generating graph for simulation i="+i);
                List<Double> betterPacketLoss = new ArrayList<>();
                List<Double> betterEnergyConsumption = new ArrayList<>();
                List<Double> standardPacketLoss = new ArrayList<>();
                List<Double> standardEnergyConsumption = new ArrayList<>();

                String fileName = betterFiles[i].getName();
                String simulationInfo = fileName.substring(fileName.indexOf("simulation"), fileName.indexOf("_neigh"));
                String neighInfo = fileName.substring(fileName.indexOf("neigh"));

                // Lettura dei dati dai file CSV
                try {
                    readCsvData(betterFiles[i], betterPacketLoss, betterEnergyConsumption);
                    readCsvData(standardFiles[i], standardPacketLoss, standardEnergyConsumption);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Creazione dei grafici per Packet Loss e Energy Consumption
                JFreeChart packetLossChart = createChart("Packet Loss Comparison - " + simulationInfo + "_" + neighInfo,
                        "Run", "Packet Loss", betterPacketLoss, standardPacketLoss);
                JFreeChart energyChart = createChart("Energy Consumption Comparison - " + simulationInfo + "_" + neighInfo,
                        "Run", "Energy Consumption", betterEnergyConsumption, standardEnergyConsumption);

                // Creazione di una singola immagine contenente entrambi i grafici affiancati
                int width = 1600; // Larghezza totale (800 per ogni grafico)
                int height = 600; // Altezza dei grafici

                BufferedImage combinedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = combinedImage.createGraphics();

                // Disegna il primo grafico (Packet Loss) a sinistra
                BufferedImage packetLossImage = packetLossChart.createBufferedImage(800, 600);
                g2.drawImage(packetLossImage, 0, 0, null);

                // Disegna il secondo grafico (Energy Consumption) a destra
                BufferedImage energyImage = energyChart.createBufferedImage(800, 600);
                g2.drawImage(energyImage, 800, 0, null);

                g2.dispose();

                // Salvataggio dell'immagine combinata come PNG
                try {
                    String outputFileName = outputDir + "/Comparison_" + simulationInfo + "_" + neighInfo + ".png";
                    ImageIO.write(combinedImage, "png", new File(outputFileName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("Le directory non contengono lo stesso numero di file.");
        }
    }

    private static void readCsvData(File file, List<Double> packetLoss, List<Double> energyConsumption) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] line;
            reader.readNext(); // Salta l'intestazione

            while ((line = reader.readNext()) != null) {
                packetLoss.add(Double.parseDouble(line[1]));
                energyConsumption.add(Double.parseDouble(line[2]));
            }
        }
    }

    private static JFreeChart createChart(String title, String xAxisLabel, String yAxisLabel,
                                          List<Double> betterData, List<Double> standardData) {
        XYSeries betterSeries = new XYSeries("BetterPolicy");
        XYSeries standardSeries = new XYSeries("StandardPolicy");

        for (int i = 0; i < betterData.size(); i++) {
            betterSeries.add(i, betterData.get(i));
            standardSeries.add(i, standardData.get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(betterSeries);
        dataset.addSeries(standardSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                xAxisLabel,
                yAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(true);

        return chart;
    }
}
