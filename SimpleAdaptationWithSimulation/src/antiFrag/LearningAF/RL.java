package antiFrag.LearningAF;
import antiFrag.AnomalyDetection.AnomalyDetection;
import antiFrag.SimulationClientAF;
import antiFrag.Utils.SaveJsonToFile;
import com.github.chen0040.rl.learning.qlearn.QLearner;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.client.SimulationClient;
import mapek.FeedbackLoopAFReactive;
import mapek.FeedbackLoopRL;
import simulator.QoS;

import java.awt.geom.Point2D;
import java.util.*;

import static antiFrag.LearningAF.LoadTrainedAgent.loadAgentFromJson;
import static antiFrag.Position.FindPositionAndNeighbour.findClosestNode;
import static antiFrag.Position.FindPositionAndNeighbour.getPosition;
import static antiFrag.TwinInterrogation.buildCloneNetwork;


public class RL {
    static int discr_energyusageLevels = 10;
    static int discr_lossLevels = 10;
    static int discr_changePower = 5; // numero di livelli per `powerAdd` e `powerSub`
    static int discr_changeDistr = 5;
    static double epsilon = 1.0; // Probabilità di esplorazione
    static Random random = new Random();

    static double avgReward = 0.0;

    static int current_neigh = 0;

    private static int dimMotes = 15; //TODO deve essere dinamico!
    static int stateCount = discr_energyusageLevels * discr_lossLevels * 15;//discr_energyusageLevels * discr_lossLevels * discr_changePower * discr_changePower * discr_changeDistr;
    static int actionCount = discr_changeDistr * discr_changePower * discr_changePower;

    static AnomalyDetection anomalyDetection = new AnomalyDetection();
    public static double globalReward(ArrayList<QoS> results, boolean challenger) {
        /*
        if(challenger) {
            return calculateRewardEnergyNodes(results, 0);
        }
        else {
            return calculateRewardEnergyNodes(results, 10);
        }

         */
        return calculateLossReward(results);
    }

    public static void main(String[] args) {


        QLearner agent = new QLearner(stateCount, actionCount);

        startTraining(5000, agent, stateCount, actionCount, false, 0, "JsonRL/" + SimulationClientAF.Case.CASE1 + ".json", false);
        //anomalyDetection.init();
        //startTraining(1000, agent, stateCount, actionCount, false, 0, "JsonRL/" + "recoveryFromAnomaly" + ".json", true);


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

    private static SimulationClientAF createTrainingNetwork(boolean forceSimulationClient, int forcedneigh){
        Point2D point2D = getPosition();
        int neigh = findClosestNode(point2D);
        if(forceSimulationClient){
            while (neigh != forcedneigh){
                point2D = getPosition();
                neigh = findClosestNode(point2D);
            }
        }
        current_neigh = neigh;
        System.out.println("neighbour is: " + neigh);
        Random random2 = new Random();
        double delta = random2.nextInt(1001);
        SimulationClientAF sc = new SimulationClientAF(SimulationClientAF.Case.CASE1, (int) point2D.getX(), (int) point2D.getY(), 118800.0, 200, neigh, delta);
        return sc;
    }

    static public SimulationClientAF createRecoveryClient(int i){
        Point2D point2D = getPosition();
        int neigh = findClosestNode(point2D);
        current_neigh = neigh;
        System.out.println("neighbour is: " + neigh);
        Random random2 = new Random();
        double delta = 0;//random2.nextInt(1001);
        SimulationClientAF sc = new SimulationClientAF(SimulationClientAF.Case.CASE1, (int) point2D.getX(), (int) point2D.getY(), 118800.0, 200, neigh, delta);
        //utilizziamolo con l'anomalia e poi appena finisce la togliamo

        FeedbackLoopAFReactive feedbackLoop = new FeedbackLoopAFReactive(anomalyDetection);

        // get probe and effectors
        Probe probe = sc.getProbe();
        Effector effector = sc.getEffector();

        // Connect probe and effectors with feedback loop
        feedbackLoop.setProbe(probe);
        feedbackLoop.setEffector(effector);
        feedbackLoop.setNetwork(new SimulationClient(sc.getSimulator()));

        // StartFeedback loop
        SimulationClient temp = feedbackLoop.startRecovery(i);
        sc = new SimulationClientAF(temp.getSimulator());

        return sc;
    }
    public static void startTraining(int maxTime, QLearner agent, int stateCount, int actionCount, boolean forceSimulationClient, int forcedneigh, String filename, boolean recovery) {


        int currentState = random.nextInt(stateCount);
        List<Move> moves = new ArrayList<>();

        for (int time = 0; time < maxTime; ++time) {
            SimulationClientAF sc = null;
            if(!recovery){
                sc = createTrainingNetwork(forceSimulationClient, forcedneigh);
            }else{
                sc = createRecoveryClient(time);
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

            // Decodifica dell'azione per ottenere i valori per powerAdd, powerSub e distributionChange
            int[] powerAdd = decodePowerAdd(actionId);   // L'agente sceglie direttamente powerAdd
            int[] powerSub = decodePowerSub(actionId);   // L'agente sceglie direttamente powerSub
            int[] distributionChange = decodeDistributionChange(actionId);  // L'agente sceglie direttamente distributionChange

            // Assegna direttamente i valori decisi dall'agente
            int[] currentPowerAdd = powerAdd;
            int[] currentPowerSub = powerSub;
            int[] currentDistribution = distributionChange;

            // Crea l'oggetto di simulazione
            SimulationClient networkMgmt = new SimulationClient(sc.getSimulator());

            // Feedback loop e connessione a probe/effector
            FeedbackLoopRL feedbackLoop = new FeedbackLoopRL();
            Probe probe = networkMgmt.getProbe();
            Effector effector = networkMgmt.getEffector();
            feedbackLoop.setProbe(probe);
            feedbackLoop.setEffector(effector);
            feedbackLoop.setNetwork(networkMgmt);

            feedbackLoop.start(currentPowerAdd, currentPowerSub, currentDistribution);

            ArrayList<QoS> result = networkMgmt.getNetworkQoS(96);
            System.out.println("Run, PacketLoss, EnergyConsumption");
            result.forEach(qos -> System.out.println(qos));

            // Calcola la ricompensa in base alla nuova funzione
            //double reward = calculateReward(result);

            double reward = 0;
            if(!recovery){
                reward = globalReward(result, false);
            }else{
                reward = recoveryReward(networkMgmt);
            }
            if(!forceSimulationClient) {
                avgReward += reward;
            }

            // Determina il nuovo stato basato sui risultati della simulazione
            double averageEnergy = result.stream().mapToDouble(QoS::getEnergyConsumption).average().orElse(0.0);
            double averageLoss = result.stream().mapToDouble(QoS::getPacketLoss).average().orElse(0.0);

            int powerAddHash = Objects.hash(powerAdd);
            int powerSubHash = Objects.hash(powerSub);
            int distributionChangeHash = Objects.hash(distributionChange);
            int newStateId = getStateFromSimulation(averageEnergy, averageLoss, powerAddHash, powerSubHash, distributionChangeHash, current_neigh);

            // Aggiorna la lista di mosse e la Q-Table dell'agente
            moves.add(new Move(currentState, actionId, newStateId, reward));
            agent.update(currentState, actionId, newStateId, reward);


            currentState = newStateId;  // Aggiorna lo stato corrente

            //System.out.println("current powerAdd = " + currentPowerAdd + ", current powerSub = " + currentPowerSub.toString() + " , current distribution = " + currentDistribution.toString()+ ", reward = " + reward);
            System.out.println("current powerAdd = " + Arrays.toString(currentPowerAdd) +
                    ", current powerSub = " + Arrays.toString(currentPowerSub) +
                    ", current distribution = " + Arrays.toString(currentDistribution));
            System.out.println("reward = " + reward);
        }
        if(!forceSimulationClient){
            avgReward = avgReward/maxTime;
        }


        SaveJsonToFile saveJsonToFile = new SaveJsonToFile();
        saveJsonToFile.save(agent.toJson(), filename);
    }

    private static void moreSpecificTraining(int neigh, SimulationClientAF sc){
        String pathJson = "JsonRL/CASE1.json";
        if(evaluate(neigh, sc)) {
            System.out.println("adjust needed");
            QLearner trainedAgent = loadAgentFromJson(pathJson);
            startTraining(100, trainedAgent, stateCount, actionCount, true, neigh, "JsonRL/" + SimulationClientAF.Case.CASE1 + ".json", false);
        }else{
            System.out.println("adj not needed");
        }




    }

    public static double decreaseEpsilon(double epsilon){
        double ret = 0.0;

        ret = epsilon * 0.95; //dim 5%

        ret = Math.max(ret, 0.1);

        return ret;
    }

    private static boolean evaluate(int neigh, SimulationClientAF sc){
        LoadTrainedAgent loadTrainedAgent = new LoadTrainedAgent();
        int[][] prevConf = {{1, 1, 1,1,1,1,1,1,1,1,1,1,1,1,1},{1, 1, 1,1,1,1,1,1,1,1,1,1,1,1,1},{10, 10, 10,10,10,10,10,10,10,10,10,10,10,10,10}};
        String pathJson = "JsonRL/CASE1.json";
        int[][] resRL = loadTrainedAgent.interrogation(pathJson, neigh, sc.getSimulator(), prevConf[0], prevConf[1],prevConf[2]);
        // Crea l'oggetto di simulazione
        SimulationClient networkMgmt = new SimulationClient(sc.getSimulator());

        // Feedback loop e connessione a probe/effector
        FeedbackLoopRL feedbackLoop = new FeedbackLoopRL();
        Probe probe = networkMgmt.getProbe();
        Effector effector = networkMgmt.getEffector();
        feedbackLoop.setProbe(probe);
        feedbackLoop.setEffector(effector);
        feedbackLoop.setNetwork(networkMgmt);

        feedbackLoop.start(resRL[0], resRL[1], resRL[2]);

        ArrayList<QoS> result = networkMgmt.getNetworkQoS(96);
        double newReward = globalReward(result, false);
        System.out.println("avg reward: " + avgReward + " newreward: "+newReward);

        if(newReward < avgReward-500) {
            return true;
        }else {
            return false;
        }
    }




    // Metodi di supporto:


    public static int getStateFromSimulation(double energy, double loss, int powerAdd, int powerSub, int distChange, int neigh) {
        int energyState = (int) (energy / 10); // suddividi energia in intervalli di 10
        int lossState = (int) (loss * 10); // scala perdita tra 0-1 a intervalli di 0.1

        // Calcola un hash univoco utilizzando tutti i parametri
        //int state = Objects.hash(energyState, lossState, powerAdd, powerSub, distChange, neigh);
        int state = Objects.hash(energyState, lossState, neigh);
        return state;
    }


    public static int[] decodePowerAdd(int actionId) {
        int[] result = new int[dimMotes];
        for (int i = 0; i < dimMotes; i++) {
            result[i] = Math.abs((actionId) % 6); // Valori da 0 a 5
        }
        return result;
    }

    public static int[] decodePowerSub(int actionId) {
        int[] result = new int[dimMotes];
        for (int i = 0; i < dimMotes; i++) {
            result[i] = Math.abs(((actionId) / 6) % 6); // Valori da 0 a 5
        }
        return result;
    }

    public static int[] decodeDistributionChange(int actionId) {
        int[] result = new int[dimMotes];
        for (int i = 0; i < dimMotes; i++) {
            result[i] = Math.abs((((actionId) / 36) % 6) * 10); // Valori da 0 a 50 in step di 10
        }
        return result;
    }


    public static double recoveryReward(SimulationClient networkMgmt){
        double reward = 0;

        //no more anomaly, we want to restore classical behaviour as soon as possible


        //TODO vorrei far si che questa funzione di reward punti a minimizzare la distanza da un punto normale
        int timestamp = 94;//networkMgmt.getSimulator().getRunInfo().getRunNumber();
        double[] point = new double[9]; // Punto con 9 dimensioni
        ArrayList<QoS> qos = networkMgmt.getNetworkQoS(timestamp);

        // Dati da qos
        point[0] = qos.get(timestamp - 1).getPacketLoss();
        point[1] = qos.get(timestamp - 1).getEnergyConsumption();
        point[2] = qos.get(timestamp - 1).getNumNodesEnergy();
        point[3] = qos.get(timestamp - 1).getNumNodesLoss();
        point[4] = qos.get(timestamp - 1).getFairnessIndex();

        // Calcolo dei dati aggregati dai nodi e dai link
        double totBattery = 0.0;
        double averagePower = 0.0;
        double totalDistribution = 0.0;
        int linkCount = 0;

        List<domain.Mote> motes = networkMgmt.getSimulator().getMotes();
        for (domain.Mote m : motes) {
            totBattery += m.getBatteryRemaining(); // Batteria rimanente
            List<domain.Link> links = m.getLinks();
            for (domain.Link l : links) {
                averagePower += l.getPowerNumber();      // Potenza sul link
                totalDistribution += l.getDistribution(); // Distribuzione sul link
                linkCount++;
            }
        }

        // Dati calcolati dai nodi e dai link
        point[5] = totBattery / motes.size();       // Batteria media dei nodi
        point[6] = linkCount > 0 ? averagePower / linkCount : 0.0; // Potenza media sui link
        point[7] = linkCount > 0 ? totalDistribution / linkCount : 0.0; // Distribuzione media sui link
        point[8] = motes.size();                    // Numero totale di nodi

        timestamp = Math.min(96, timestamp); // Limita il timestamp massimo a 96


        double distance = anomalyDetection.getDistance(timestamp, point);
        System.out.println("distance = " + distance);
        reward += (1/distance)*10000;
        return reward;
    }





    public static double calculateReward(ArrayList<QoS> results) {
        double penalty = 0.0;
        double rewardBonus = 0.0;
        double maxPacketLossThreshold = 0.30;
        double minFairnessThreshold = 0.50;
        double highFairnessThreshold = 0.80;
        double stabilityRewardBonus = 5000; // Ricompensa cospicua per mantenimento o miglioramento

        int resultSize = results.size();
        if (resultSize == 0) return 0.0;

        // Otteniamo i valori iniziali per confronti progressivi
        double initialLoss = results.get(0).getPacketLoss();
        double initialEnergy = results.get(0).getEnergyConsumption();
        double initialFairness = results.get(0).getFairnessIndex();
        int initialNumNodesEnergy = results.get(0).getNumNodesEnergy();

        for (int i = 1; i < resultSize; i++) {
            double currentLoss = results.get(i).getPacketLoss();
            double currentEnergy = results.get(i).getEnergyConsumption();
            double currentFairness = results.get(i).getFairnessIndex();
            int currentNumNodesEnergy = results.get(i).getNumNodesEnergy();

            boolean stableOrImproving = true;

            // Packet Loss Constraints
            if (currentLoss > maxPacketLossThreshold) {
                penalty += 2000 * (currentLoss - maxPacketLossThreshold); // Penalità alta per superamento soglia
                stableOrImproving = false;
            }
            if (currentLoss > initialLoss) {
                penalty += 500; // Penalità se il packet loss aumenta
                stableOrImproving = false;
            }

            // Energy Consumption Constraints
            if (currentEnergy > initialEnergy) {
                penalty += 250; // Penalità se il consumo energetico aumenta
                stableOrImproving = false;
            }

            // Fairness Index Constraints
            if (currentFairness < minFairnessThreshold) {
                penalty += 1000; // Penalità per fairness troppo bassa
                stableOrImproving = false;
            } else if (currentFairness >= highFairnessThreshold) {
                rewardBonus += 2000; // Premio per alta fairness
            }

            // Num Nodes Energy Constraints
            penalty += 500 * currentNumNodesEnergy; // Penalità progressiva basata su NumNodesEnergy
            if (currentNumNodesEnergy > initialNumNodesEnergy) {
                stableOrImproving = false;
            }

            // Ricompensa cospicua se tutti i parametri sono migliorati o rimasti stabili
            if (stableOrImproving) {
                rewardBonus += stabilityRewardBonus;
            }

            // Aggiorna i valori iniziali per la prossima iterazione
            initialLoss = currentLoss;
            initialEnergy = currentEnergy;
            initialFairness = currentFairness;
            initialNumNodesEnergy = currentNumNodesEnergy;
        }

        // Penalità aggiuntiva per packet loss medio sopra la soglia
        double currentAvgLoss = results.stream().mapToDouble(QoS::getPacketLoss).average().orElse(0.0);
        if (currentAvgLoss > maxPacketLossThreshold) {
            penalty += 1000 * (currentAvgLoss - maxPacketLossThreshold);
        }



        // Calcolo del reward finale come differenza tra bonus e penalità
        double reward = rewardBonus - penalty;

        return reward;
    }




    public static double calculateRewardFairness(ArrayList<QoS> results) {
        double reward = 0.0;
        int resultSize = results.size();
        if (resultSize < 2) return reward; // Se ci sono meno di 2 iterazioni, nessuna valutazione può essere fatta

        for(int i = 0; i <resultSize; i++){

            reward += 100*results.get(i).getFairnessIndex()*i;

        }


        return reward;

    }

    public static double calculateRewardEnergyUsage(ArrayList<QoS> results) {
        double reward = 0.0;

        for (QoS result : results) {
            double currentEnergy = result.getEnergyConsumption();

            // Premia la minimizzazione del consumo energetico. Più basso è il consumo energetico, maggiore è il reward.
            reward -=  currentEnergy; // Assumendo che il consumo energetico medio sia intorno a 100
        }

        return reward;

    }

    public static double calculateLossReward(List<QoS> results) {
        double reward = 0.0;

        for (QoS result : results) {
            double currentLoss = result.getPacketLoss();

            // Premia la minimizzazione della packet loss. Più il valore è vicino a 0, maggiore è il reward.
            reward += (1.0 - currentLoss)*100; // Assumendo che il range della packet loss sia tra 0 e 1
        }

        return reward;
    }


    public static double calculateRewardEnergyNodes(ArrayList<QoS> results, int stop) {
        double penalty = 0.0;
        double rewardBonus = 0.0;

        int resultSize = results.size();
        if (resultSize == 0) return 0.0;
        int initialNodesEnergy = results.get(0).getNumNodesEnergy();
        boolean hasDecreasedEarly = false;

        if (stop != 0) {
            resultSize = Math.min(resultSize, stop);
        }

        for (int i = 1; i < resultSize; i++) {
            int currentNodesEnergy = results.get(i).getNumNodesEnergy();

            // Premia decrementi immediati
            if (!hasDecreasedEarly && currentNodesEnergy < initialNodesEnergy) {
                rewardBonus += 2000; // Ricompensa maggiore per decremento iniziale
                hasDecreasedEarly = true; // Segnala che un decremento è avvenuto
            } else if (currentNodesEnergy < initialNodesEnergy) {
                rewardBonus += 500; // Ricompensa minore per decrementi tardivi
            } else if (currentNodesEnergy > initialNodesEnergy) {
                penalty += 1000; // Penalità per aumento
            }

            // Penalità incrementale per numero di nodi elevato
            penalty += 50 * currentNodesEnergy;

            // Aggiorna lo stato iniziale per il confronto successivo
            initialNodesEnergy = currentNodesEnergy;
        }

        // Penalità aggiuntiva se non c'è mai stato un decremento
        if (!hasDecreasedEarly) {
            penalty += 5000; // Penalità significativa se non c'è decremento
        }

        return rewardBonus - penalty;
    }





}
