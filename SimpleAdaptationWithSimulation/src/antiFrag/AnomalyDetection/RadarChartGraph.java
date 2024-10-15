package antiFrag.AnomalyDetection;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.SpiderWebPlot;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RadarChartGraph extends JFrame {

    private Map<Integer, AnomalyDetection.DataPoint> normalModel;
    private int runIdToDisplay;

    public RadarChartGraph(Map<Integer, AnomalyDetection.DataPoint> normalModel, int runIdToDisplay) {
        this.normalModel = normalModel;
        this.runIdToDisplay = runIdToDisplay;
        setTitle("Radar Chart for Run ID: " + runIdToDisplay);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setContentPane(createChartPanel());
    }

    private JPanel createChartPanel() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // Estrai i dati per il runId specificato
        AnomalyDetection.DataPoint dataPoint = normalModel.get(runIdToDisplay);
        if (dataPoint == null) {
            JOptionPane.showMessageDialog(this, "No data found for Run ID: " + runIdToDisplay);
            return new JPanel();
        }

        double[] points = dataPoint.getPoints();

        // Aggiungi i dati al dataset per il radar chart
        dataset.addValue(points[0], "Run " + runIdToDisplay, "PacketLoss");
        dataset.addValue(points[1], "Run " + runIdToDisplay, "EnergyConsumption");
        dataset.addValue(points[2], "Run " + runIdToDisplay, "Battery");
        dataset.addValue(points[3], "Run " + runIdToDisplay, "Power");
        dataset.addValue(points[4], "Run " + runIdToDisplay, "Distribution");

        // Creazione del radar chart (SpiderWebPlot)
        SpiderWebPlot plot = new SpiderWebPlot(dataset);
        JFreeChart chart = new JFreeChart("Radar Chart for Run ID " + runIdToDisplay, JFreeChart.DEFAULT_TITLE_FONT, plot, false);

        // Personalizza il renderer del radar chart
        plot.setStartAngle(0);
        plot.setInteriorGap(0.4);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(true);

        return new ChartPanel(chart);
    }

    public static void main(String[] args) {
        AnomalyDetection ad = new AnomalyDetection();

        try {
            // Directory dei file
            String baseDir = "AnomalyDetectionFiles";

            // Carica i file qos_X.csv e stateY.txt
            java.util.List<File> qosFiles = Files.list(Paths.get(baseDir))
                    .filter(p -> p.toString().contains("qos_"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            List<File> stateFiles = Files.list(Paths.get(baseDir))
                    .filter(p -> p.toString().contains("state"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            // Costruisce il modello normale
            ad.buildNormalModel(qosFiles, stateFiles);
        }catch (IOException e) {
            e.printStackTrace();
        }

        // Supponiamo che il normalModel sia già stato creato con AnomalyDetection
        Map<Integer, AnomalyDetection.DataPoint> normalModel = ad.getNormalModel();  // Metodo ipotetico per recuperare il modello normale

        // Specifica l'ID della run che vuoi visualizzare
        int runIdToDisplay = 93;  // Cambia con l'ID della run che desideri

        // Crea il grafico e mostra la finestra
        SwingUtilities.invokeLater(() -> {
            RadarChartGraph example = new RadarChartGraph(normalModel, runIdToDisplay);
            example.setVisible(true);
        });
    }
}
