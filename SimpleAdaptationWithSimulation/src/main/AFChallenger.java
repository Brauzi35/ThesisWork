package main;

import antiFrag.LearningAF.Move;
import antiFrag.LearningAF.RL;
import antiFrag.SimulationClientAF;
import antiFrag.Utils.CsvWriter;
import antiFrag.Utils.WealthScore;
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
import static antiFrag.Utils.CsvWriter.writeOrUpdateScoreToCSV;

public class AFChallenger {
    /**
     * we want to build a fair AF antagonist. We'll build a continuos learning agent to see how much time we can save.
     */


    static int discr_energyusageLevels = 10;
    static int discr_lossLevels = 10;
    static int discr_changePower = 5; // #levels `powerAdd` and `powerSub`
    static int discr_changeDistr = 5;
    static double epsilon = 1.0;
    static Random random = new Random();
    static Random random2 = new Random();
    static private CsvWriter csvWriter = new CsvWriter();



    public static void start(){
        int stateCount = discr_energyusageLevels * discr_lossLevels*15*2;//discr_energyusageLevels * discr_lossLevels * discr_changePower * discr_changePower * discr_changeDistr;
        int actionCount = discr_changeDistr * discr_changePower * discr_changePower;

        QLearner agent = new QLearner(stateCount, actionCount);

        int currentState = random.nextInt(stateCount);
        List<Move> moves = new ArrayList<>();
        int trainingTime = 100;

        for (int time = 0; time < trainingTime; ++time) {
            System.out.println("progression: "+ time+"/"+trainingTime);
            Point2D point2D = getPosition();
            int neigh = findClosestNode(point2D);
            double delta = 0;//random2.nextInt(1001);
            int [] neigh_arr = {neigh, 0};
            int[] x = {(int) point2D.getX(),0};
            int[] y = {(int) point2D.getY(),0};

            point2D = getPosition();
            neigh = findClosestNode(point2D);
            neigh_arr[1] = neigh;
            x[1] = (int) point2D.getX();
            y[1] = (int) point2D.getY();

            SimulationClientAF sc = new SimulationClientAF(SimulationClientAF.Case.CASE1, x, y, 118800.0, 200, neigh_arr, delta);
            //SimulationClientAF sc = new SimulationClientAF(SimulationClientAF.Case.UNKNOWN, x, y, 118800.0, 200, neigh_arr, delta);

            // epsilon-greedy
            int actionId;
            sc.getAllMotes();
            List<QoS> partialqos = sc.getNetworkQoS(1);
            currentState = getStateFromSimulation(new double[]{partialqos.get(0).getPacketLoss(), partialqos.get(0).getEnergyConsumption()},neigh_arr);
            if (random.nextDouble() < epsilon*(1/(time+1.0))) {
                actionId = random.nextInt(actionCount);
                epsilon = decreaseEpsilon(epsilon);
                //System.out.println("Agent explores with action-" + actionId);
            } else {
                actionId = agent.selectAction(currentState).getIndex();
                if(agent.getModel().getQ(currentState, actionId) == 0.1){
                    actionId = random2.nextInt(216); //inducing casuality
                }
                //System.out.println("Agent exploits with action-" + actionId);
            }

            // decode action to obtain powerAdd, powerSub e distributionChange
            int powerAdd = RL.decodePowerAdd(actionId, 15);
            int powerSub = RL.decodePowerSub(actionId, 15);
            int distributionChange = RL.decodeDistributionChange(actionId, 15);

            System.out.println(powerAdd);
            System.out.println(powerSub);
            System.out.println(distributionChange);

            int currentPowerAdd = powerAdd;
            int currentPowerSub = powerSub;
            int currentDistribution = distributionChange;

            // simulation setup
            SimulationClient networkMgmt = new SimulationClient(sc.getSimulator());

            // Feedback loop and probe/effector connection
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

            double reward = globalReward(finalResult, true);

            // get state
            double averageEnergy = finalResult.stream().mapToDouble(QoS::getEnergyConsumption).average().orElse(0.0);
            double averageLoss = finalResult.stream().mapToDouble(QoS::getPacketLoss).average().orElse(0.0);

            //int newStateId = getStateFromSimulation(averageEnergy, averageLoss, powerAdd, powerSub, distributionChange, neigh_arr);
            int newStateId = currentState;
            // update moves nd Q-Table
            moves.add(new Move(currentState, actionId, newStateId, reward));
            agent.update(currentState, actionId, newStateId, reward);

            //currentState = newStateId;  // updateState

            //System.out.println("current powerAdd = " + currentPowerAdd + ", current powerSub = " + currentPowerSub + " , current distribution = " + currentDistribution + ", reward = " + reward);

            csvWriter.writeQoSToCSV(finalResult, "ChallengerProgression/simulation"+time+"_neigh"+Arrays.toString(neigh_arr)+ "_stopAnomaly"+ stopIdx +".csv");
            WealthScore wealthScore = new WealthScore();
            double score = wealthScore.calculateScore(finalResult);
            writeOrUpdateScoreToCSV(time, score, "challenger.csv");
        }
    }

    public static void main(String[] args) {
        start();
    }

}
