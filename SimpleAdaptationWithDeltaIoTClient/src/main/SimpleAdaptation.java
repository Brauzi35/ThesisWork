package main;

import java.util.List;

import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.services.QoS;
import mapek.FeedbackLoop;

public class SimpleAdaptation {
	
	Probe probe;
	Effector effector;
	
	public void start(){

		// get probe and effectors
		probe = new Probe();
		effector = new Effector();

		// Connect probe and effectors with feedback loop
		FeedbackLoop feedbackLoop = new FeedbackLoop();
		feedbackLoop.setProbe(probe);
		feedbackLoop.setEffector(effector);

		// StartFeedback loop
		feedbackLoop.start();

		// See results
		printResults();
	}
	
	void printResults(){
		// Get QoS data of previous runs
		List<QoS> qosList =  probe.getNetworkQoS(96);
		for(QoS qos: qosList){
			System.out.println(String.format("%s, %d, %d", qos.getPeriod(), 
					qos.getPacketLoss(), qos.getEnergyConsumption()));
		}
	}

	public static void main(String[] args) {
		SimpleAdaptation simpleAdaptation = new SimpleAdaptation();
		simpleAdaptation.start();
 	}
}
