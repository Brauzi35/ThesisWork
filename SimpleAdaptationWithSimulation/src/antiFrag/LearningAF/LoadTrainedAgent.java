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

import static antiFrag.Position.FindPositionAndNeighbour.findClosestNode;
import static antiFrag.Position.FindPositionAndNeighbour.getPosition;

public class LoadTrainedAgent {
    static Random random = new Random();

    public static void main(String[] args) {
        //interrogation("JsonRL/CASE1.json");
    }

    public int[] interrogation(String pathJson, int neigh, Simulator actualNetwork){ //TODO passare anche il tipo di anomalia
        // Carica l'agente dal file JSON salvato durante il training
        QLearner trainedAgent = loadAgentFromJson(pathJson);

        // Controllo che l'agente sia stato caricato correttamente
        if (trainedAgent != null) {
            System.out.println("Agent successfully loaded from JSON.");


            // Definisci la potenza e la distribuzione iniziali
            int currentPowerAdd = 1;
            int currentPowerSub = 1;
            int currentDistribution = 1;

            List<QoS> currentSituation = actualNetwork.getQosValues();
            double avg_en = getAverageEnergy(currentSituation);
            double avg_loss = getAveragePacketLoss(currentSituation);

            // Ottieni lo stato corrente
            int currentState = getStateFromSimulation(avg_en, avg_loss, currentPowerAdd, currentPowerSub, 1, neigh);

            // L'agente seleziona la migliore azione basata sullo stato attuale
            int actionId = trainedAgent.selectAction(currentState).getIndex();
            System.out.println("Agent selects action-" + actionId);

            // Decodifica l'azione per determinare le modifiche alla potenza e distribuzione
            int powerAdd = decodePowerAdd(actionId);
            int powerSub = decodePowerSub(actionId);
            int distributionChange = decodeDistributionChange(actionId);

            // Aggiorna i valori di potenza e distribuzione correnti
            currentPowerAdd = Math.max(1, currentPowerAdd + powerAdd);
            currentPowerSub = Math.max(1, currentPowerSub + powerSub);
            currentDistribution = Math.max(1, currentDistribution + distributionChange);

            // Limita la potenza e la distribuzione ai valori massimi consentiti
            currentPowerAdd = Math.min(currentPowerAdd, 10);
            currentPowerSub = Math.min(currentPowerSub, 10);
            currentDistribution = Math.min(currentDistribution, 50);

            int[] ret =  {currentPowerAdd, currentPowerSub,currentDistribution};
            return ret;
        } else {
            System.out.println("Failed to load the agent.");
            int[] ret = null;
            return ret;
        }
    }

    private static QLearner loadAgentFromJson(String jsonFilePath) {
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
    private static int getStateFromSimulation(double energy, double loss, int powerAdd, int powerSub, int distChange, int neigh) {
        int energyState = (int) (energy / 10); // dividendo in intervalli di 10
        int lossState = (int) (loss * 10); // scala la perdita tra 0-1 a intervalli di 0.1
        int state = energyState * 100000 + lossState * 10000 + powerAdd * 1000 + powerSub * 100 + distChange * 10 + neigh; // combinazione unica di stato
        return state;
    }

    private static int decodePowerAdd(int actionId) {
        return (actionId % 5) + 1; // Valori da 1 a 5, garantiti positivi per aggiungere potenza
    }

    private static int decodePowerSub(int actionId) {
        return ((actionId / 5) % 5) + 1; // Valori da 1 a 5, garantiti positivi per sottrarre potenza
    }

    private static int decodeDistributionChange(int actionId) {
        return ((actionId / 25) % 5) + 1; // Valori da 1 a 5, garantiti positivi per cambiare distribuzione
    }

    private static void applyConfiguration(int effectivePower, int distribution) {
        // Implementa qui il metodo per applicare la configurazione della rete usando effectivePower e distribution
        System.out.println("Applying configuration: Effective Power = " + effectivePower + ", Distribution = " + distribution);
        // Potresti voler inviare questi valori al tuo sistema di gestione della rete
    }

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

