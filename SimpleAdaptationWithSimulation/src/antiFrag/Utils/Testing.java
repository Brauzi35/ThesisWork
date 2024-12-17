package antiFrag.Utils;

import antiFrag.SimulationClientAF;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import domain.Link;
import domain.Mote;
import mapek.FeedbackLoop;
import simulator.QoS;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import static antiFrag.Position.FindPositionAndNeighbour.getPosition;

public class Testing {

    public static void main(String[] args) {
        ArrayList<Point2D> neigh1 = new ArrayList<>();
        ArrayList<Point2D> neigh2 = new ArrayList<>();

        for (int i = 0; i<1; i++){
            Point2D point2D = getPosition();
            neigh1.add(point2D);
            point2D = getPosition();
            neigh2.add(point2D);
        }

        for(int i = 0; i<50; i++){
            NetworksGenerator generator = new NetworksGenerator(neigh1, neigh2);
            ArrayList<SimulationClientAF> sims = generator.networksGenerator(1, 200, SimulationClientAF.Case.CASE1);
            for (SimulationClientAF s : sims){
                System.out.println(s.getSimulator().getMotes());
                for(Mote m : s.getSimulator().getMotes()){
                    for(Link l : m.getLinks()){
                        System.out.println(l.toString());

                    }
                }
                System.out.println(s.getSimulator().getRunInfo().getGlobalInterference().get(0));
                FeedbackLoop feedbackLoop = new FeedbackLoop();

                // get probe and effectors
                Probe probe = s.getProbe();
                Effector effector = s.getEffector();

                // Connect probe and effectors with feedback loop
                feedbackLoop.setProbe(probe);
                feedbackLoop.setEffector(effector);

                // StartFeedback loop
                feedbackLoop.start(new int[]{1, 1, 10});

                ArrayList<QoS> result = s.getNetworkQoS(96);
                //writeQoSToCSV(result, "AnomalyDetectionFiles/qos_"+counter+".csv");
                System.out.println("Run, PacketLoss, EnergyConsumption, NodesExcedingEnergyusage, NodesExcedingQueueSpace, Fairnessindex");
                result.forEach(qos -> System.out.println(qos));

            }
            System.out.println("---------------------------------------------------");
        }


    }

}
