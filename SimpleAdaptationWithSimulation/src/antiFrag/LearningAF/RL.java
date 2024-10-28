package antiFrag.LearningAF;
import antiFrag.SimulationClientAF;
import antiFrag.Utils.SaveJsonToFile;
import com.github.chen0040.rl.learning.qlearn.QLearner;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.client.SimulationClient;
import mapek.FeedbackLoopRL;
import simulator.QoS;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static antiFrag.Position.FindPositionAndNeighbour.findClosestNode;
import static antiFrag.Position.FindPositionAndNeighbour.getPosition;

class Move {
    int oldState;
    int newState;
    int action;
    double reward;

    public Move(int oldState, int action, int newState, double reward) {
        this.oldState = oldState;
        this.newState = newState;
        this.reward = reward;
        this.action = action;
    }
}

public class RL {
    static int discr_energyusageLevels = 10;
    static int discr_lossLevels = 10;
    static int discr_changePower = 5; // numero di livelli per `powerAdd` e `powerSub`
    static int discr_changeDistr = 5;
    static double epsilon = 0.1; // Probabilità di esplorazione (10%)
    static Random random = new Random();

    public static void main(String[] args) {
        int stateCount = discr_energyusageLevels * discr_lossLevels * discr_changePower * discr_changePower * discr_changeDistr;
        int actionCount = discr_changeDistr * discr_changePower * discr_changePower;

        QLearner agent = new QLearner(stateCount, actionCount);

        int currentState = random.nextInt(stateCount);
        List<Move> moves = new ArrayList<>();

        for (int time = 0; time < 5000; ++time) {
            Point2D point2D = getPosition();
            int neigh = findClosestNode(point2D);
            System.out.println("neighbour is: " + neigh);
            SimulationClientAF sc = new SimulationClientAF(SimulationClientAF.Case.CASE1, (int) point2D.getX(), (int) point2D.getY(), 118800.0, 200, neigh);

            // Esplorazione epsilon-greedy
            int actionId;
            if (random.nextDouble() < epsilon) {
                actionId = random.nextInt(actionCount);
                System.out.println("Agent explores with action-" + actionId);
            } else {
                actionId = agent.selectAction(currentState).getIndex();
                System.out.println("Agent exploits with action-" + actionId);
            }

            // Decodifica dell'azione per ottenere i valori per powerAdd, powerSub e distributionChange
            int powerAdd = decodePowerAdd(actionId);   // L'agente sceglie direttamente powerAdd
            int powerSub = decodePowerSub(actionId);   // L'agente sceglie direttamente powerSub
            int distributionChange = decodeDistributionChange(actionId);  // L'agente sceglie direttamente distributionChange

            // Assegna direttamente i valori decisi dall'agente
            int currentPowerAdd = powerAdd;
            int currentPowerSub = powerSub;
            int currentDistribution = distributionChange;

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
            double reward = calculateReward(result);

            // Determina il nuovo stato basato sui risultati della simulazione
            double averageEnergy = result.stream().mapToDouble(QoS::getEnergyConsumption).average().orElse(0.0);
            double averageLoss = result.stream().mapToDouble(QoS::getPacketLoss).average().orElse(0.0);

            int newStateId = getStateFromSimulation(averageEnergy, averageLoss, powerAdd, powerSub, distributionChange, neigh);

            // Aggiorna la lista di mosse e la Q-Table dell'agente
            moves.add(new Move(currentState, actionId, newStateId, reward));
            agent.update(currentState, actionId, newStateId, reward);

            currentState = newStateId;  // Aggiorna lo stato corrente

            System.out.println("current powerAdd = " + currentPowerAdd + ", current powerSub = " + currentPowerSub + " , current distribution = " + currentDistribution + ", reward = " + reward);
        }

        SaveJsonToFile saveJsonToFile = new SaveJsonToFile();
        saveJsonToFile.save(agent.toJson(), "JsonRL/" + SimulationClientAF.Case.CASE1 + ".json");
    }




    // Metodi di supporto:
    private static int getStateFromSimulation(double energy, double loss, int powerAdd, int powerSub, int distChange, int neigh) {
        int energyState = (int) (energy / 10); // dividendo in intervalli di 10
        int lossState = (int) (loss * 10); // scala la perdita tra 0-1 a intervalli di 0.1
        int state = energyState * 10000 + lossState * 1000 + powerAdd * 100 + powerSub * 10 + distChange + neigh; // combinazione unica di stato
        return state;
    }

    private static int decodePowerAdd(int actionId) {
        return (actionId % 5) + 1; // Valori da 1 a 5, garantiti positivi per aggiungere potenza
    }

    private static int decodePowerSub(int actionId) {
        return ((actionId / 5) % 5) + 1; // Valori da 1 a 5, garantiti positivi per sottrarre potenza
    }

    private static int decodeDistributionChange(int actionId) {
        return (((actionId / 5) % 5) + 1)*10; // Valori da 1 a 5, garantiti positivi
    }


    /*
    private static double calculateReward(ArrayList<QoS> result) {
        double totalEnergy = 0.0;
        double totalPacketLoss = 0.0;
        double penalty = 0.0;
        double rewardBonus = 0.0;
        int counter_min30 = 0;
        int counter_more30 = 0;

        for (QoS qos : result) {
            totalEnergy += qos.getEnergyConsumption();
            totalPacketLoss += qos.getPacketLoss();

            // Penalità maggiore se il PacketLoss supera 0.30
            if (qos.getPacketLoss() > 0.30) {
                double overThreshold = qos.getPacketLoss() - 0.30;
                penalty += 500 * overThreshold; // Penalità progressiva per ogni incremento sopra la soglia
                penalty += 1000; // Penalità costante se supera la soglia
                counter_more30++;
            }else {
                counter_min30++;
            }

            if(Math.abs(counter_min30-counter_more30)<10){
                penalty += 2000;
            }

            // Aggiungere una ricompensa extra se il PacketLoss è molto basso (<0.10)
            if (qos.getPacketLoss() < 0.10) {
                rewardBonus += 50; // Ricompensa aggiuntiva per configurazioni con basso PacketLoss
            }
        }

        double averagePacketLoss = totalPacketLoss / result.size();
        double reward = -totalEnergy - penalty + rewardBonus;

        // Penalità ulteriore se l'average PacketLoss è sopra 0.30
        if (averagePacketLoss > 0.30) {
            reward -= 2000; // Penalità aggiuntiva per tenere alto il PacketLoss
        }

        // Aggiungere una ricompensa extra se il PacketLoss medio è sotto 0.30
        if (averagePacketLoss <= 0.30) {
            reward += 100; // Ricompensa per mantenere il PacketLoss sotto il limite accettabile
        }

        return reward;
    }

     */

    private static double calculateReward(ArrayList<QoS> results) {
        double totalEnergy = 0.0;
        double totalPacketLoss = 0.0;
        double penalty = 0.0;
        double rewardBonus = 0.0;

        int resultSize = results.size();

        // Calcola la media corrente di Energy e Packet Loss
        double currentAvgEnergy = results.stream().mapToDouble(QoS::getEnergyConsumption).average().orElse(0.0);
        double currentAvgLoss = results.stream().mapToDouble(QoS::getPacketLoss).average().orElse(0.0);

        // Premi per riduzioni rispetto alle iterazioni precedenti, con enfasi su packet loss
        if (resultSize > 1) {
            // Ottieni i valori dell'iterazione precedente
            QoS prevQoS = results.get(resultSize - 2);
            double prevEnergy = prevQoS.getEnergyConsumption();
            double prevLoss = prevQoS.getPacketLoss();

            // Riduzione di packet loss e energy favorisce la ricompensa, con peso doppio per packet loss
            rewardBonus += 2 * (prevLoss - currentAvgLoss) + (prevEnergy - currentAvgEnergy);

            // Penalità se i valori di loss o energy sono sopra la soglia desiderata
            if (currentAvgLoss > 0.30) penalty += 1000 * (currentAvgLoss - 0.30); // Penalità più alta per packet loss
            if (currentAvgEnergy > prevEnergy) penalty += 500 * (currentAvgEnergy - prevEnergy); // Penalità per aumento di energy

            // Penalità per fluttuazioni (spikes) analizzando tutta la cronologia
            for (int i = 1; i < resultSize; i++) {
                double prevLossSpike = results.get(i - 1).getPacketLoss();
                double currentLossSpike = results.get(i).getPacketLoss();
                double prevEnergySpike = results.get(i - 1).getEnergyConsumption();
                double currentEnergySpike = results.get(i).getEnergyConsumption();

                // Penalità per variazioni elevate tra un timestamp e l'altro
                if (Math.abs(currentLossSpike - prevLossSpike) > 0.05) penalty += 100; // Penalità per fluttuazione in loss
                if (Math.abs(currentEnergySpike - prevEnergySpike) > 5) penalty += 50; // Penalità per fluttuazione in energy
            }
        }

        // Ricompensa finale calcolata come differenza tra premio per miglioramenti e penalità per peggioramenti
        double reward = rewardBonus - penalty;
        return reward;
    }



}
