package main;

import antiFrag.AnomalyDetection.AnomalyDetection;
import antiFrag.SimulationClientAF;
import antiFrag.Utils.CsvWriter;
import antiFrag.Utils.WealthScore;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.client.SimulationClient;
import mapek.FeedbackLoopAFReactive;
import simulator.QoS;
import simulator.Simulator;

import java.awt.geom.Point2D;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static antiFrag.BetterFeedbackAF.writeMotesToCsv;
import static antiFrag.BetterFeedbackAF.writeQoSToCSV;
import static antiFrag.Position.FindPositionAndNeighbour.findClosestNode;
import static antiFrag.Position.FindPositionAndNeighbour.getPosition;
import static antiFrag.Utils.CsvWriter.writeOrUpdateScoreToCSV;

public class AFAdaptation {
    SimulationClient networkMgmt;
    CsvWriter csvWriter = new CsvWriter();
    static AnomalyDetection anomalyDetection;
    SimulationClient simulationClientRecovery;
    public void start(int i){

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

        //System.out.println("neigh: " + Arrays.toString(neigh_arr));
        SimulationClientAF sc = new SimulationClientAF(SimulationClientAF.Case.CASE1, x, y, 118800.0, 200, neigh_arr);
        //SimulationClientAF sc = new SimulationClientAF(SimulationClientAF.Case.UNKNOWN, x, y, 118800.0, 200, neigh_arr);
        // Create a simulation client object
        networkMgmt = new SimulationClient(sc.getSimulator());
        //System.out.println(networkMgmt.getSimulator().getMoteWithId(16).getLinks());
        //System.out.println(networkMgmt.getSimulator().getMoteWithId(17).getLinks());

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
        csvWriter.writeQoSToCSV(finalResult, "BetterPolicy/simulation"+i+"_neigh" + Arrays.toString(neigh_arr) + "_stopAnomaly"+ stopIdx+".csv");

        WealthScore wealthScore = new WealthScore();
        double score = wealthScore.calculateScore(finalResult);
        writeOrUpdateScoreToCSV(i, score, "antifrag.csv");

        //writeQoSToCSV(result, "AnomalyDetectionFiles1/qos_"+i+".csv");
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
        anomalyDetection.init("AnomalyDetectionFiles");
    }

    public static List<String> getFolders(String configFilePath) {
        List<String> folders = new ArrayList<>();
        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            properties.load(fis);
            String folderList = properties.getProperty("folders");
            if (folderList != null) {
                folders.addAll(Arrays.asList(folderList.split(",")));
            }
        } catch (IOException e) {
            System.err.println("Error reading configuration file: " + e.getMessage());
        }

        return folders;
    }
}
