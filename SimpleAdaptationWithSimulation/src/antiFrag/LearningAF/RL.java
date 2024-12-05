package antiFrag.LearningAF;
import antiFrag.AnomalyBuilder.AnomalyGeneralizer;
import antiFrag.AnomalyDetection.AnomalyDetection;
import antiFrag.SimulationClientAF;
import antiFrag.Utils.SaveJsonToFile;
import com.github.chen0040.rl.learning.qlearn.QLearner;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.client.SimulationClient;
import domain.Mote;
import mapek.FeedbackLoopAFReactive;
import mapek.FeedbackLoopRL;
import simulator.QoS;
import simulator.Simulator;

import java.awt.geom.Point2D;
import java.util.*;

import static antiFrag.AnomalyBuilder.AnomalyBuilder.replicateAnomalyVariation;
import static antiFrag.LearningAF.LoadTrainedAgent.loadAgentFromJson;
import static antiFrag.Position.FindPositionAndNeighbour.findClosestNode;
import static antiFrag.Position.FindPositionAndNeighbour.getPosition;
import static antiFrag.TwinInterrogation.buildCloneNetwork;
import static com.github.chen0040.rl.learning.qlearn.QLearner.fromJson;


public class RL {
    static int discr_energyusageLevels = 10;
    static int discr_lossLevels = 10;
    static int discr_changePower = 5; // #levels for `powerAdd` e `powerSub`
    static int discr_changeDistr = 5;
    static double epsilon = 1.0;
    static Random random = new Random();

    static double avgReward = 0.0;

    static int[] current_neigh = {0,0};

    static int dimMotes = 15; //outdated
    static int actualDim = 15; //outdated
    static int stateCount = discr_energyusageLevels * discr_lossLevels * 15*2;//discr_energyusageLevels * discr_lossLevels * discr_changePower * discr_changePower * discr_changeDistr;
    static int stateCountRecovery = discr_energyusageLevels * discr_lossLevels * 15;//discr_energyusageLevels * discr_lossLevels * discr_changePower * discr_changePower * discr_changeDistr;

    static int actionCount = discr_changeDistr * discr_changePower * discr_changePower;

    static AnomalyDetection anomalyDetection = new AnomalyDetection();

    static private AnomalyGeneralizer anomalyGeneralizer;

    public RL() {
        anomalyGeneralizer = null;
    }

    public RL(AnomalyGeneralizer anomalyGeneralizer) {
        this.anomalyGeneralizer = anomalyGeneralizer;
    }

    public static double globalReward(ArrayList<QoS> results, boolean challenger) {

        return calculateLossReward(results);
        //return calculateRewardEnergyUsage(results);
        //return calculateRewardFairness(results);
        //return calculateRewardEnergyNodes(results, 0);
    }

    public static void main(String[] args) {


        QLearner agent = new QLearner(stateCount, actionCount);

        //startTraining(5000, agent, stateCount, actionCount, false, 0, "JsonRL/" + SimulationClientAF.Case.CASE1 + ".json", false, SimulationClientAF.Case.CASE1);
        anomalyDetection.init("AnomalyDetectionFiles");
        startTraining(1000, agent, stateCountRecovery, actionCount, false, 0, "JsonRL/" + "recoveryFromAnomaly" + ".json", true, SimulationClientAF.Case.CASE1);
        //agent = loadAgentFromJson("JsonRL/recoveryFromAnomaly.json");
        //startTraining(1000, agent, stateCount, actionCount, false, 0, "JsonRL/" + "recoveryFromAnomaly" + ".json", true, SimulationClientAF.Case.UNKNOWN);


        /*
        int[] originalMotes = {2,3,4,5,6,7,8,9,10,11,12,13,14,15};
        for(int i : originalMotes){
            System.out.println("specific training for neigh: " +i);
            boolean go = true;
            int count = 0;
            while(go){

                Point2D point2D = getPosition();
                int neigh = findClosestNode(point2D);
                while (neigh != i){
                    point2D = getPosition();
                    neigh = findClosestNode(point2D);
                }
                Random random2 = new Random();
                double delta = 0;//random2.nextInt(1001);
                SimulationClientAF sc = new SimulationClientAF(SimulationClientAF.Case.CASE1, (int) point2D.getX(), (int) point2D.getY(), 118800.0, 200, neigh, delta);
                if(evaluate(neigh, sc)) {
                    moreSpecificTraining(neigh, sc);

                }else {
                    go = false;
                }
                count++;
                if (count == 5){
                    go = false;
                }
            }



        }

         */


    }
    public void transferLearning(String similarAgent, String pathNewLearner){
        QLearner agent = loadAgentFromJson(similarAgent);
        System.out.println("similarAgent " + similarAgent);
        System.out.println("pathNewLearner " + pathNewLearner);
        startTraining(1000, agent, stateCount, actionCount, false, 0, pathNewLearner, false, SimulationClientAF.Case.UNKNOWN);
    }

    public void fromScratchLearning(String pathNewLearner){
        QLearner agent = new QLearner(stateCount, actionCount);
        startTraining(5000, agent, stateCount, actionCount, false, 0, pathNewLearner, false, SimulationClientAF.Case.UNKNOWN);

    }

    private static SimulationClientAF createTrainingNetwork(boolean forceSimulationClient, int forcedneigh, SimulationClientAF.Case c){
        Point2D point2D = getPosition();
        int neigh = findClosestNode(point2D);

        if(forceSimulationClient){
            while (neigh != forcedneigh){
                point2D = getPosition();
                neigh = findClosestNode(point2D);
            }
        }

        int[] neigh_arr = {neigh, 0};
        current_neigh = neigh_arr;

        int[] x = {(int) point2D.getX(),0};
        int[] y = {(int) point2D.getY(),0};

        point2D = getPosition();
        neigh = findClosestNode(point2D);
        if(forceSimulationClient){
            while (neigh != forcedneigh){
                point2D = getPosition();
                neigh = findClosestNode(point2D);
            }
        }
        neigh_arr[1] = neigh;
        x[1] = (int) point2D.getX();
        y[1] = (int) point2D.getY();

        current_neigh = neigh_arr;
        Random random2 = new Random();
        double delta = 0;//random2.nextInt(1001);
        SimulationClientAF sc = new SimulationClientAF(c, x, y, 118800.0, 200, neigh_arr, delta);
        return sc;
    }

    public static SimulationClientAF createTrainingNetworkNew(int seed){
        Simulator simulator = replicateAnomalyVariation(seed, anomalyGeneralizer);
        return new SimulationClientAF(simulator);
    }


    static public SimulationClientAF createRecoveryClient(int i, SimulationClientAF.Case c){
        Point2D point2D = getPosition();
        int neigh = findClosestNode(point2D);

        int [] neigh_arr = {neigh, 0};
        current_neigh = neigh_arr;
        System.out.println("neighbour is: " + neigh);
        Random random2 = new Random();
        double delta = random2.nextInt(1001);
        int[] x = {(int) point2D.getX(),0};
        int[] y = {(int) point2D.getY(),0};
        point2D = getPosition();
        neigh = findClosestNode(point2D);
        neigh_arr[1] = neigh;
        x[1] = (int) point2D.getX();
        y[1] = (int) point2D.getY();

        SimulationClientAF sc = new SimulationClientAF(c, x, y, 118800.0, 200, neigh_arr, delta);
        // first we make the feedback loop run so that it will remove the anomaly

        FeedbackLoopAFReactive feedbackLoop = new FeedbackLoopAFReactive(anomalyDetection);

        // get probe and effectors
        Probe probe = sc.getProbe();
        Effector effector = sc.getEffector();

        // Connect probe and effectors with feedback loop
        feedbackLoop.setProbe(probe);
        feedbackLoop.setEffector(effector);
        feedbackLoop.setNetwork(new SimulationClient(sc.getSimulator()));

        // now we can do training on the anomaly free (= no recovery) client
        SimulationClient temp = feedbackLoop.start(i);
        sc = new SimulationClientAF(temp.getSimulator());

        return sc;
    }
    public static void startTraining(int maxTime, QLearner agent, int stateCount, int actionCount, boolean forceSimulationClient, int forcedneigh, String filename, boolean recovery, SimulationClientAF.Case c) {


        int currentState = random.nextInt(stateCount);
        List<Move> moves = new ArrayList<>();

        for (int time = 0; time < maxTime; ++time) {
            SimulationClientAF sc = null;
            if(!recovery){
                if(anomalyGeneralizer == null) {
                    sc = createTrainingNetwork(forceSimulationClient, forcedneigh, c);
                }else {

                    sc = createTrainingNetworkNew(time);
                }

            }else{
                sc = createRecoveryClient(time, c);
            }

            // Esplorazione epsilon-greedy
            int actionId;
            if (random.nextDouble() < epsilon) {
                actionId = random.nextInt(actionCount);
                System.out.println("Agent explores with action-" + actionId);
                epsilon = decreaseEpsilon(epsilon);
            } else {
                actionId = agent.selectAction(currentState).getIndex();
                System.out.println("Agent exploits with action-" + actionId);
            }
            //System.out.println("dim motes "+ sc.getSimulator().getMotes());

            // get best policy from action
            int powerAdd = decodePowerAdd(actionId, actualDim);
            int powerSub = decodePowerSub(actionId, actualDim);
            int distributionChange = decodeDistributionChange(actionId, actualDim);

            // Assegna direttamente i valori decisi dall'agente
            int currentPowerAdd = powerAdd;
            int currentPowerSub = powerSub;
            int currentDistribution = distributionChange;

            // Create simulation object
            SimulationClient networkMgmt = new SimulationClient(sc.getSimulator());

            // Feedback loop and probe/effector connection
            FeedbackLoopRL feedbackLoop = new FeedbackLoopRL();
            Probe probe = networkMgmt.getProbe();
            Effector effector = networkMgmt.getEffector();
            feedbackLoop.setProbe(probe);
            feedbackLoop.setEffector(effector);
            feedbackLoop.setNetwork(networkMgmt);

            feedbackLoop.start(currentPowerAdd, currentPowerSub, currentDistribution);

            ArrayList<QoS> result = networkMgmt.getNetworkQoS(96);

            //System.out.println("Run, PacketLoss, EnergyConsumption");
            //result.forEach(qos -> System.out.println(qos));



            double reward = 0;
            if(!recovery){
                reward = globalReward(result, false);
            }else{
                reward = recoveryReward(networkMgmt);
            }
            if(!forceSimulationClient) {
                avgReward += reward;
            }

            // Find new state
            double averageEnergy = result.stream().mapToDouble(QoS::getEnergyConsumption).average().orElse(0.0);
            double averageLoss = result.stream().mapToDouble(QoS::getPacketLoss).average().orElse(0.0);


            int newStateId = getStateFromSimulation(averageEnergy, averageLoss, powerAdd, powerSub, distributionChange, current_neigh);

            // Update moves list and agent Q-Table
            moves.add(new Move(currentState, actionId, newStateId, reward));
            agent.update(currentState, actionId, newStateId, reward);


            currentState = newStateId;  // update current state

            System.out.println("current powerAdd = " + currentPowerAdd +
                    ", current powerSub = " + currentPowerSub +
                    ", current distribution = " + currentDistribution);
            System.out.println("reward = " + reward);
        }
        if(!forceSimulationClient){
            avgReward = avgReward/maxTime;
        }


        SaveJsonToFile saveJsonToFile = new SaveJsonToFile();
        saveJsonToFile.save(agent.toJson(), filename);
    }



    public static double decreaseEpsilon(double epsilon){
        double ret = 0.0;

        ret = epsilon * 0.95; // -5%

        ret = Math.max(ret, 0.1);

        return ret;
    }

    // Support methods:


    public static int getStateFromSimulation(double energy, double loss, int powerAdd, int powerSub, int distChange, int[] neigh) {
        int energyState = (int) (energy / 10); // 10 intervals - 10 in 10
        int lossState = (int) (loss * 10); // 0-1, 0.1 intervals

        // hashing all parameters
        int state = Objects.hash(energyState, lossState, Arrays.hashCode(neigh));
        return state;
    }

    public void setDimMotes(int dimMotes1){
        dimMotes = dimMotes1;
    }


    public static int decodePowerAdd(int actionId, int actualDim) {
        dimMotes = actualDim;
        int result = Math.abs((actionId) % 6);

        return result;
    }

    public static int decodePowerSub(int actionId, int actualDim) {
        dimMotes = actualDim;
        int result = Math.abs(((actionId) / 6) % 6);

        return result;
    }

    public static int decodeDistributionChange(int actionId, int actualDim) {
        dimMotes = actualDim;
        int result = Math.abs((((actionId) / 36) % 6) * 10);

        return result;
    }


    public static double recoveryReward(SimulationClient networkMgmt){
        double reward = 0;

        //no more anomaly, we want to restore classical behaviour as soon as possible

        int timestamp = networkMgmt.getSimulator().getRunInfo().getRunNumber();
        double[] point = new double[9]; // Punto con 9 dimensioni
        ArrayList<QoS> qos = networkMgmt.getNetworkQoS(timestamp);

        // Building point for new data
        point[0] = qos.get(timestamp - 1).getPacketLoss();
        point[1] = qos.get(timestamp - 1).getEnergyConsumption();
        point[2] = qos.get(timestamp - 1).getNumNodesEnergy();
        point[3] = qos.get(timestamp - 1).getNumNodesLoss();
        point[4] = qos.get(timestamp - 1).getFairnessIndex();

        // aggregates
        double totBattery = 0.0;
        double averagePower = 0.0;
        double totalDistribution = 0.0;
        int linkCount = 0;

        List<domain.Mote> motes = networkMgmt.getSimulator().getMotes();
        for (domain.Mote m : motes) {
            totBattery += m.getBatteryRemaining();
            List<domain.Link> links = m.getLinks();
            for (domain.Link l : links) {
                averagePower += l.getPowerNumber();
                totalDistribution += l.getDistribution();
                linkCount++;
            }
        }

        // remaining data
        point[5] = totBattery / motes.size();       // avg battery
        point[6] = linkCount > 0 ? averagePower / linkCount : 0.0; // avg power
        point[7] = linkCount > 0 ? totalDistribution / linkCount : 0.0; // avg distribution
        point[8] = motes.size();                    //#motes

        /*
        String out ="";
        for(int i=0; i<9; i++){
            out += point[i];
            out += " || ";
        }
        System.out.println("point: "+out);

         */
        timestamp = Math.min(94, timestamp); // timestamp limitation


        double distance = anomalyDetection.getDistance(timestamp, point);
        System.out.println("distance from usual = " + distance);
        reward += (1/distance)*100000;

        int counter = 0;
        double avgEngPostRecovery = 0.0;
        for(QoS q : qos){
            if(q.getEnergyConsumption()<100){
                avgEngPostRecovery += q.getEnergyConsumption();
                counter++;
            }
        }
        if(counter!=0){
            avgEngPostRecovery = avgEngPostRecovery/counter;
            reward+=(1/avgEngPostRecovery)*100;
        }

        return reward;
    }










    public static double calculateRewardFairness(ArrayList<QoS> results) {
        double reward = 0.0;
        int resultSize = results.size();
        if (resultSize < 2) return reward; // too little data to do any evaluation

        for(int i = 0; i <resultSize; i++){

            reward += 100*results.get(i).getFairnessIndex()*i;

        }


        return reward;

    }

    public static double calculateRewardEnergyUsage(ArrayList<QoS> results) {
        double reward = 0.0;
        int counter = 0;
        for (QoS result : results) {
            double currentEnergy = result.getEnergyConsumption();

            reward -=  currentEnergy;
            counter++;
            if(counter == 10){
                break;
            }
        }

        return reward;

    }

    public static double calculateLossReward(List<QoS> results) {
        double reward = 0.0;

        for (QoS result : results) {
            double currentLoss = result.getPacketLoss();

            reward += (1.0 - currentLoss)*100;
        }

        return reward;
    }


    public static double calculateRewardEnergyNodes(ArrayList<QoS> results, int stop) {
        double penalty = 0.0;
        double rewardBonus = 0.0;

        int resultSize = results.size();
        if (resultSize == 0) return 0.0;
        int initialNodesEnergy = results.get(0).getNumNodesEnergy();


        if (stop != 0) {
            resultSize = Math.min(resultSize, stop);
        }
        for (int i = 1; i < resultSize; i++) {
            int currentNodesEnergy = results.get(i).getNumNodesEnergy();

            if(currentNodesEnergy <= initialNodesEnergy){
                rewardBonus += 1000*(1/i)*(initialNodesEnergy-currentNodesEnergy);
            }

            if(currentNodesEnergy > initialNodesEnergy){
                penalty += 2000*(1/i);
            }
        }





        return rewardBonus - penalty;
    }







}
