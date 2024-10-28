package antiFrag;

import antiFrag.LearningAF.LoadTrainedAgent;
import deltaiot.client.SimulationClient;
import domain.DoubleRange;
import domain.Gateway;
import domain.Link;
import domain.Mote;
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

    private static Simulator buildCloneNetwork(SimulationClient ntwrk){
        Simulator simul = new Simulator();
        //ArrayList<Mote> motes = ntwrk.getAllMotes();
        List<Mote> motes = ntwrk.getSimulator().getMotes();

        Mote[] allMotes = motes.toArray(new Mote[0]);

        simul.addMotes(allMotes);

        // Gateway
        Gateway gateway = ntwrk.getSimulator().getGatewayWithId(1);
        gateway.setView(allMotes);
        simul.addGateways(gateway);

        //i link dovrebbero essere ok
        List<Integer> order = ntwrk.getSimulator().getTurnOrder();

        Integer[] turnOrderArray = order.toArray(new Integer[0]);
        simul.setTurnOrder(turnOrderArray);
        // Global random interference (mimicking Usman's random interference) simul.getRunInfo().setGlobalInterference(new DoubleRange(-5.0, 5.0));
        simul.getRunInfo().setGlobalInterference(new DoubleRange(-5.0, 5.0));


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

    public int[] startRL(){ //TODO passare il caso individuato dall'anomaly detection, andrebbe messo nello stato del RL
        Simulator copy = buildCloneNetwork(actualNetwork);
        LoadTrainedAgent rl = new LoadTrainedAgent();
        String path = "JsonRL/CASE1.json";
        List<Link> links = copy.getMoteWithId(16).getLinks();
        int neigh = 0;
        for (Link link : links) {
            neigh = link.getTo().getId(); // get neigh
        }
        return rl.interrogation(path, neigh, copy);
    }
}
