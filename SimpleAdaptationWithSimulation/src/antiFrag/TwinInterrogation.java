package antiFrag;

import antiFrag.LearningAF.LoadTrainedAgent;
import deltaiot.client.SimulationClient;
import domain.*;
import mapek.FeedbackLoop;
import simulator.QoS;
import simulator.Simulator;

import java.util.ArrayList;
import java.util.List;

public class TwinInterrogation {
    /**
     * tries various scenarios and selects the least harmful
     */
    private SimulationClient actualNetwork;

    private FeedbackLoop feedbackLoop;

    public TwinInterrogation(SimulationClient actualNetwork) {
        this.actualNetwork = actualNetwork;
    }

    public static Simulator buildCloneNetwork(SimulationClient ntwrk){
        Simulator simul = new Simulator();
        List<Mote> motes = ntwrk.getSimulator().getMotes();

        Mote[] allMotes = motes.toArray(new Mote[0]);

        simul.addMotes(allMotes);

        // Gateway
        Gateway gateway = ntwrk.getSimulator().getGatewayWithId(1);
        gateway.setView(allMotes);
        simul.addGateways(gateway);

        // links
        List<Integer> order = ntwrk.getSimulator().getTurnOrder();
        // turns
        Integer[] turnOrderArray = order.toArray(new Integer[0]);
        simul.setTurnOrder(turnOrderArray);
        // Global random interference (mimicking Usman's random interference) simul.getRunInfo().setGlobalInterference(new DoubleRange(-5.0, 5.0));
        //simul.getRunInfo().setGlobalInterference(new DoubleRange(-5.0, 5.0));
        Profile<Double> interference  = ntwrk.getSimulator().getRunInfo().getGlobalInterference();
        simul.getRunInfo().setGlobalInterference(interference);

        return simul;
    }

    public int[] start(){
        boolean firstTime = true;
        ArrayList<QoS> bestResult = null;

        int bestj = 0;
        int bestd = 0;
        System.out.println("twin starting, at run: " + actualNetwork.getSimulator().getRunInfo().getRunNumber());

        for(int i = 0; i<5;i++){
            for(int j = 0; j<5; j++) {
                //System.out.println("twinInterrogation!"+i+j);
                //Simulator copy = new Simulator();
                Simulator copy = buildCloneNetwork(actualNetwork);


                //System.out.println("run number twin:"+copy.getRunInfo().getRunNumber());
                BetterFeedbackAF betterFeedbackAF = new BetterFeedbackAF(new SimulationClientAF(copy));

                ArrayList<QoS> temp = betterFeedbackAF.start(SimulationClientAF.Case.CASE1, j,i,firstTime, bestResult);
                firstTime = false;

                if(!temp.equals(bestResult)){
                    bestj = j;
                    bestd = i;
                }

                bestResult = temp;
            }
        }
        int[] ret = {bestd, bestj};
        return ret;
    }

    public int[][] startRL(int[][] prevConf){ //TODO passare il caso individuato dall'anomaly detection, andrebbe messo nello stato del RL
        Simulator copy = buildCloneNetwork(actualNetwork);
        LoadTrainedAgent rl = new LoadTrainedAgent();
        String path = "JsonRL/CASE1.json";
        List<Link> links = copy.getMoteWithId(16).getLinks();//TODO non possiamo assumere che sia anomalia 1 e che il nodo abbia 16 come id
        int neigh = 0;
        for (Link link : links) {
            neigh = link.getTo().getId(); // get neigh
        }
        return rl.interrogation(path, neigh, copy, prevConf[0], prevConf[1],prevConf[2]);
    }

    public int[][] startRecovery(int[][] prevConf){ //TODO passare il caso individuato dall'anomaly detection
        Simulator copy = buildCloneNetwork(actualNetwork);
        LoadTrainedAgent rl = new LoadTrainedAgent();
        String path = "JsonRL/recoveryFromAnomaly.json";

        return rl.interrogation(path, 0, copy, prevConf[0], prevConf[1],prevConf[2]);
    }

    public void setActualNetwork(SimulationClient sc){
        this.actualNetwork = sc;
    }
}
