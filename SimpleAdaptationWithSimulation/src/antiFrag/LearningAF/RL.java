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

        int currentPowerAdd = 1; // Inizializza a 1 per garantire valori positivi
        int currentPowerSub = 1; // Inizializza a 1 per garantire valori positivi
        int currentDistribution = 1; // Inizializza a 1 per garantire valori positivi

        for (int time = 0; time < 1000; ++time) {
            Point2D point2D = getPosition();
            int neigh = findClosestNode(point2D); //voglio anche questo nello stato
            System.out.println("neighbour is: " + neigh);
            SimulationClientAF sc = new SimulationClientAF(SimulationClientAF.Case.CASE1, (int) point2D.getX(), (int) point2D.getY(), 118800.0, 200, neigh);

            // Modifica per usare epsilon-greedy esplorazione:
            int actionId;
            if (random.nextDouble() < epsilon) {
                // Esplora una nuova azione casualmente
                actionId = random.nextInt(actionCount);
                System.out.println("Agent explores with action-" + actionId);
            } else {
                // Sfrutta l'azione migliore conosciuta
                actionId = agent.selectAction(currentState).getIndex();
                System.out.println("Agent exploits with action-" + actionId);
            }

            // Decodifica l'azione per determinare le modifiche alla potenza e distribuzione
            int powerAdd = decodePowerAdd(actionId);
            int powerSub = decodePowerSub(actionId);
            int distributionChange = decodeDistributionChange(actionId);

            // Create a simulation client object
            SimulationClient networkMgmt = new SimulationClient(sc.getSimulator());

            // Create Feedback loop
            FeedbackLoopRL feedbackLoop = new FeedbackLoopRL();

            // get probe and effectors
            Probe probe = networkMgmt.getProbe();
            Effector effector = networkMgmt.getEffector();

            // Connect probe and effectors with feedback loop
            feedbackLoop.setProbe(probe);
            feedbackLoop.setEffector(effector);
            feedbackLoop.setNetwork(networkMgmt);

            // Start Feedback loop
            //int effectivePower = Math.max(1, currentPowerAdd - currentPowerSub); // Calcola la potenza effettiva
            feedbackLoop.start(currentPowerAdd, currentPowerSub, currentDistribution);

            ArrayList<QoS> result = networkMgmt.getNetworkQoS(96);

            System.out.println("Run, PacketLoss, EnergyConsumption");
            result.forEach(qos -> System.out.println(qos));

            // Calcola la ricompensa: vogliamo minimizzare il consumo di energia e tenere la perdita di pacchetti < 0.30
            double reward = calculateReward(result);

            // Determina il nuovo stato basato sui risultati della simulazione
            double averageEnergy = result.stream().mapToDouble(QoS::getEnergyConsumption).average().orElse(0.0);
            double averageLoss = result.stream().mapToDouble(QoS::getPacketLoss).average().orElse(0.0);

            int newStateId = getStateFromSimulation(averageEnergy, averageLoss, powerAdd, powerSub, distributionChange, neigh);

            int oldStateId = currentState;
            moves.add(new Move(oldStateId, actionId, newStateId, reward));
            currentState = newStateId;

            // Aggiorna i valori di potenza e distribuzione correnti
            currentPowerAdd = Math.max(1, currentPowerAdd + powerAdd); // Usa `powerAdd`
            currentPowerSub = Math.max(1, currentPowerSub + powerSub); // Usa `powerSub`
            currentDistribution = Math.max(1, currentDistribution + distributionChange);

            // Limita la potenza e la distribuzione ai valori massimi consentiti
            currentPowerAdd = Math.min(currentPowerAdd, 10); // Imposta il limite massimo per la potenza aggiunta a 10
            currentPowerSub = Math.min(currentPowerSub, 10); // Imposta il limite massimo per la potenza sottratta a 10
            currentDistribution = Math.min(currentDistribution, 50); // Imposta il limite massimo per la distribuzione a 50

            // Aggiorna la Q-Table dell'agente
            agent.update(oldStateId, actionId, newStateId, reward);

            System.out.println("current powerAdd = " + currentPowerAdd + ", current powerSub = " + currentPowerSub + " , current distribution = " + currentDistribution + ", reward = " + reward);
        }

        SaveJsonToFile saveJsonToFile = new SaveJsonToFile();
        saveJsonToFile.save(agent.toJson(), "JsonRL/" + SimulationClientAF.Case.CASE1 + ".json"); //TODO il case va passato quando si chiama il training
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
        return ((actionId / 5) % 5) + 1; // Valori da 1 a 5, garantiti positivi
    }

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

}
