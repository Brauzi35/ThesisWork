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



    public int[] interrogation(String pathJson, int[] neigh, Simulator actualNetwork, int currentPowerAdd, int currentPowerSub, int currentDistribution){ //TODO passare anche il tipo di anomalia
        // load scpecific agent (.json)
        QLearner trainedAgent = loadAgentFromJson(pathJson);


        if (trainedAgent != null) {
            System.out.println("Agent successfully loaded from JSON.");


            List<QoS> currentSituation = actualNetwork.getQosValues();
            double avg_en = getAverageEnergy(currentSituation);
            double avg_loss = getAveragePacketLoss(currentSituation);

            // get current state
            int currentState = getStateFromSimulation(avg_en, avg_loss, currentPowerAdd, currentPowerSub, currentDistribution, neigh);

            // get best action
            int actionId = trainedAgent.selectAction(currentState).getIndex();
            System.out.println("Agent selects action-" + actionId);

            // get policy from best action
            int powerAdd = decodePowerAdd(actionId, 16);
            int powerSub = decodePowerSub(actionId, 16);
            int distributionChange = decodeDistributionChange(actionId, 16);




            int[] ret =  {powerAdd, powerSub,distributionChange};
            return ret;
        } else {
            System.out.println("Failed to load the agent.");

            return null;
        }
    }

    public static QLearner loadAgentFromJson(String jsonFilePath) {
        QLearner agent = new QLearner();
        try {
            String json = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            agent = QLearner.fromJson(json);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return agent;
    }

    // supportMethods:



    public static double getAverageEnergy(List<QoS> qosList) {
        return qosList.stream()
                .mapToDouble(QoS::getEnergyConsumption)
                .average()
                .orElse(0.0); // return 0.0 if empty list
    }

    public static double getAveragePacketLoss(List<QoS> qosList) {
        return qosList.stream()
                .mapToDouble(QoS::getPacketLoss)
                .average()
                .orElse(0.0); // return 0.0 if empty list
    }
}

