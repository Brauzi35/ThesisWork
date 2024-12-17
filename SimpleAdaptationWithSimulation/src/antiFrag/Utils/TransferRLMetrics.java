package antiFrag.Utils;

import antiFrag.AnomalyBuilder.AnomalyGeneralizer;
import antiFrag.AnomalyBuilder.BaseNetwork;
import antiFrag.LearningAF.Move;
import antiFrag.LearningAF.RL;
import antiFrag.SimulationClientAF;
import com.github.chen0040.rl.learning.qlearn.QLearner;
import com.github.chen0040.rl.models.QModel;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.client.SimulationClient;
import mapek.FeedbackLoopRL;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import simulator.QoS;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static antiFrag.LearningAF.LoadTrainedAgent.loadAgentFromJson;
import static antiFrag.LearningAF.RL.*;
import static antiFrag.NetworkSimulatorAF.createSimulatorCase1noProbs;
import static antiFrag.Position.FindPositionAndNeighbour.findClosestNode;
import static antiFrag.Position.FindPositionAndNeighbour.getPosition;
import static antiFrag.TwinInterrogation.buildCloneNetwork;


public class TransferRLMetrics {

    private static double rewardtl = 0.0;
    private static int iterationrl = 0;

    private static double rewardfs = 0.0;



    private static ArrayList<Integer> states = new ArrayList<>();
    static int[] current_neigh = {0,0};
    static Random random = new Random();

    static ArrayList<Point2D> neigh1 = new ArrayList<>();
    static ArrayList<Point2D> neigh2 = new ArrayList<>();
    static final int differentnetworks = 100;
    static final int trainingTime = 100;

    static final int load = 125;
    static final double max_epsilon = 0.75;

    static ArrayList<Integer> seeds = new ArrayList<>();
    static ArrayList<SimulationClientAF> simulators = new ArrayList<>();
    static SimulationClientAF.Case ca = SimulationClientAF.Case.CASE1;
    static SimulationClientAF.Case un = SimulationClientAF.Case.CASE1;
/*
    private double rewardtl_first = 0;
    private int iterationrl_first = 0;

    private double rewardfs_first = 0;
    private int iterationfs_first = 0;

 */

    private static double epsilon = max_epsilon;

    /**
     * we use this class to calculate some TL metrics, in particular:
     *
     *  1. Jumpstart: The initial performance of an agent in a target task may be improved by transfer
     *  from a source task.
     *  2. Asymptotic Performance: The final learned performance of an agent in the target task may be
     *  improved via transfer.
     *  3. Total Reward: The total reward accumulated by an agent (i.e., the area under the learning
     *  curve) may be improved if it uses transfer, compared to learning without transfer.
     *  4. Transfer Ratio: The ratio of the total reward accumulated by the transfer learner and the total
     *  reward accumulated by the non-transfer learner.
     */
    public static SimulationClientAF createTrainingNetwork(boolean forceSimulationClient, int forcedneigh, SimulationClientAF.Case c){
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
        SimulationClientAF sc = new SimulationClientAF(c, x, y, 118800.0, load, neigh_arr, delta);
        //SimulationClientAF sc = new SimulationClientAF(c, x, y, 118800.0, 100, neigh_arr, delta);
        return sc;
    }
    public static void main(String[] args) {
        QLearner tlAgent = loadAgentFromJson("JsonRL/CASE1.json");
        String policy =tlAgent.getActionSelection();
        System.out.println(policy);
        policy = policy.substring(11);
        policy = "epsilon=0.1"+policy;
        System.out.println(policy);



        Random r = new Random(42);
        for (int i = 0; i<differentnetworks; i++){
            Point2D point2D = getPosition();
            neigh1.add(point2D);
            point2D = getPosition();
            neigh2.add(point2D);
            seeds.add(r.nextInt(97));
        }

        //tlAgent.setActionSelection(policy);
        QLearner voidAgent = new QLearner(stateCount, actionCount);

        SaveJsonToFile saveJsonToFile = new SaveJsonToFile();
        saveJsonToFile.save(tlAgent.toJson(), "TransferLearningMetrics/transferAgent.json");
        saveJsonToFile.save(voidAgent.toJson(), "TransferLearningMetrics/voidAgent.json");

        QLearner newAgent = loadAgentFromJson("TransferLearningMetrics/voidAgent.json");
        //System.out.println(newAgent.toJson());

        ArrayList<QLearner> learners = new ArrayList<>();
        learners.add(tlAgent);
        learners.add(newAgent);

        //dummyTraining(tlAgent, true);
        //dummyTraining(newAgent, false);
        double sumTL = 0;
        double sumNew = 0;
        ArrayList<Double> collector =  new ArrayList<>();

        ArrayList<Double> arrTL = new ArrayList<>();
        ArrayList<Double> arrNew = new ArrayList<>();


        //NetworksGenerator first = new NetworksGenerator(neigh1, neigh2);

        //simulators = first.networksGenerator(differentnetworks, load, ca);
        int maxIt = 20;
        for(int i = 0; i<maxIt; i++){

            NetworksGenerator generator = new NetworksGenerator(neigh1, neigh2);
            simulators = generator.networksGenerator(differentnetworks, load, un);
            for(int j = 0; j< differentnetworks; j++){

                //int currentState = getStateFromSimulation(averageEnergy, averageLoss, powerAdd, powerSub, distributionChange, neigh_arr);
                //int actionId = tlAgent.selectAction(currentState).getIndex();
                tlAgent.setActionSelection(policy);
                //double reward = executeInterrogation(tlAgent, new SimulationClientAF(buildCloneNetwork(new SimulationClient(simulators.get(j).getSimulator()))), j); //we're evaluating agent performances
                double reward = executeInterrogation(tlAgent, new SimulationClientAF(buildCloneNetwork(new SimulationClient(simulators.get(j).getSimulator()))), j); //we're evaluating agent performances

                sumTL+=reward;
                collector.add(reward);
            }
            simulators.clear();

            Collections.sort(collector);
            System.out.println("transfer " + collector);
            arrTL.add(sumTL); //add avg
            //arrTL.add(collector.get(50));
            tlAgent = dummyTraining(tlAgent, true);

            sumTL = 0;
            collector.clear();
            epsilon = max_epsilon;
        }
        seeds.clear();
        r = new Random(42);
        for(int i=0; i<100; i++){
            seeds.add(r.nextInt(97));
        }
        //new agent
        for(int i = 0; i<maxIt; i++){
            NetworksGenerator generator = new NetworksGenerator(neigh1, neigh2);
            simulators = generator.networksGenerator(differentnetworks, load, un);
            for(int j = 0; j< differentnetworks; j++){

                //int currentState = getStateFromSimulation(averageEnergy, averageLoss, powerAdd, powerSub, distributionChange, neigh_arr);
                //int actionId = tlAgent.selectAction(currentState).getIndex();
                double reward = executeInterrogation(newAgent, new SimulationClientAF(buildCloneNetwork(new SimulationClient(simulators.get(j).getSimulator()))), j); //we're evaluating agent performances
                sumNew+=reward;
                collector.add(reward);
            }
            simulators.clear();


            Collections.sort(collector);
            System.out.println("scratch " + collector);
            arrNew.add(sumNew); //add avg
            //arrNew.add(collector.get(50));
            newAgent = dummyTraining(newAgent, false);

            System.out.println(newAgent.toJson());
            sumNew = 0;
            collector.clear();
            epsilon = max_epsilon;
        }

        checkBlocks(states);



        try {
            createAndSaveChart(arrTL, arrNew, "TransferLearningMetrics", "metriche transfer learning");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void checkBlocks(ArrayList<Integer> states) {
        int blockSize = differentnetworks; // Dimensione dei blocchi
        int totalBlocks = states.size() / blockSize; // Numero di blocchi
        Set<Integer> unequalIndices = new HashSet<>(); // Indici dei valori non uguali

        for (int block = 0; block < totalBlocks - 1; block++) {
            // Confronta il blocco corrente con il successivo
            for (int i = 0; i < blockSize; i++) {
                int currentValue = states.get(block * blockSize + i);
                int nextValue = states.get((block + 1) * blockSize + i);

                if (currentValue != nextValue) {
                    unequalIndices.add(block * blockSize + i); // Indice nel blocco corrente
                    unequalIndices.add((block + 1) * blockSize + i); // Indice nel blocco successivo
                }
            }
        }

        // Stampa i risultati
        if (unequalIndices.isEmpty()) {
            System.out.println("All blocks are equal.");
        } else {
            System.out.println("Unequal values found at indices:");
            for (int index : unequalIndices) {
                System.out.println("Index: " + index + ", Value: " + states.get(index));
            }
        }
    }




    public static double executeInterrogation(QLearner agent, SimulationClientAF sc, int i){
        sc.getAllMotes();


        String policy =agent.getActionSelection();

        policy = policy.substring(11);
        policy = "epsilon=0.0"+policy;
        agent.setActionSelection(policy);

        //System.out.println("i:"+i+" " + sc.getSimulator().getMotes());
        //System.out.println("debug problemi inizio: " + sc.getSimulator().getRunInfo().getRunNumber());

        List<QoS> partialqos = sc.getNetworkQoS(1);
        System.out.println("loss " + partialqos.get(0).getPacketLoss()+ " energ" + partialqos.get(0).getEnergyConsumption());
        int currentState = getStateFromSimulation(new double[]{partialqos.get(0).getPacketLoss(), partialqos.get(0).getEnergyConsumption()},new int[]{0,0});
        System.out.println("i:"+i+" state " + currentState);

        int actionId = agent.selectAction(currentState).getIndex();
        QModel model = agent.getModel();
        states.add(currentState);

        if(model.getQ(currentState, actionId)==0.1){//state not discovered yet in training
            policy = policy.substring(11);
            policy = "epsilon=0.1"+policy;
            agent.setActionSelection(policy);
            return 3000.0;

        }


        System.out.println("state: " + currentState + " action: "+ actionId + " model " + model.getQ(currentState, actionId));

        // decode action to obtain powerAdd, powerSub e distributionChange
        int powerAdd = RL.decodePowerAdd(actionId, 15);
        int powerSub = RL.decodePowerSub(actionId, 15);
        int distributionChange = RL.decodeDistributionChange(actionId, 15);

        int currentPowerAdd = powerAdd;
        int currentPowerSub = powerSub;
        int currentDistribution = distributionChange;

        // simulation setup
        SimulationClient networkMgmt = new SimulationClient(sc.getSimulator());

        FeedbackLoopRL feedbackLoop = new FeedbackLoopRL();
        Probe probe = networkMgmt.getProbe();
        Effector effector = networkMgmt.getEffector();
        feedbackLoop.setProbe(probe);
        feedbackLoop.setEffector(effector);
        feedbackLoop.setNetwork(networkMgmt);

        feedbackLoop.start(currentPowerAdd, currentPowerSub, currentDistribution);

        ArrayList<QoS> result = networkMgmt.getNetworkQoS(96);
        //System.out.println(sc.getSimulator().getMotes());
        //System.out.println("debug problemi fine: " + sc.getSimulator().getRunInfo().getRunNumber());
        policy = policy.substring(11);
        policy = "epsilon=0.1"+policy;
        agent.setActionSelection(policy);
        return globalReward(result, false);
    }



    private static QLearner dummyTraining(QLearner agent, boolean isTransfer){
        int currentState = random.nextInt(stateCount);
        List<Move> moves = new ArrayList<>();
        if(isTransfer){
            System.out.println("beginning transfer learning training");
        }else {
            System.out.println("beginning learning from scratch training");
        }

        for(iterationrl = 0; iterationrl< trainingTime; iterationrl++){
            //SimulationClientAF sc = createTrainingNetwork(false, 0, SimulationClientAF.Case.UNKNOWN);
            SimulationClientAF sc = createTrainingNetwork(false, 0, un);

            int actionId;

            sc.getAllMotes();
            List<QoS> partialqos = sc.getNetworkQoS(1);
            currentState = getStateFromSimulation(new double[]{partialqos.get(0).getPacketLoss(), partialqos.get(0).getEnergyConsumption()},current_neigh);


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
            int powerAdd = decodePowerAdd(actionId, 16);
            int powerSub = decodePowerSub(actionId, 16);
            int distributionChange = decodeDistributionChange(actionId, 16);


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



            double reward = globalReward(result, false);
            /*
            if(isTransfer){
                rewardtl += reward;
                rewardsTL.add(rewardtl);
            }else{
                rewardfs += reward;
                rewardsFS.add(rewardfs);
            }

             */
            // Find new state
            double averageEnergy = result.stream().mapToDouble(QoS::getEnergyConsumption).average().orElse(0.0);
            double averageLoss = result.stream().mapToDouble(QoS::getPacketLoss).average().orElse(0.0);


            //int newStateId = getStateFromSimulation(averageEnergy, averageLoss, powerAdd, powerSub, distributionChange, current_neigh);
            int newStateId = currentState;
            // Update moves list and agent Q-Table
            moves.add(new Move(currentState, actionId, newStateId, reward));
            agent.update(currentState, actionId, newStateId, reward);




            /*System.out.println("current powerAdd = " + currentPowerAdd +
                    ", current powerSub = " + currentPowerSub +
                    ", current distribution = " + currentDistribution);

             */
            System.out.println("reward = " + reward + " progression: " + iterationrl + "/"+ trainingTime);


        }
        ArrayList<Point2D> n1 =  new ArrayList<>(), n2 = new ArrayList<>();
        int limiter = 1;
        for (int i = 0; i<limiter; i++){
            int offset = seeds.get(0);
            seeds.remove(0);
            n1.add(neigh1.get(i+offset));
            n2.add(neigh2.get(i+offset));
        }
        NetworksGenerator networksGeneratorTraining = new NetworksGenerator(n1, n2);
        ArrayList<SimulationClientAF> simsTr = networksGeneratorTraining.networksGenerator(limiter, load, un);
        for(int j = 0; j<limiter; j++){
            int actionId;
            SimulationClientAF sc = simsTr.get(j);
            sc.getAllMotes();
            List<QoS> partialqos = sc.getNetworkQoS(1);
            currentState = getStateFromSimulation(new double[]{partialqos.get(0).getPacketLoss(), partialqos.get(0).getEnergyConsumption()},current_neigh);


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
            int powerAdd = decodePowerAdd(actionId, 16);
            int powerSub = decodePowerSub(actionId, 16);
            int distributionChange = decodeDistributionChange(actionId, 16);


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



            double reward = globalReward(result, false);

            // Find new state
            double averageEnergy = result.stream().mapToDouble(QoS::getEnergyConsumption).average().orElse(0.0);
            double averageLoss = result.stream().mapToDouble(QoS::getPacketLoss).average().orElse(0.0);


            //int newStateId = getStateFromSimulation(averageEnergy, averageLoss, powerAdd, powerSub, distributionChange, current_neigh);
            int newStateId = currentState;
            // Update moves list and agent Q-Table
            moves.add(new Move(currentState, actionId, newStateId, reward));
            agent.update(currentState, actionId, newStateId, reward);
        }
        return agent;
    }


    private static QLearner preciseTraining(QLearner agent, boolean isTransfer, SimulationClientAF sc){
        int currentState = random.nextInt(stateCount);
        List<Move> moves = new ArrayList<>();
        if(isTransfer){
            System.out.println("beginning transfer learning training");
        }else {
            System.out.println("beginning learning from scratch training");
        }

        for(iterationrl = 0; iterationrl< 1; iterationrl++){
            //SimulationClientAF sc = createTrainingNetwork(false, 0, SimulationClientAF.Case.UNKNOWN);

            int actionId;

            sc.getAllMotes();
            List<QoS> partialqos = sc.getNetworkQoS(1);
            currentState = getStateFromSimulation(new double[]{partialqos.get(0).getPacketLoss(), partialqos.get(0).getEnergyConsumption()},current_neigh);


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
            int powerAdd = decodePowerAdd(actionId, 16);
            int powerSub = decodePowerSub(actionId, 16);
            int distributionChange = decodeDistributionChange(actionId, 16);


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



            double reward = globalReward(result, false) + 1000;
            /*
            if(isTransfer){
                rewardtl += reward;
                rewardsTL.add(rewardtl);
            }else{
                rewardfs += reward;
                rewardsFS.add(rewardfs);
            }

             */
            // Find new state
            double averageEnergy = result.stream().mapToDouble(QoS::getEnergyConsumption).average().orElse(0.0);
            double averageLoss = result.stream().mapToDouble(QoS::getPacketLoss).average().orElse(0.0);


            //int newStateId = getStateFromSimulation(averageEnergy, averageLoss, powerAdd, powerSub, distributionChange, current_neigh);
            int newStateId = currentState;
            // Update moves list and agent Q-Table
            moves.add(new Move(currentState, actionId, 0, reward));
            agent.update(currentState, actionId, 0, reward);




            /*System.out.println("current powerAdd = " + currentPowerAdd +
                    ", current powerSub = " + currentPowerSub +
                    ", current distribution = " + currentDistribution);

             */
            System.out.println("reward = " + reward + " progression: " + iterationrl + "/"+ trainingTime);


        }
        return agent;
    }

    public static void createAndSaveChart(ArrayList<Double> rewardsTL, ArrayList<Double> rewardsFS, String folderName, String chartTitle) throws IOException, IOException {
        // Create the XY series for Transfer Learning and No Transfer
        XYSeries seriesTL = new XYSeries("Transfer");
        XYSeries seriesFS = new XYSeries("No Transfer");

        for (int i = 0; i < rewardsTL.size(); i++) {
            seriesTL.add(i, rewardsTL.get(i));
        }
        for (int i = 0; i < rewardsFS.size(); i++) {
            seriesFS.add(i, rewardsFS.get(i));
        }

        // Create the dataset
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(seriesTL);
        dataset.addSeries(seriesFS);

        // Create the chart
        JFreeChart chart = ChartFactory.createXYLineChart(
                chartTitle,
                "Training Time (sample complexity 1:"+trainingTime+")",
                "Performance",
                dataset
        );

        // Create the output folder if it does not exist
        File folder = new File(folderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Save the chart as a PNG file
        File outputFile = new File(folder, "PerformanceChart.png");
        ChartUtils.saveChartAsPNG(outputFile, chart, 800, 600);

        System.out.println("Chart saved to: " + outputFile.getAbsolutePath());
    }


}
