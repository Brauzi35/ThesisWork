package antiFrag.Utils;

import deltaiot.client.SimulationClient;
import simulator.QoS;

import java.util.ArrayList;

/**
 * calculates a score to better understand network's conditions
 */
public class WealthScore {

    private double score = 0.0;

    private static final double weight = 1.0;



    // Funzione di supporto per la normalizzazione min-max
    private static double normalize(double value, double min, double max) {
        if (max == min) {
            return 0.0; // Evita divisione per zero se tutti i valori sono uguali
        }
        return (value - min) / (max - min);
    }


    public double getScore(ArrayList<QoS> finalResult){

        for(QoS qos : finalResult) {
            // Normalizza i valori di QoS
            double normalizedPacketLoss = normalize(qos.getPacketLoss(), 0, 1); // Range: 0 (min) to 1 (max)
            double normalizedEnergyConsumption = normalize(qos.getEnergyConsumption(), 0, 1); // Range: 0 (min) to 1 (max)
            double normalizedNumNodesEnergy = normalize(qos.getNumNodesEnergy(), 0, 1); // Range: 0 (min) to 1 (max)
            double normalizedNumNodesLoss = normalize(qos.getNumNodesLoss(), 0, 1); // Range: 0 (min) to 1 (max)
            double normalizedFairnessIndex = normalize(qos.getFairnessIndex(), 0, 1); // Range: 0 (min) to 1 (max)

            score += (1 - normalizedPacketLoss); // Minore è meglio
            score += (1 - normalizedEnergyConsumption); // Minore è meglio
            score += (1 - normalizedNumNodesEnergy); // Minore è meglio
            score += (1 - normalizedNumNodesLoss); // Minore è meglio
            score += normalizedFairnessIndex; // Maggiore è meglio
        }

        return score;
    }


}
