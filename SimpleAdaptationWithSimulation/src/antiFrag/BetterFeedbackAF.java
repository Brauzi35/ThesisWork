package antiFrag;

import antiFrag.LearningAF.ReactiveAF;
import antiFrag.Position.FindPositionAndNeighbour;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.services.Mote;
import simulator.QoS;
import simulator.Simulator;

import java.awt.geom.Point2D;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static antiFrag.Position.FindPositionAndNeighbour.findClosestNode;
import static antiFrag.Position.FindPositionAndNeighbour.getPosition;


public class BetterFeedbackAF {
    /**
     * outdated
     */

    SimulationClientAF networkMgmt;
    int bestj = 0;
    int bestd = 0;

    public static void writeMotesToCsv(List<Mote> motes, String fileName) {
        try (FileWriter csvWriter = new FileWriter(fileName)) {
            // Scrivi l'header del CSV
            csvWriter.append("id,parents,battery,load,dataProbability\n");

            // Itera sui motes
            for (Mote m : motes) {
                // Ottieni la stringa dal metodo toString() del Mote
                String moteString = m.toString();

                // Estrai i valori di MoteId, Parents, Battery, Load e DataProbability
                String[] values = parseMoteString(moteString);

                // Scrivi i valori nel CSV
                csvWriter.append(String.join(",", values));
                csvWriter.append("\n"); // Nuova riga per ogni Mote
            }

            // Flush e chiudi il writer
            csvWriter.flush();
            System.out.println("CSV creato con successo: " + fileName);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Metodo che analizza la stringa formattata dal metodo toString() del Mote
    private static String[] parseMoteString(String moteString) {
        // Esempio di formato: MoteId=1, Parents=3, Battery=11578,058304, Load=100, DataProbability=90
        // Usa un'espressione regolare per estrarre i campi correttamente

        // Rimuoviamo il prefisso e prendiamo solo i valori
        moteString = moteString.replace("MoteId=", "")
                .replace("Parents=", "")
                .replace("Battery=", "")
                .replace("Load=", "")
                .replace("DataProbability=", "");

        // A questo punto la stringa appare come: "1, 3, 11578,058304, 100, 90"

        // Ora dobbiamo dividere sui valori principali e sostituire le virgole decimali nelle batterie
        String[] parts = moteString.split(", ");

        // Riuniamo i campi correttamente se battery è separato a causa della virgola
        String moteId = parts[0]; // "1"
        String parents = parts[1]; // "3"

        // Battery è nel formato "11578,058304", sostituiamo la virgola con un punto per evitare problemi
        String battery = parts[2].replace(",", "."); // "11578.058304"

        String load = parts[3]; // "100"
        String dataProbability = parts[4]; // "90"

        return new String[]{moteId, parents, battery, load, dataProbability};
    }

    public static void writeQoSToCSV(ArrayList<QoS> result, String fileName) {
        // Impostare il formattatore decimale per usare il punto come separatore decimale
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);  // Usa il punto decimale
        DecimalFormat decimalFormat = new DecimalFormat("0.######", symbols);  // Massimo 6 cifre decimali

        try (PrintWriter csvWriter = new PrintWriter(new FileWriter(fileName))) {
            // Scrivi l'header
            csvWriter.println("Run,PacketLoss,EnergyConsumption,NodesExceedingEnergyusage,NodesExceedingQueueSpace,FairnessIndex");

            // Scrivi i dati, stampa anche su schermo
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

                // Stampa a schermo
                //System.out.println(formattedQoS);

                // Scrivi nel CSV
                csvWriter.println(formattedQoS);
            }

            System.out.println("Dati scritti su " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }










    public Simulator getSimulator() {
        return networkMgmt.getSimulator();
    }
}
