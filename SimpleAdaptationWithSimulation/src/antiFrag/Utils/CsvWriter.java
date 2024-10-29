package antiFrag.Utils;

import simulator.QoS;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

public class CsvWriter {

    public void writeQoSToCSV(ArrayList<QoS> result, String fileName) {
        // Impostare il formattatore decimale per usare il punto come separatore decimale
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);  // Usa il punto decimale
        DecimalFormat decimalFormat = new DecimalFormat("0.######", symbols);  // Massimo 6 cifre decimali

        try (PrintWriter csvWriter = new PrintWriter(new FileWriter(fileName))) {
            // Scrivi l'header
            csvWriter.println("Run,PacketLoss,EnergyConsumption");

            // Scrivi i dati, stampa anche su schermo
            for (int i = 0; i < result.size(); i++) {
                QoS qos = result.get(i);
                String formattedQoS = String.format("%d,%s,%s",
                        i,  // Usa l'indice come "Run"
                        decimalFormat.format(qos.getPacketLoss()),
                        decimalFormat.format(qos.getEnergyConsumption())
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
}
