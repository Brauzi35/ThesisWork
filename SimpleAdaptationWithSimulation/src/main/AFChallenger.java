package main;

import antiFrag.LearningAF.Move;
import antiFrag.LearningAF.RL;
import antiFrag.SimulationClientAF;
import antiFrag.Utils.CsvWriter;
import com.github.chen0040.rl.learning.qlearn.QLearner;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.client.SimulationClient;
import mapek.FeedbackLoopChallenger;
import mapek.FeedbackLoopRL;
import simulator.QoS;

import java.awt.geom.Point2D;
import java.util.*;

import static antiFrag.LearningAF.RL.*;
import static antiFrag.Position.FindPositionAndNeighbour.findClosestNode;
import static antiFrag.Position.FindPositionAndNeighbour.getPosition;

public class AFChallenger {
    /**
     * we want to build a fair AF antagonist. We'll build a continuos learning agent to see how much time we can save.
     */


    static int discr_energyusageLevels = 10;
    static int discr_lossLevels = 10;
    static int discr_changePower = 5; // numero di livelli per `powerAdd` e `powerSub`
    static int discr_changeDistr = 5;
    static double epsilon = 1.0; // Probabilità di esplorazione (10%)
    static Random random = new Random();
    static private CsvWriter csvWriter = new CsvWriter();



    public static void start(){
        int stateCount = discr_energyusageLevels * discr_lossLevels*15;//discr_energyusageLevels * discr_lossLevels * discr_changePower * discr_changePower * discr_changeDistr;
        int actionCount = discr_changeDistr * discr_changePower * discr_changePower;

        QLearner agent = new QLearner(stateCount, actionCount);

        int currentState = random.nextInt(stateCount);
        List<Move> moves = new ArrayList<>();
        int trainingTime = 100;

        for (int time = 0; time < trainingTime; ++time) {
            System.out.println("progression: "+ time+"/"+trainingTime);
            Point2D point2D = getPosition();
            int neigh = findClosestNode(point2D);
            //System.out.println("neighbour is: " + neigh);
            //Random random2 = new Random();
            double delta = 0;//random2.nextInt(1001);
            SimulationClientAF sc = new SimulationClientAF(SimulationClientAF.Case.CASE1, (int) point2D.getX(), (int) point2D.getY(), 118800.0, 200, neigh, delta);

            // Esplorazione epsilon-greedy
            int actionId;
            if (random.nextDouble() < epsilon*(1/(time+1.0))) {
                actionId = random.nextInt(actionCount);
                epsilon = decreaseEpsilon(epsilon);
                //System.out.println("Agent explores with action-" + actionId);
            } else {
                actionId = agent.selectAction(currentState).getIndex();
                //System.out.println("Agent exploits with action-" + actionId);
            }

            // Decodifica dell'azione per ottenere i valori per powerAdd, powerSub e distributionChange
            int[] powerAdd = RL.decodePowerAdd(actionId);   // L'agente sceglie direttamente powerAdd
            int[] powerSub = RL.decodePowerSub(actionId);   // L'agente sceglie direttamente powerSub
            int[] distributionChange = RL.decodeDistributionChange(actionId);  // L'agente sceglie direttamente distributionChange

            System.out.println(powerAdd[0]);
            System.out.println(powerSub[0]);
            System.out.println(distributionChange[0]);
            // Assegna direttamente i valori decisi dall'agente
            int[] currentPowerAdd = powerAdd;
            int[] currentPowerSub = powerSub;
            int[] currentDistribution = distributionChange;

            // Crea l'oggetto di simulazione
            SimulationClient networkMgmt = new SimulationClient(sc.getSimulator());

            // Feedback loop e connessione a probe/effector
            FeedbackLoopChallenger feedbackLoop = new FeedbackLoopChallenger();
            Probe probe = networkMgmt.getProbe();
            Effector effector = networkMgmt.getEffector();
            feedbackLoop.setProbe(probe);
            feedbackLoop.setEffector(effector);
            feedbackLoop.setNetwork(networkMgmt);

            networkMgmt = feedbackLoop.start(currentPowerAdd, currentPowerSub, currentDistribution, time);
            int stopIdx = feedbackLoop.getRecoveredTimestamp();

            ArrayList<QoS> result = networkMgmt.getNetworkQoS(96);

            //RECOVERY
            if(stopIdx<0){
                stopIdx = 95;
            }
            Probe probe2 = networkMgmt.getProbe();
            Effector effector2 = networkMgmt.getEffector();

            feedbackLoop.setProbe(probe2);
            feedbackLoop.setEffector(effector2);
            feedbackLoop.setNetwork(networkMgmt);

            SimulationClient fin = feedbackLoop.start(currentPowerAdd, currentPowerSub, currentDistribution,time);

            ArrayList<QoS> resultRecovery = fin.getNetworkQoS(96-stopIdx-1);

            ArrayList<QoS> finalResult = new ArrayList<>();
            for(int k = 0; k<=stopIdx; k++){
                finalResult.add(result.get(k));
            }
            finalResult.addAll(resultRecovery);
            // Calcola la ricompensa in base alla nuova funzione
            //double reward = calculateReward(result);
            double reward = globalReward(finalResult, true);

            // Determina il nuovo stato basato sui risultati della simulazione
            double averageEnergy = finalResult.stream().mapToDouble(QoS::getEnergyConsumption).average().orElse(0.0);
            double averageLoss = finalResult.stream().mapToDouble(QoS::getPacketLoss).average().orElse(0.0);

            int newStateId = getStateFromSimulation(averageEnergy, averageLoss, Objects.hash(powerAdd), Objects.hash(powerSub), Objects.hash(distributionChange), neigh);

            // Aggiorna la lista di mosse e la Q-Table dell'agente
            moves.add(new Move(currentState, actionId, newStateId, reward));
            agent.update(currentState, actionId, newStateId, reward);

            currentState = newStateId;  // Aggiorna lo stato corrente

            //System.out.println("current powerAdd = " + currentPowerAdd + ", current powerSub = " + currentPowerSub + " , current distribution = " + currentDistribution + ", reward = " + reward);

            csvWriter.writeQoSToCSV(finalResult, "ChallengerProgression/simulation"+time+"_neigh"+neigh+ "_stopAnomaly"+ stopIdx +".csv");

        }
    }

    public static void main(String[] args) {
        start();
    }

}
