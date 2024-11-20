package main;

import antiFrag.AnomalyDetection.AnomalyDetection;
import antiFrag.SimulationClientAF;
import antiFrag.Utils.CsvWriter;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.client.SimulationClient;
import mapek.FeedbackLoopAFReactive;
import simulator.QoS;
import simulator.Simulator;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static antiFrag.Position.FindPositionAndNeighbour.findClosestNode;
import static antiFrag.Position.FindPositionAndNeighbour.getPosition;

public class AFAdaptation {
    SimulationClient networkMgmt;
    CsvWriter csvWriter = new CsvWriter();
    static AnomalyDetection anomalyDetection;
    SimulationClient simulationClientRecovery;
    public void start(int i){

        Point2D point2D = getPosition();
        int neigh = findClosestNode(point2D);
        System.out.println("il vicino è: " + neigh);
        SimulationClientAF sc = new SimulationClientAF(SimulationClientAF.Case.CASE1, (int)point2D.getX(), (int)point2D.getY(), 118800.0, 200, neigh);
        // Create a simulation client object
        networkMgmt = new SimulationClient(sc.getSimulator());



        // Create Feedback loop
        //FeedbackLoop feedbackLoop = new FeedbackLoop();
        FeedbackLoopAFReactive feedbackLoop = new FeedbackLoopAFReactive(anomalyDetection);

        // get probe and effectors
        Probe probe = networkMgmt.getProbe();
        Effector effector = networkMgmt.getEffector();

        // Connect probe and effectors with feedback loop
        feedbackLoop.setProbe(probe);
        feedbackLoop.setEffector(effector);
        feedbackLoop.setNetwork(networkMgmt);

        // StartFeedback loop
        networkMgmt = feedbackLoop.start(i);
        int stopIdx = feedbackLoop.getRecoveredTimestamp();
        System.out.println(networkMgmt.getAllMotes());

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

        simulationClientRecovery = feedbackLoop.startRecovery(i);


        ArrayList<QoS> resultRecovery = simulationClientRecovery.getNetworkQoS(96-stopIdx-1);

        System.out.println("Run, PacketLoss, EnergyConsumption, ");
        result.forEach(qos -> System.out.println(qos));

        System.out.println("Run, PacketLoss, EnergyConsumption, ");
        resultRecovery.forEach(qos -> System.out.println(qos));


        System.out.println(simulationClientRecovery.getAllMotes());

        ArrayList<QoS> finalResult = new ArrayList<>();
        for(int k = 0; k<=stopIdx; k++){
            finalResult.add(result.get(k));
        }

        finalResult.addAll(resultRecovery);
        csvWriter.writeQoSToCSV(finalResult, "BetterPolicy/simulation"+i+"_neigh" +neigh+ "_stopAnomaly"+ stopIdx+".csv");
    }

    public static void main(String[] args) {
        initAD();

        for(int i = 0; i<100; i++) {
            AFAdaptation client = new AFAdaptation();
            client.start(i);

        }


        //AFAdaptation client = new AFAdaptation();
        //client.start(1);
    }

    public Simulator getSimulator() {
        return networkMgmt.getSimulator();
    }

    public static void initAD(){
        anomalyDetection = new AnomalyDetection();
        anomalyDetection.init();
    }
}
