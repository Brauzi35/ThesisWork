package antiFrag.Utils;

import antiFrag.SimulationClientAF;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import static antiFrag.NetworkSimulatorAF.createSimulatorCase1noProbs;
import static antiFrag.NetworkSimulatorAF.createSimulatorCase2noProbs;
import static antiFrag.Position.FindPositionAndNeighbour.findClosestNode;
import static antiFrag.Position.FindPositionAndNeighbour.getPosition;

public class NetworksGenerator {


    private ArrayList<Point2D> neigh1;
    private ArrayList<Point2D> neigh2;

    public NetworksGenerator(ArrayList<Point2D> neigh1, ArrayList<Point2D> neigh2) {
        this.neigh1 = neigh1;
        this.neigh2 = neigh2;
    }



    public ArrayList<SimulationClientAF> networksGenerator(int differentnetworks, int load, SimulationClientAF.Case c){
        ArrayList<SimulationClientAF> simulators = new ArrayList<>();

        for(int k = 0; k< differentnetworks; k++){
            /*
            Point2D point2D = getPosition();

            int neigh = findClosestNode(point2D);
            double delta = 0;//random2.nextInt(1001);
            int [] neigh_arr = {neigh, 0};

             */
            double delta = 0;
            int neigh = findClosestNode(neigh1.get(k));
            int [] neigh_arr = {neigh, 0};
            int[] x = {(int)neigh1.get(k).getX(),0};
            int[] y = {(int)neigh1.get(k).getY(),0};

            /*
            point2D = getPosition();
            neigh = findClosestNode(point2D);
            neigh_arr[1] = neigh;

             */
            //neighs.add(neigh_arr);
            neigh = findClosestNode(neigh1.get(k));
            neigh_arr[1] = neigh;
            x[1] = (int) neigh2.get(k).getX();
            y[1] = (int) neigh2.get(k).getY();

            //SimulationClientAF sc = new SimulationClientAF(c, x, y, 118800.0, load, neigh_arr, delta);
            //SimulationClientAF sc = new SimulationClientAF(SimulationClientAF.Case.UNKNOWN, x, y, 118800.0, load, neigh_arr, delta);
            //SimulationClientAF sc = new SimulationClientAF(createSimulatorCase1noProbs(x[0], y[0], 118800.0, load, neigh_arr[0], delta));
            SimulationClientAF sc = new SimulationClientAF(createSimulatorCase2noProbs(x, y, 118800.0, load, neigh_arr, delta));
            simulators.add(sc);
        }
        return simulators;
    }



}
