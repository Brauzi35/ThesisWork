package antiFrag.Utils;

import com.opencsv.CSVReader;
import org.jfree.chart.*;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
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

    public static void main(String[] args) {
        String betterPolicyDir = "BetterPolicy";
        String standardPolicyDir = "StandardPolicy";
        String outputDir = "ComparisonCharts";

        String ChallengerPolicyDir = "ChallengerProgression";
        String ChallengeroutputDir = "ChallengerCharts";

        //buildGraphs(betterPolicyDir, standardPolicyDir, outputDir);
        //buildStyledBoxPlotGraphs(betterPolicyDir, standardPolicyDir, outputDir);

        buildGraphs(betterPolicyDir, ChallengerPolicyDir, ChallengeroutputDir);
        buildStyledBoxPlotGraphs(betterPolicyDir, ChallengerPolicyDir, ChallengeroutputDir);

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
                fairness.add(Double.parseDouble(line[4]));
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
}
