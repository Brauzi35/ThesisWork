package main;

import antiFrag.SimulationClientAF;
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
    public void start(){

        Point2D point2D = getPosition();
        int neigh = findClosestNode(point2D);
        System.out.println("il vicino è: " + neigh);
        SimulationClientAF sc = new SimulationClientAF(SimulationClientAF.Case.CASE1, (int)point2D.getX(), (int)point2D.getY(), 118800.0, 200, neigh);
        // Create a simulation client object
        networkMgmt = new SimulationClient(sc.getSimulator());



        // Create Feedback loop
        //FeedbackLoop feedbackLoop = new FeedbackLoop();
        FeedbackLoopAFReactive feedbackLoop = new FeedbackLoopAFReactive();

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

    }

    public static void main(String[] args) {
        AFAdaptation client = new AFAdaptation();
        client.start();
    }

    public Simulator getSimulator() {
        return networkMgmt.getSimulator();
    }
}
