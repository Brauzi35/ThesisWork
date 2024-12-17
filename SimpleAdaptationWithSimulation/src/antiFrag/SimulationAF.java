package antiFrag;

import antiFrag.Position.FindPositionAndNeighbour;
import antiFrag.Utils.CsvWriter;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.services.Mote;
import mapek.FeedbackLoop;
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


public class SimulationAF {

    SimulationClientAF networkMgmt;

    public static void writeMotesToCsv(List<Mote> motes, String fileName) {
        try (FileWriter csvWriter = new FileWriter(fileName)) {
            // write header
            csvWriter.append("id,parents,battery,load,dataProbability\n");

            // on motes
            for (Mote m : motes) {
                // get string resume for mote
                String moteString = m.toString();

                // get MoteId, Parents, Battery, Load and DataProbability
                String[] values = parseMoteString(moteString);

                // write values in CSV
                csvWriter.append(String.join(",", values));
                csvWriter.append("\n"); // Nuova riga per ogni Mote
            }

            // Flush and close
            csvWriter.flush();
            System.out.println("CSV successfully generated: " + fileName);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static String[] parseMoteString(String moteString) {
        // example: MoteId=1, Parents=3, Battery=11578,058304, Load=100, DataProbability=90

        moteString = moteString.replace("MoteId=", "")
                .replace("Parents=", "")
                .replace("Battery=", "")
                .replace("Load=", "")
                .replace("DataProbability=", "");

        // now we have: "1, 3, 11578,058304, 100, 90"

        // split on commas
        String[] parts = moteString.split(", ");


        String moteId = parts[0]; // "1"
        String parents = parts[1]; // "3"

        // correct float format
        String battery = parts[2].replace(",", "."); // "11578.058304"

        String load = parts[3]; // "100"
        String dataProbability = parts[4]; // "90"

        return new String[]{moteId, parents, battery, load, dataProbability};
    }






    public void start(SimulationClientAF.Case c, int i, int neigh){


        // Create Feedback loop
        FeedbackLoop feedbackLoop = new FeedbackLoop();

        // get probe and effectors
        Probe probe = networkMgmt.getProbe();
        Effector effector = networkMgmt.getEffector();

        // Connect probe and effectors with feedback loop
        feedbackLoop.setProbe(probe);
        feedbackLoop.setEffector(effector);

        // StartFeedback loop
        feedbackLoop.start(new int[]{1, 1, 10}); //Start fa partire il loop

        ArrayList<QoS> result = networkMgmt.getNetworkQoS(96);
        CsvWriter wrt = new CsvWriter();
        wrt.writeQoSToCSV(result, "StandardPolicy/sumulation"+i+"_neigh"+neigh+".csv");
        System.out.println("Run, PacketLoss, EnergyConsumption, #motesTooMuchEnergy, #motesTooMuchQueue, fairnessIndex");
        result.forEach(qos -> System.out.println(qos));

        if(c.equals(SimulationClientAF.Case.CASE1)) {
            System.out.println("traffic load for mote 16: " + probe.getMoteTrafficLoad(16));
            System.out.println("energy for mote 16: " + probe.getMoteEnergyLevel(16));
        }

        ArrayList<Mote> motes = networkMgmt.getAllMotes();
        //writeMotesToCsv(motes, "SimulationAFDataC1/motes_data"+i+"_neighbour"+neigh+".csv");


    }


    public Simulator getSimulator() {
        return networkMgmt.getSimulator();
    }

}
