package main;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import antiFrag.BetterFeedbackAF;
import antiFrag.SimulationClientAF;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.client.SimulationClient;
import domain.Link;
import domain.Mote;
import mapek.FeedbackLoop;
import mapek.FeedbackLoopAFReacrive;
import simulator.QoS;
import simulator.Simulator;

import static antiFrag.BetterFeedbackAF.writeMotesToCsv;
import static antiFrag.BetterFeedbackAF.writeQoSToCSV;
import static antiFrag.Position.FindPositionAndNeighbour.findClosestNode;
import static antiFrag.Position.FindPositionAndNeighbour.getPosition;

public class SimpleAdaptation {

	/**
	 * the original simple adaptation class, standard network configuration
	 */

	SimulationClient networkMgmt;
	static int counter;
	public void start(){
		BetterFeedbackAF betterFeedbackAF = new BetterFeedbackAF();
		// Create a simulation client object
		networkMgmt = new SimulationClient();
		if(networkMgmt == null){
			System.out.println("NULL");
		}

		// Create Feedback loop
		FeedbackLoop feedbackLoop = new FeedbackLoop();

		// get probe and effectors
		Probe probe = networkMgmt.getProbe();
		Effector effector = networkMgmt.getEffector();

		// Connect probe and effectors with feedback loop
		feedbackLoop.setProbe(probe);
		feedbackLoop.setEffector(effector);

		// StartFeedback loop
		feedbackLoop.start();


		writeMotesToCsv(networkMgmt.getAllMotes(), "AnomalyDetectionFiles/motes_"+counter+".csv");





		ArrayList<QoS> result = networkMgmt.getNetworkQoS(96);
		writeQoSToCSV(result, "AnomalyDetectionFiles/qos_"+counter+".csv");
		System.out.println("Run, PacketLoss, EnergyConsumption");
		result.forEach(qos -> System.out.println(qos));

	}

	public static void main(String[] args) {


		for(int i = 0; i<100; i++){
			SimpleAdaptation client = new SimpleAdaptation();
			counter=i;
			client.start();

		}
		SimpleAdaptation client = new SimpleAdaptation();





	}


	public Simulator getSimulator() {

		return networkMgmt.getSimulator();
	}
}
