package antiFrag.AnomalyDetection;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.xy.DefaultXYDataset;

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

public class NormalPointGraph extends JFrame {

    private Map<Integer, AnomalyDetection.DataPoint> normalModel;

    public NormalPointGraph(Map<Integer, AnomalyDetection.DataPoint> normalModel) {
        this.normalModel = normalModel;
        setTitle("Normal Points Graph");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setContentPane(createChartPanel());
    }

    private JPanel createChartPanel() {
        // Crea un dataset per il grafico scatter
        DefaultXYDataset dataset = new DefaultXYDataset();

        // Estrai i dati dal normalModel (uso di packetLoss e energyConsumption come assi x e y)
        double[][] data = new double[2][normalModel.size()];
        int index = 0;
        for (Map.Entry<Integer, AnomalyDetection.DataPoint> entry : normalModel.entrySet()) {
            double[] points = entry.getValue().getPoints();
            data[0][index] = points[0];  // packetLoss sull'asse X
            data[1][index] = points[1];  // energyConsumption sull'asse Y
            index++;
        }

        // Aggiungi i dati al dataset con un nome per la serie di punti
        dataset.addSeries("Normal Points", data);

        // Crea il grafico scatter
        JFreeChart chart = ChartFactory.createScatterPlot(
                "Normal Points Scatter Plot",       // Titolo del grafico
                "Packet Loss",                      // Etichetta asse X
                "Energy Consumption",               // Etichetta asse Y
                dataset,                            // Dati del grafico
                PlotOrientation.VERTICAL,           // Orientamento del grafico
                true,                               // Legenda
                true,                               // Tooltips
                false                               // URLs
        );

        // Personalizza il renderer del grafico per i punti
        XYPlot plot = (XYPlot) chart.getPlot();
        XYDotRenderer renderer = new XYDotRenderer();
        renderer.setDotWidth(5);
        renderer.setDotHeight(5);
        plot.setRenderer(renderer);

        return new ChartPanel(chart);
    }

    public static void main(String[] args) {
        // Supponiamo che il normalModel sia già stato creato con AnomalyDetection
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
            AnomalyDetection.buildNormalModel(qosFiles, stateFiles);

            // Recupera il modello normale
            Map<Integer, AnomalyDetection.DataPoint> normalModel = AnomalyDetection.getNormalModel();

            // Crea il grafico e mostra la finestra
            SwingUtilities.invokeLater(() -> {
                NormalPointGraph example = new NormalPointGraph(normalModel);
                example.setVisible(true);
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

