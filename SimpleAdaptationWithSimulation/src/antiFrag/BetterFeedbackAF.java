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






    public ArrayList<QoS> start(SimulationClientAF.Case c, int j, int d, boolean firstTime, ArrayList<QoS> bestResult){

        //System.out.println("start!"+d+j);
        int[] pwrs = {1, 2, 3, 4, 5};
        int[] dists = {10, 20, 30, 40, 50};

        ArrayList<QoS> bestResult2 = null;



        // Create Feedback loop
        ReactiveAF feedbackLoop = new ReactiveAF(pwrs[j], dists[d]);

        // get probe and effectors
        Probe probe = networkMgmt.getProbe();
        Effector effector = networkMgmt.getEffector();


        // Connect probe and effectors with feedback loop
        feedbackLoop.setProbe(probe);
        feedbackLoop.setEffector(effector);

        // StartFeedback loop
        //try{
            feedbackLoop.start(); //Start fa partire il loop
            if (firstTime) {
                bestResult2 = networkMgmt.getNetworkQoS(96);
                bestj = 0;
                bestd = 0;
                if(d==0 && j==0) {
                    //System.out.println("primo risultato è quello con indice j,d: "+j+","+d );
                    //bestResult2.forEach(qos -> System.out.println(qos));
                }
                //return bestResult;
                return bestResult2;


            } else {
                ArrayList<QoS> newResult = networkMgmt.getNetworkQoS(96);

                if (isBetterResult(newResult, bestResult)) {
                    this.bestd = d;
                    this.bestj = j;
                    /*
                    System.out.println("Miglior risultato è quello con indice j,d: "+j+","+d );
                    newResult.forEach(qos -> System.out.println(qos));
                    */

                    return newResult;  // Aggiorna bestResult se newResult è migliore


                }
            }



/*

        }catch (ArrayIndexOutOfBoundsException e){
            System.out.println("ArrayIndexOutOfBoundsException for idx j,d="+j+","+d);
            System.err.println(e.getCause());
        }

 */

        return bestResult;


    }

    // Funzione per confrontare due arrayList di QoS e determinare quale è migliore

    /*
    private static boolean isBetterResult(ArrayList<QoS> newResult, ArrayList<QoS> bestResult) {
        int betterPacketLossCount = 0;
        int betterEnergyConsumptionCount = 0;
        int totalComparisons = Math.min(newResult.size(), bestResult.size());

        for (int i = 0; i < totalComparisons; i++) {
            QoS newQoS = newResult.get(i);
            QoS bestQoS = bestResult.get(i);

            // Confronta PacketLoss (valore più basso è migliore)
            if (newQoS.getPacketLoss() < bestQoS.getPacketLoss()) {
                betterPacketLossCount++;
            } else if (newQoS.getPacketLoss() > bestQoS.getPacketLoss()) {
                betterPacketLossCount--;
            }

            // Confronta EnergyConsumption (valore più basso è migliore)
            if (newQoS.getEnergyConsumption() < bestQoS.getEnergyConsumption()) {
                betterEnergyConsumptionCount++;
            } else if (newQoS.getEnergyConsumption() > bestQoS.getEnergyConsumption()) {
                betterEnergyConsumptionCount--;
            }
        }

        // Decidi se newResult è migliore (maggioranza di confronti favorevoli)
        return (betterPacketLossCount > 0 && betterEnergyConsumptionCount > 0);
    }

     */

    private static boolean isBetterResult(ArrayList<QoS> newResult, ArrayList<QoS> bestResult) {
        // Calcola la discesa rapida e il numero di cambi di direzione per la lista newResult
        double newResultRapidDrop = calculateRapidDrop(newResult);
        int newResultMonotonicity = calculateMonotonicity(newResult);

        // Calcola la discesa rapida e il numero di cambi di direzione per la lista bestResult
        double bestResultRapidDrop = calculateRapidDrop(bestResult);
        int bestResultMonotonicity = calculateMonotonicity(bestResult);

        // Definizione di criteri di confronto
        // 1. Verifica quale lista ha la discesa più rapida (valore più negativo è migliore)
        if (newResultRapidDrop < bestResultRapidDrop) {
            return true;
        } else if (newResultRapidDrop == bestResultRapidDrop) {
            // 2. Se la discesa è uguale, confronta la monotonicità (minori cambi di direzione è meglio)
            return newResultMonotonicity < bestResultMonotonicity;
        }
        return false;
    }

    /**
     * Calcola la discesa totale rapida per una lista di QoS.
     * @param result - Lista di QoS.
     * @return Somma delle variazioni negative di packetLoss e energyConsumption.
     */
    private static double calculateRapidDrop(ArrayList<QoS> result) {
        double rapidDrop = 0.0;
        for (int i = 1; i < result.size(); i++) {
            // Calcola la variazione per packetLoss
            double packetLossDrop = result.get(i).getPacketLoss() - result.get(i - 1).getPacketLoss();
            if (packetLossDrop < 0) rapidDrop += packetLossDrop;

            // Calcola la variazione per energyConsumption
            double energyDrop = result.get(i).getEnergyConsumption() - result.get(i - 1).getEnergyConsumption();
            if (energyDrop < 0) rapidDrop += energyDrop;
        }
        return rapidDrop;
    }

    /**
     * Calcola la monotonicità di una lista di QoS.
     * @param result - Lista di QoS.
     * @return Numero di cambi di direzione (più basso è meglio).
     */
    private static int calculateMonotonicity(ArrayList<QoS> result) {
        int directionChanges = 0;
        boolean increasing = true;

        for (int i = 1; i < result.size(); i++) {
            // Controlla se packetLoss cambia direzione
            if ((result.get(i).getPacketLoss() > result.get(i - 1).getPacketLoss()) != increasing) {
                directionChanges++;
                increasing = !increasing;
            }

            // Controlla se energyConsumption cambia direzione
            if ((result.get(i).getEnergyConsumption() > result.get(i - 1).getEnergyConsumption()) != increasing) {
                directionChanges++;
                increasing = !increasing;
            }
        }
        return directionChanges;
    }


    private void initC1(SimulationClientAF.Case c, int x, int y, double battery, int load, int niegh){
        // Create a simulation client object
        networkMgmt = new SimulationClientAF(c,x,y,battery,load,niegh);
    }

    public BetterFeedbackAF(SimulationClientAF networkMgmt) {
        this.networkMgmt = networkMgmt;
    }

    public BetterFeedbackAF() {
    }

    public static void main(String[] args) {
        //SimulationAF client = new SimulationAF();
        //client.start(SimulationClientAF.Case.DEFAULT);
        System.out.println("entrato nel main");
        FindPositionAndNeighbour fpn = new FindPositionAndNeighbour();
        for (int i = 0; i<100; i++){
            Point2D point2D = getPosition();
            int neigh = findClosestNode(point2D);
            System.out.println(neigh);
            boolean firstTime = true;
            ArrayList<QoS> best = null;
            int bestj = 0;
            int bestd = 0;

            for(int j = 0; j<5; j++) {
                for (int d = 0; d < 5; d++) {

                    BetterFeedbackAF client = new BetterFeedbackAF();




                    client.initC1(SimulationClientAF.Case.CASE1, (int) point2D.getX(), (int) point2D.getY(), 118800.0, 200, neigh);
                    ArrayList<QoS> temp = client.start(SimulationClientAF.Case.CASE1, j, d, firstTime, best);
                    if(!temp.equals(best)){
                        bestj = j;
                        bestd = d;
                    }
                    best = temp;
                    firstTime = false;
                }
            }
            System.out.println("Miglior risultato finale con j,d = "+bestj+","+bestd );
            best.forEach(qos -> System.out.println(qos));
            writeQoSToCSV(best, "BetterPolicy/bestConf_neigh_"+neigh+"_iteration_"+i+"_jd_"+bestj+","+bestd+".csv");
        }
    }

    public Simulator getSimulator() {
        return networkMgmt.getSimulator();
    }
}
