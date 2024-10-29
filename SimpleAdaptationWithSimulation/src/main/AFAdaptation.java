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

import static antiFrag.Position.FindPositionAndNeighbour.findClosestNode;
import static antiFrag.Position.FindPositionAndNeighbour.getPosition;

public class AFAdaptation {
    SimulationClient networkMgmt;
    CsvWriter csvWriter = new CsvWriter();
    static AnomalyDetection anomalyDetection;
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
        feedbackLoop.start();

        ArrayList<QoS> result = networkMgmt.getNetworkQoS(96);

        System.out.println("Run, PacketLoss, EnergyConsumption");
        result.forEach(qos -> System.out.println(qos));



        csvWriter.writeQoSToCSV(result, "BetterPolicy/simulation"+i+"_neigh" +neigh+".csv");
    }

    public static void main(String[] args) {
        initAD();
        for(int i = 0; i<100; i++) {
            AFAdaptation client = new AFAdaptation();
            client.start(i);

        }
    }

    public Simulator getSimulator() {
        return networkMgmt.getSimulator();
    }

    public static void initAD(){
        anomalyDetection = new AnomalyDetection();
        anomalyDetection.init();
    }
}
