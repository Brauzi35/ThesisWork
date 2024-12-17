package antiFrag.Utils;

import com.opencsv.CSVReader;
import org.jfree.chart.*;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
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

    public static void main(String[] args) throws IOException {
        String betterPolicyDir = "BetterPolicy";
        String standardPolicyDir = "StandardPolicy";
        String outputDir = "ComparisonCharts";

        String ChallengerPolicyDir = "ChallengerProgression";
        String ChallengeroutputDir = "ChallengerCharts";

        //buildGraphs(betterPolicyDir, standardPolicyDir, outputDir);
        //buildStyledBoxPlotGraphs(betterPolicyDir, standardPolicyDir, outputDir);

        buildGraphs(betterPolicyDir, ChallengerPolicyDir, ChallengeroutputDir);
        //buildStyledBoxPlotGraphs(betterPolicyDir, ChallengerPolicyDir, ChallengeroutputDir);

        createBoxplotComparison(betterPolicyDir, ChallengerPolicyDir, true, "boxplot_before_stop.png");
        createBoxplotComparison(betterPolicyDir, ChallengerPolicyDir, false, "boxplot_after_stop.png");

    }

    private static void buildGraphs(String betterPolicyDir, String standardPolicyDir, String outputDir){
        File[] betterFiles = new File(betterPolicyDir).listFiles((dir, name) -> name.endsWith(".csv"));
        File[] standardFiles = new File(standardPolicyDir).listFiles((dir, name) -> name.endsWith(".csv"));

        if (betterFiles != null && standardFiles != null && betterFiles.length == standardFiles.length) {
            new File(outputDir).mkdirs();

            for (int i = 0; i < betterFiles.length; i++) {
                System.out.println("Generating graph for simulation i=" + i);

                // graph data init
                List<Double> betterPacketLoss = new ArrayList<>();
                List<Double> betterEnergyConsumption = new ArrayList<>();
                List<Double> standardPacketLoss = new ArrayList<>();
                List<Double> standardEnergyConsumption = new ArrayList<>();
                List<Double> betterNumNodesEnergy = new ArrayList<>();
                List<Double> standardNumNodesEnergy = new ArrayList<>();
                List<Double> betterFairnessIndex = new ArrayList<>();
                List<Double> standardFairnessIndex = new ArrayList<>();

                String fileName = betterFiles[i].getName();
                String simulationInfo = fileName.substring(fileName.indexOf("simulation"), fileName.indexOf("_neigh"));
                String neighInfo = fileName.substring(fileName.indexOf("neigh"));

                // read data from csv
                try {
                    readCsvData(betterFiles[i], betterPacketLoss, betterEnergyConsumption, betterNumNodesEnergy, betterFairnessIndex);
                    readCsvData(standardFiles[i], standardPacketLoss, standardEnergyConsumption, standardNumNodesEnergy, standardFairnessIndex);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // creating graphs w Packet Loss, Energy Consumption, NumNodesEnergy e Fairness Index
                JFreeChart packetLossChart = createChart("Packet Loss Comparison - " + simulationInfo + "_" + neighInfo,
                        "Run", "Packet Loss", betterPacketLoss, standardPacketLoss);
                JFreeChart energyChart = createChart("Energy Consumption Comparison - " + simulationInfo + "_" + neighInfo,
                        "Run", "Energy Consumption", betterEnergyConsumption, standardEnergyConsumption);
                JFreeChart numNodesEnergyChart = createChart("# Nodes Exceeding Energy Usage - " + simulationInfo + "_" + neighInfo,
                        "Run", "# Nodes", betterNumNodesEnergy, standardNumNodesEnergy);
                JFreeChart fairnessIndexChart = createChart("Fairness Index Comparison - " + simulationInfo + "_" + neighInfo,
                        "Run", "Fairness Index", betterFairnessIndex, standardFairnessIndex);


                int width = 1600;
                int height = 1200;

                BufferedImage combinedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = combinedImage.createGraphics();

                // draw graphs
                g2.drawImage(packetLossChart.createBufferedImage(800, 600), 0, 0, null);
                g2.drawImage(energyChart.createBufferedImage(800, 600), 800, 0, null);
                g2.drawImage(numNodesEnergyChart.createBufferedImage(800, 600), 0, 600, null);
                g2.drawImage(fairnessIndexChart.createBufferedImage(800, 600), 800, 600, null);

                g2.dispose();

                // save png
                try {
                    String outputFileName = outputDir + "/Comparison_" + simulationInfo + "_" + neighInfo + ".png";
                    ImageIO.write(combinedImage, "png", new File(outputFileName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("directories must contain the same number of files");
        }
    }

    public static void buildStyledBoxPlotGraphs(String betterPolicyDir, String standardPolicyDir, String outputDir) {
        File[] betterFiles = new File(betterPolicyDir).listFiles((dir, name) -> name.endsWith(".csv"));
        File[] standardFiles = new File(standardPolicyDir).listFiles((dir, name) -> name.endsWith(".csv"));

        if (betterFiles != null && standardFiles != null && betterFiles.length == standardFiles.length) {
            new File(outputDir).mkdirs();

            for (int i = 0; i < betterFiles.length; i++) {
                System.out.println("Generating styled boxplot for simulation i=" + i);

                List<Double> betterPacketLoss = new ArrayList<>();
                List<Double> betterEnergyConsumption = new ArrayList<>();
                List<Double> standardPacketLoss = new ArrayList<>();
                List<Double> standardEnergyConsumption = new ArrayList<>();
                List<Double> betterNumNodesEnergy = new ArrayList<>();
                List<Double> standardNumNodesEnergy = new ArrayList<>();
                List<Double> betterFairnessIndex = new ArrayList<>();
                List<Double> standardFairnessIndex = new ArrayList<>();

                String fileName = betterFiles[i].getName();
                String simulationInfo = fileName.substring(fileName.indexOf("simulation"), fileName.indexOf("_neigh"));
                String neighInfo = fileName.substring(fileName.indexOf("neigh"));

                try {
                    readCsvData(betterFiles[i], betterPacketLoss, betterEnergyConsumption, betterNumNodesEnergy, betterFairnessIndex);
                    readCsvData(standardFiles[i], standardPacketLoss, standardEnergyConsumption, standardNumNodesEnergy, standardFairnessIndex);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // boxplots
                JFreeChart packetLossBoxPlot = createStyledBoxPlot("Packet Loss - " + simulationInfo + "_" + neighInfo,
                        "Policy", "Packet Loss", betterPacketLoss, standardPacketLoss);
                JFreeChart energyBoxPlot = createStyledBoxPlot("Energy Consumption - " + simulationInfo + "_" + neighInfo,
                        "Policy", "Energy Consumption", betterEnergyConsumption, standardEnergyConsumption);
                JFreeChart numNodesEnergyBoxPlot = createStyledBoxPlot("# Nodes Exceeding Energy Usage - " + simulationInfo + "_" + neighInfo,
                        "Policy", "# Nodes", betterNumNodesEnergy, standardNumNodesEnergy);
                JFreeChart fairnessIndexBoxPlot = createStyledBoxPlot("Fairness Index - " + simulationInfo + "_" + neighInfo,
                        "Policy", "Fairness Index", betterFairnessIndex, standardFairnessIndex);

                int width = 1600;
                int height = 1200;

                BufferedImage combinedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = combinedImage.createGraphics();

                g2.drawImage(packetLossBoxPlot.createBufferedImage(800, 600), 0, 0, null);
                g2.drawImage(energyBoxPlot.createBufferedImage(800, 600), 800, 0, null);
                g2.drawImage(numNodesEnergyBoxPlot.createBufferedImage(800, 600), 0, 600, null);
                g2.drawImage(fairnessIndexBoxPlot.createBufferedImage(800, 600), 800, 600, null);

                g2.dispose();

                try {
                    String outputFileName = outputDir + "/Styled_BoxPlot_Comparison_" + simulationInfo + "_" + neighInfo + ".png";
                    ImageIO.write(combinedImage, "png", new File(outputFileName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("directories must contain the same number of files");
        }
    }

    // Metodo per creare un boxplot con JFreeChart
    private static JFreeChart createStyledBoxPlot(String title, String categoryAxisLabel, String valueAxisLabel,
                                                  List<Double> betterData, List<Double> standardData) {
        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
        dataset.add(betterData, "Better Policy", categoryAxisLabel);
        dataset.add(standardData, "Standard Policy", categoryAxisLabel);

        JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(
                title,
                categoryAxisLabel,
                valueAxisLabel,
                dataset,
                true //legend
        );

        CategoryPlot plot = (CategoryPlot) boxplot.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setFillBox(true);
        renderer.setSeriesPaint(0, new Color(255, 140, 0)); // Orange - Better Policy
        renderer.setSeriesPaint(1, new Color(100, 149, 237)); // LightBlue - Standard Policy
        renderer.setMeanVisible(true);
        renderer.setMedianVisible(true);
        renderer.setWhiskerWidth(0.5);
        renderer.setMaximumBarWidth(0.1);

        plot.setRenderer(renderer);

        // configs
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        domainAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 14));

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        rangeAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 14));

        // legend
        LegendItemCollection legend = new LegendItemCollection();
        legend.add(new LegendItem("Better Policy", new Color(255, 140, 0)));
        legend.add(new LegendItem("Standard Policy", new Color(100, 149, 237)));
        plot.setFixedLegendItems(legend);

        return boxplot;
    }


    private static void readCsvData(File file, List<Double> packetLoss, List<Double> energyConsumption, List<Double> numNodesEnergy, List<Double> fairness) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] line;
            reader.readNext(); // skip header

            while ((line = reader.readNext()) != null) {
                packetLoss.add(Double.parseDouble(line[1]));
                energyConsumption.add(Double.parseDouble(line[2]));
                numNodesEnergy.add(Double.parseDouble(line[3]));
                fairness.add(Double.parseDouble(line[5]));
            }
        }
    }

    private static JFreeChart createChart(String title, String xAxisLabel, String yAxisLabel,
                                          List<Double> betterData, List<Double> standardData) {
        XYSeries betterSeries = new XYSeries("BetterPolicy");
        XYSeries standardSeries = new XYSeries("StandardPolicy");

        for (int i = 0; i < betterData.size()-1; i++) {
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

    public static void createBoxplotComparison(String betterPolicyDir, String challengerPolicyDir,
                                               boolean beforeStop, String outputFile) throws IOException {
        // Dataset separati per ogni feature
        DefaultBoxAndWhiskerCategoryDataset feature1Dataset = new DefaultBoxAndWhiskerCategoryDataset();
        DefaultBoxAndWhiskerCategoryDataset feature2Dataset = new DefaultBoxAndWhiskerCategoryDataset();
        DefaultBoxAndWhiskerCategoryDataset feature3Dataset = new DefaultBoxAndWhiskerCategoryDataset();
        DefaultBoxAndWhiskerCategoryDataset feature4Dataset = new DefaultBoxAndWhiskerCategoryDataset();

        // Leggi i dati dalle cartelle
        List<double[]> betterPolicyData = readPolicyData(betterPolicyDir, beforeStop);
        List<double[]> challengerPolicyData = readPolicyData(challengerPolicyDir, beforeStop);

        // Aggiungi i dati ai dataset
        addDataToFeatureDataset(feature1Dataset, betterPolicyData, challengerPolicyData, 0, "Feature 1");
        addDataToFeatureDataset(feature2Dataset, betterPolicyData, challengerPolicyData, 1, "Feature 2");
        addDataToFeatureDataset(feature3Dataset, betterPolicyData, challengerPolicyData, 2, "Feature 3");
        addDataToFeatureDataset(feature4Dataset, betterPolicyData, challengerPolicyData, 3, "Feature 4");

        // Crea i grafici boxplot per ogni feature con estetica migliorata
        JFreeChart feature1Chart = createStyledBoxplotChart("Packet Loss Comparison", "Policy", "Packet Loss", feature1Dataset);
        JFreeChart feature2Chart = createStyledBoxplotChart("Energy Consumption Comparison", "Policy", "Energy Consumption", feature2Dataset);
        JFreeChart feature3Chart = createStyledBoxplotChart("Nodes Exceeding Energy Usage Comparison", "Policy", "# Nodes", feature3Dataset);
        JFreeChart feature4Chart = createStyledBoxplotChart("Fairness Index Comparison", "Policy", "Fairness Index", feature4Dataset);

        // Salva i grafici come un'unica immagine
        saveChartsAsSingleImage(outputFile, feature1Chart, feature2Chart, feature3Chart, feature4Chart);
    }

    // Metodo per creare grafici boxplot con uno stile migliorato
    private static JFreeChart createStyledBoxplotChart(String title, String categoryAxisLabel, String valueAxisLabel,
                                                       DefaultBoxAndWhiskerCategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
                title,
                categoryAxisLabel,
                valueAxisLabel,
                dataset,
                true
        );

        // Personalizzazione della grafica
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.white); // Sfondo bianco
        plot.setRangeGridlinePaint(Color.lightGray); // Griglia leggera
        plot.setDomainGridlinePaint(Color.lightGray);

        // Modifica stile dei box e whiskers
        BoxAndWhiskerRenderer renderer = (BoxAndWhiskerRenderer) plot.getRenderer();
        renderer.setFillBox(true);
        renderer.setSeriesPaint(0, new Color(255, 140, 0)); // Rosso chiaro (Better Policy)
        renderer.setSeriesPaint(1, new Color(100, 149, 237)); // Blu chiaro (Standard Policy)
        renderer.setMeanVisible(false); // Nasconde il valore medio
        renderer.setMaximumBarWidth(0.1); // Larghezza delle box

        // Personalizzazione dei titoli e font
        chart.setTitle(new TextTitle(title, new Font("SansSerif", Font.BOLD, 14)));
        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 12));

        return chart;
    }

    public static List<double[]> readPolicyData(String dirPath, boolean beforeStop) throws IOException {
        File dir = new File(dirPath);
        File[] files = dir.listFiles((d, name) -> name.matches("simulation\\d+_neigh\\[\\d+, \\d+\\]_stopAnomaly\\d+\\.csv"));

        List<double[]> featureAverages = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                List<double[]> data = readFile(file, beforeStop);
                featureAverages.addAll(data);
            }
        }

        return featureAverages;
    }

    public static List<double[]> readFile(File file, boolean beforeStop) throws IOException {
        List<double[]> rows = new ArrayList<>();
        int stopAnomalyRow = extractStopAnomaly(file.getName());
        int currentRow = 0;

        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] line;
            boolean isHeader = true;
            while ((line = reader.readNext()) != null) {
                // Salta l'header
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                // Filtra righe in base a beforeStop o afterStop
                if (beforeStop && currentRow >= stopAnomalyRow) break;
                if (!beforeStop && currentRow < stopAnomalyRow) {
                    currentRow++;
                    continue;
                }

                // Estrai colonne richieste (1, 2, 3, 5)
                double col1 = Double.parseDouble(line[1]);
                double col2 = Double.parseDouble(line[2]);
                double col3 = Double.parseDouble(line[3]);
                double col5 = Double.parseDouble(line[5]);

                rows.add(new double[]{col1, col2, col3, col5});
                currentRow++;
            }
        }

        return rows;
    }

    public static int extractStopAnomaly(String fileName) {
        // Estrai il valore di stopAnomalyX dal nome del file
        String stopAnomalyStr = fileName.substring(fileName.indexOf("stopAnomaly") + 11, fileName.lastIndexOf(".csv"));
        return Integer.parseInt(stopAnomalyStr);
    }

    public static void addDataToFeatureDataset(DefaultBoxAndWhiskerCategoryDataset dataset,
                                               List<double[]> betterPolicyData, List<double[]> challengerPolicyData,
                                               int featureIndex, String featureName) {
        // Dati per Better Policy
        List<Double> betterValues = new ArrayList<>();
        for (double[] row : betterPolicyData) {
            betterValues.add(row[featureIndex]);
        }
        dataset.add(betterValues, "Better Policy", featureName);

        // Dati per Challenger Policy
        List<Double> challengerValues = new ArrayList<>();
        for (double[] row : challengerPolicyData) {
            challengerValues.add(row[featureIndex]);
        }
        dataset.add(challengerValues, "Challenger Policy", featureName);
    }

    public static void saveChartsAsSingleImage(String outputFile, JFreeChart chart1, JFreeChart chart2,
                                               JFreeChart chart3, JFreeChart chart4) throws IOException {
        int width = 800;   // Larghezza di ogni singolo grafico
        int height = 600;  // Altezza di ogni singolo grafico
        int rows = 2;      // Due righe di grafici
        int cols = 2;      // Due colonne di grafici

        // Creazione dell'immagine combinata con dimensioni appropriate
        BufferedImage combinedImage = new BufferedImage(width * cols, height * rows, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = combinedImage.createGraphics();

        // Disegna ogni grafico nella posizione appropriata
        chart1.draw(g2, new Rectangle(0, 0, width, height));
        chart2.draw(g2, new Rectangle(width, 0, width, height));
        chart3.draw(g2, new Rectangle(0, height, width, height));
        chart4.draw(g2, new Rectangle(width, height, width, height));

        g2.dispose();

        // Salva l'immagine combinata come file PNG
        File output = new File(outputFile);
        ImageIO.write(combinedImage, "png", output);
        System.out.println("Saved combined chart to: " + output.getAbsolutePath());
    }


}
