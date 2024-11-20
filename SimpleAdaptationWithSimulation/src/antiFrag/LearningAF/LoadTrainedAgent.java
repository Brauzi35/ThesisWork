package antiFrag.LearningAF;



import antiFrag.SimulationClientAF;
import com.github.chen0040.rl.learning.qlearn.QLearner;
import deltaiot.client.SimulationClient;
import domain.Gateway;
import mapek.FeedbackLoopRL;
import simulator.QoS;
import simulator.Simulator;

import java.awt.geom.Point2D;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Objects;

import static antiFrag.LearningAF.RL.*;
import static antiFrag.Position.FindPositionAndNeighbour.findClosestNode;
import static antiFrag.Position.FindPositionAndNeighbour.getPosition;

public class LoadTrainedAgent {
    static Random random = new Random();



    public int[][] interrogation(String pathJson, int neigh, Simulator actualNetwork, int[] currentPowerAdd, int[] currentPowerSub, int[] currentDistribution){ //TODO passare anche il tipo di anomalia
        // Carica l'agente dal file JSON salvato durante il training
        QLearner trainedAgent = loadAgentFromJson(pathJson);

        // Controllo che l'agente sia stato caricato correttamente
        if (trainedAgent != null) {
            System.out.println("Agent successfully loaded from JSON.");


            // Definisci la potenza e la distribuzione iniziali
            //int currentPowerAdd = 1;
            //int currentPowerSub = 1;
            //int currentDistribution = 10;

            List<QoS> currentSituation = actualNetwork.getQosValues();
            double avg_en = getAverageEnergy(currentSituation);
            double avg_loss = getAveragePacketLoss(currentSituation);

            // Ottieni lo stato corrente
            int currentState = getStateFromSimulation(avg_en, avg_loss, Objects.hash(currentPowerAdd), Objects.hash(currentPowerSub), Objects.hash(currentDistribution), neigh);

            // L'agente seleziona la migliore azione basata sullo stato attuale
            int actionId = trainedAgent.selectAction(currentState).getIndex();
            System.out.println("Agent selects action-" + actionId);

            // Decodifica l'azione per determinare le modifiche alla potenza e distribuzione
            int[] powerAdd = decodePowerAdd(actionId);
            int[] powerSub = decodePowerSub(actionId);
            int[] distributionChange = decodeDistributionChange(actionId);

            // Aggiorna i valori di potenza e distribuzione correnti
            /*
            currentPowerAdd = Math.max(0, powerAdd);
            currentPowerSub = Math.max(0, powerSub);
            currentDistribution = Math.max(0, distributionChange);

             */


            // Limita la potenza e la distribuzione ai valori massimi consentiti
            /*
            currentPowerAdd = Math.min(currentPowerAdd, 10);
            currentPowerSub = Math.min(currentPowerSub, 10);
            currentDistribution = Math.min(currentDistribution, 50);

             */

            int[][] ret =  {powerAdd, powerSub,distributionChange};
            return ret;
        } else {
            System.out.println("Failed to load the agent.");

            return null;
        }
    }

    public static QLearner loadAgentFromJson(String jsonFilePath) {
        QLearner agent = new QLearner();
        try {
            // Carica l'agente dal file JSON
            String json = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            agent = QLearner.fromJson(json);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return agent;
    }

    // Metodi di supporto:










    public static double getAverageEnergy(List<QoS> qosList) {
        return qosList.stream()
                .mapToDouble(QoS::getEnergyConsumption)
                .average()
                .orElse(0.0); // Restituisce 0.0 se la lista è vuota
    }

    public static double getAveragePacketLoss(List<QoS> qosList) {
        return qosList.stream()
                .mapToDouble(QoS::getPacketLoss)
                .average()
                .orElse(0.0); // Restituisce 0.0 se la lista è vuota
    }
}

