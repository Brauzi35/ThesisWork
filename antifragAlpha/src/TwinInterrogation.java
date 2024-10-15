import LearningAF.ReactiveAF;
import deltaiot.client.SimulationClient;
import mapek.FeedbackLoop;
import simulator.QoS;
import simulator.Simulator;

import java.util.ArrayList;

public class TwinInterrogation {
    /**
     * tries various scenarios and selects the least harmful
     */
    private SimulationClient actualNetwork;

    private FeedbackLoop feedbackLoop;

    public TwinInterrogation(SimulationClient actualNetwork) {
        this.actualNetwork = actualNetwork;
    }

    public int[] start(){
        boolean firstTime = true;
        ArrayList<QoS> bestResult = null;

        int bestj = 0;
        int bestd = 0;

        for(int i = 0; i<5;i++){
            for(int j = 0; j<5; j++) {
                BetterFeedbackAF betterFeedbackAF = new BetterFeedbackAF(new SimulationClientAF(actualNetwork.getSimulator()));

                ArrayList<QoS> temp = betterFeedbackAF.start(SimulationClientAF.Case.CASE1, i, j, firstTime, bestResult);
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
}
