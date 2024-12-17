package main;

import antiFrag.LearningAF.Move;
import antiFrag.LearningAF.RL;
import antiFrag.SimulationClientAF;
import antiFrag.Utils.WealthScore;
import com.github.chen0040.rl.learning.qlearn.QLearner;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.client.SimulationClient;
import mapek.FeedbackLoopChallenger;
import mapek.FeedbackLoopOracle;
import simulator.QoS;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static antiFrag.LearningAF.RL.*;
import static antiFrag.Position.FindPositionAndNeighbour.findClosestNode;
import static antiFrag.Position.FindPositionAndNeighbour.getPosition;
import static antiFrag.Utils.CsvWriter.writeOrUpdateScoreToCSV;
import static antiFrag.BetterFeedbackAF.writeQoSToCSV;

public class AFOracle {

    public static void start(int trainingTime){


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



            // simulation setup
            SimulationClient networkMgmt = new SimulationClient(sc.getSimulator());

            // Feedback loop and probe/effector connection
            FeedbackLoopOracle feedbackLoop = new FeedbackLoopOracle();
            Probe probe = networkMgmt.getProbe();
            Effector effector = networkMgmt.getEffector();
            feedbackLoop.setProbe(probe);
            feedbackLoop.setEffector(effector);
            feedbackLoop.setNetwork(networkMgmt);

            networkMgmt = feedbackLoop.start(time);
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

            SimulationClient fin = feedbackLoop.start(time);

            ArrayList<QoS> resultRecovery = fin.getNetworkQoS(96-stopIdx-1);

            ArrayList<QoS> finalResult = new ArrayList<>();
            for(int k = 0; k<=stopIdx; k++){
                finalResult.add(result.get(k));
            }
            finalResult.addAll(resultRecovery);


            //System.out.println("current powerAdd = " + currentPowerAdd + ", current powerSub = " + currentPowerSub + " , current distribution = " + currentDistribution + ", reward = " + reward);

            //writeQoSToCSV(finalResult, "ChallengerProgression/simulation"+time+"_neigh"+ Arrays.toString(neigh_arr)+ "_stopAnomaly"+ stopIdx +".csv");
            WealthScore wealthScore = new WealthScore();
            double score = wealthScore.calculateScore(finalResult);
            //writeOrUpdateScoreToCSV(time, score, "challenger.csv");
            
        }
    }

    public static void main(String[] args) {
        start(100);
    }

}
