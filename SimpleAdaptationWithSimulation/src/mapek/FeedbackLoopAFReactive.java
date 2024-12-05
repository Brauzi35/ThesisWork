package mapek;

import antiFrag.AnomalyBuilder.AnomalyGeneralizer;
import antiFrag.AnomalyBuilder.BaseNetwork;
import antiFrag.AnomalyDetection.AnomalyDetection;
import antiFrag.LearningAF.RL;
import antiFrag.SimulationClientAF;
import antiFrag.TwinInterrogation;
import antiFrag.Utils.CsvWriter;
import antiFrag.Utils.MovingAverageAF;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.client.SimulationClient;
import deltaiot.services.Link;
import deltaiot.services.LinkSettings;
import deltaiot.services.Mote;
import domain.Gateway;
import domain.Profile;
import simulator.QoS;
import simulator.Simulator;

import java.util.*;

import static antiFrag.AnomalyBuilder.AnomalyBuilder.replicateAnomalyVariation;
import static antiFrag.BetterFeedbackAF.writeQoSToCSV;
import static antiFrag.Utils.FileLister.addNewFolderToConfig;
import static antiFrag.Utils.FileLister.getFileNamesFromDirectory;
import static main.AFAdaptation.getFolders;

public class FeedbackLoopAFReactive {

    Probe probe;
    Effector effector;

    // Knowledge
    SimulationClient networkMgmt;
    SimulationClientAF clientAF;
    ArrayList<Mote> originalMotes;
    boolean firstTime = true;
    ArrayList<Mote> motes;
    List<PlanningStep> steps = new LinkedList<>();

    private int recoveredTimestamp = -1;
    int[] prevConf = {1,1,10};
    AnomalyDetection anomalyDetection;
    boolean alreadyRemoved = false;
    private boolean initRecovery = false;

    MovingAverageAF movingAverageAF;
    public FeedbackLoopAFReactive() {
        anomalyDetection = new AnomalyDetection();
        anomalyDetection.init("AnomalyDetectionFiles");
        movingAverageAF = new MovingAverageAF();
    }

    public FeedbackLoopAFReactive(AnomalyDetection anomalyDetection){

        this.anomalyDetection=anomalyDetection;
        this.movingAverageAF = new MovingAverageAF();
    }

    int round = 0;

    public void setProbe(Probe probe) {
        this.probe = probe;
    }

    public void setEffector(Effector effector) {
        this.effector = effector;
    }
    public void setNetwork(SimulationClient networkMgmt){this.networkMgmt = networkMgmt; }
    public void setNetworkAF(SimulationClientAF networkAF){
        this.clientAF = networkAF;
    }

    public void removeAnomaly(){

        Simulator simul = new Simulator();
        List<domain.Mote> motes = networkMgmt.getSimulator().getMotes();
        List<domain.Mote> modMotes = new ArrayList<>();
        for(domain.Mote m : motes){
            modMotes.add(m);
        }
        int count = 0;
        int excessMotes = modMotes.size() - 14; //the original motes are 14
        int stop = modMotes.size();
        for(int i = 0; i<stop; i++){


            //remove anomaly motes
            if(modMotes.get(i-count).getId() < 2 || modMotes.get(i-count).getId() > 15) {
                modMotes.remove(i-count);
                count++;

            }
            if(count==excessMotes){
                break;
            }
            if(modMotes.get(i).getId() >= 2 && modMotes.get(i).getId() <= 15) {
                List<domain.Link> links = modMotes.get(i).getLinks();
                for (domain.Link l : links) {
                    if (l.getFrom().getId() > 15) {
                        links.remove(l);
                    } else if (l.getTo().getId() > 15) {
                        links.remove(l);
                    }
                }

                modMotes.get(i).setLinks(links);
            }


        }


        domain.Mote[] allMotes = modMotes.toArray(new domain.Mote[0]);


        simul.addMotes(allMotes);

        // Gateway
        Gateway gateway = networkMgmt.getSimulator().getGatewayWithId(1);
        gateway.setView(allMotes);
        simul.addGateways(gateway);

        // turns
        List<Integer> oldorder = networkMgmt.getSimulator().getTurnOrder();
        List<Integer> order = new ArrayList<>();

        for(int o : oldorder){
            if(o>1 && o<16){
                order.add(o);
            }
        }
        Integer[] turnOrderArray = order.toArray(new Integer[0]);
        simul.setTurnOrder(turnOrderArray);

        Profile<Double> interference  = networkMgmt.getSimulator().getRunInfo().getGlobalInterference();
        simul.getRunInfo().setGlobalInterference(interference);


        simul.setQosValues(networkMgmt.getSimulator().getQosValues());
        simul.setRunInfo(networkMgmt.getSimulator().getRunInfo());
        SimulationClient newSc = new SimulationClient(simul);
        this.networkMgmt = newSc; //set new simulation client

    }

    public int getRecoveredTimestamp(){
        return this.recoveredTimestamp;
    }

    public SimulationClient start(int seed) {
        Random r = new Random(seed);

        for (int i = 0; i < 95; i++) {

            int c = r.nextInt(100);

            int limit = 100;
            if(i >10){
                limit = 97;
            }
            if(c>limit && !alreadyRemoved){ //era 98
                recoveredTimestamp = i;


                System.out.println("removing anomaly in run: " +i);
                removeAnomaly();
                //System.out.println("motes post remotion " + networkMgmt.getSimulator().getMotes());
                alreadyRemoved = true;


            }


            if(alreadyRemoved){
                System.out.println("motes post remotion " + networkMgmt.getSimulator().getMotes());
                if(movingAverageAF.isRecovery()) {
                    System.out.println("recovery activated in run: " +i);
                    return networkMgmt;
                }
            }


            monitor();
        }

        return networkMgmt;
    }

    public SimulationClient startRecovery(int seed) {

        for (int i = 0; i < 95-recoveredTimestamp; i++) {
            provv_monitor();
        }

        return networkMgmt;
    }

    void monitor() {
        motes = probe.getAllMotes();
        // perform analysis
        analysis();
    }

    void provv_monitor() {
        motes = probe.getAllMotes();
        provv_analysis();
    }

    void analysis() {
        int[] bestConf = {1,1,10};
        boolean isAnomaly = false;
        //System.out.println("First time " + firstTime);
        if(firstTime){
            originalMotes = motes;

        }
            //get anomaly type

            int timestamp = networkMgmt.getSimulator().getRunInfo().getRunNumber();
            double[] point = new double[9]; // 9 dims
            ArrayList<QoS> qos = networkMgmt.getNetworkQoS(timestamp);

            // from qos
            point[0] = qos.get(timestamp - 1).getPacketLoss();
            point[1] = qos.get(timestamp - 1).getEnergyConsumption();
            point[2] = qos.get(timestamp - 1).getNumNodesEnergy();
            point[3] = qos.get(timestamp - 1).getNumNodesLoss();
            point[4] = qos.get(timestamp - 1).getFairnessIndex();

            // get aggregates values
            double totBattery = 0.0;
            double averagePower = 0.0;
            double totalDistribution = 0.0;
            int linkCount = 0;

            List<domain.Mote> motes = networkMgmt.getSimulator().getMotes();
            for (domain.Mote m : motes) {
                totBattery += m.getBatteryRemaining(); // remaining battery
                List<domain.Link> links = m.getLinks();
                for (domain.Link l : links) {
                    averagePower += l.getPowerNumber();      // power on link
                    totalDistribution += l.getDistribution(); // dist on link
                    linkCount++;
                }
            }

            // from motes and links
            point[5] = totBattery / motes.size();       // avg battery
            point[6] = linkCount > 0 ? averagePower / linkCount : 0.0; // avg power
            point[7] = linkCount > 0 ? totalDistribution / linkCount : 0.0; // avg dist
            point[8] = motes.size();                    // #motes

            timestamp = Math.min(94, timestamp);

            isAnomaly = anomalyDetection.checkForAnomaly(timestamp, point); //is anomaly?
            String path = "JsonRL/CASE1.json";
            if(isAnomaly && firstTime) {
                //should enter here just one time
                List<String> folders = getFolders("SimpleAdaptationWithSimulation/src/antiFrag/AnomalyDetection/config.properties");
                ArrayList<Double> distances = new ArrayList<>();
                for (String f : folders) {
                    AnomalyDetection whichAnomaly = new AnomalyDetection();
                    whichAnomaly.init(f);
                    System.out.println(f);
                    distances.add(whichAnomaly.getDistance(timestamp, point));
                }
                int minIndex = 0;
                double minDist = distances.get(0);
                for(int i = 1; i<distances.size(); i++){

                    if(distances.get(i) < minDist){
                        minDist = distances.get(i);
                        minIndex = i;
                    }
                }
                //System.out.println("folders: "+folders);
                System.out.println("min dist: "+minDist + " distances " + distances );

                List<String> fileNames = getFileNamesFromDirectory("JsonRL");
                System.out.println(fileNames);
                if(minDist < 2000){ //is plausible that this is a known anomaly
                    System.out.println("known anomaly");
                    path = "JsonRL/"+fileNames.get(minIndex+1);
                }else if (minDist < 9000){// is plausible that this is an anomaly similar to a known one
                    System.out.println("unknown anomaly similar to known anomaly");

                    BaseNetwork baseNetwork = new BaseNetwork();
                    AnomalyGeneralizer anomalyGeneralizer = baseNetwork.analyzeUnknownAnomaly(networkMgmt.getSimulator());
                    RL rl = new RL(anomalyGeneralizer);
                    rl.setDimMotes(motes.size());
                    path = "JsonRL/"+fileNames.get(minIndex+1);
                    String newPath = "JsonRL/CASE"+(minIndex+2)+".json";
                    rl.transferLearning("JsonRL/"+fileNames.get(minIndex+1), newPath);

                    //Knowledge portion
                    knowledge(newPath, anomalyGeneralizer);

                }else { //unknown anomaly, cannot find a similar one
                    System.out.println("unknown anomaly");
                    BaseNetwork baseNetwork = new BaseNetwork();
                    AnomalyGeneralizer anomalyGeneralizer = baseNetwork.analyzeUnknownAnomaly(networkMgmt.getSimulator());
                    RL rl = new RL(anomalyGeneralizer);
                    rl.setDimMotes(motes.size());
                    String newPath = "JsonRL/CASE"+(minIndex+2)+".json";
                    rl.fromScratchLearning(newPath);

                    //Knowledge portion
                    knowledge(newPath, anomalyGeneralizer);
                }
                System.out.println("path "+path);
            }

            //anomalyDetection.init("AnomalyDetectionFiles");
            movingAverageAF.update(anomalyDetection.getDistance(timestamp, point));
            if(isAnomaly && !alreadyRemoved){

                //Simulator sim = networkMgmt.getSimulator();
                SimulationClient clientCopy = networkMgmt;
                TwinInterrogation twin = new TwinInterrogation(clientCopy);

                //bestConf = twin.start();
                if(firstTime) {
                    bestConf = twin.startRL(prevConf, path);
                    prevConf = bestConf;
                    firstTime = false;
                }
                System.out.println("round: "+round+" best conf: " + prevConf[0] + ", "+prevConf[1]+ ", "+prevConf[2]);
            }

        // analyze all link settings
        boolean adaptationRequired = analyzeLinkSettings();
        round++;
        // if adaptation required invoke the planner
        if (adaptationRequired) {
            System.out.println("round: "+round);
            planning(prevConf[0], prevConf[1], prevConf[2]);
        }
    }

    void provv_analysis() {
        int[] bestConf = {1,1,10};



        int timestamp = networkMgmt.getSimulator().getRunInfo().getRunNumber();
        if(!alreadyRemoved && timestamp<94){
            return;
        }
        double[] point = new double[9];
        ArrayList<QoS> qos = networkMgmt.getNetworkQoS(timestamp);
        // from qos
        point[0] = qos.get(timestamp - 1).getPacketLoss();
        point[1] = qos.get(timestamp - 1).getEnergyConsumption();
        point[2] = qos.get(timestamp - 1).getNumNodesEnergy();
        point[3] = qos.get(timestamp - 1).getNumNodesLoss();
        point[4] = qos.get(timestamp - 1).getFairnessIndex();


        double totBattery = 0.0;
        double averagePower = 0.0;
        double totalDistribution = 0.0;
        int linkCount = 0;

        List<domain.Mote> motes = networkMgmt.getSimulator().getMotes();
        for (domain.Mote m : motes) {
            totBattery += m.getBatteryRemaining();
            List<domain.Link> links = m.getLinks();
            for (domain.Link l : links) {
                averagePower += l.getPowerNumber();
                totalDistribution += l.getDistribution();
                linkCount++;
            }
        }


        point[5] = totBattery / motes.size();
        point[6] = linkCount > 0 ? averagePower / linkCount : 0.0;
        point[7] = linkCount > 0 ? totalDistribution / linkCount : 0.0;
        point[8] = motes.size();

        timestamp = Math.min(94, timestamp);
        //double distance = anomalyDetection.getDistance(timestamp, point);
        if(!initRecovery) {
            anomalyDetection.init("AnomalyDetectionFiles");
            initRecovery=true;
        }
        boolean isAnomaly = anomalyDetection.checkForAnomaly(timestamp, point);
        if(isAnomaly){
            System.out.println("still recovering");
            SimulationClient clientCopy = networkMgmt;
            TwinInterrogation twin = new TwinInterrogation(clientCopy);

            //bestConf = twin.start();
            bestConf = twin.startRecovery(prevConf);
            prevConf = bestConf;

        }else{
            prevConf = new int[]{1, 1, 10};
            System.out.println("recovered, returning to standard policy");
        }



        planning(prevConf[0], prevConf[1], prevConf[2]);


    }

    boolean analyzeLinkSettings() {
        // analyze all links for possible adaptation options
        for (Mote mote : motes) {
            for (Link link : mote.getLinks()) {
                if (link.getSNR() > 0 && link.getPower() > 0 || link.getSNR() < 0 && link.getPower() < 15) {
                    return true;
                }
            }
            if (mote.getLinks().size() == 2) {
                if (mote.getLinks().get(0).getPower() != mote.getLinks().get(1).getPower())
                    return true;
            }
        }
        return false;
    }

    void knowledge(String path, AnomalyGeneralizer anomalyGeneralizer){
        //update config file
        String newFolder = addNewFolderToConfig("SimpleAdaptationWithSimulation/src/antiFrag/AnomalyDetection/config.properties");
        //get runs information
        for(int i = 0; i<100; i++){
            Simulator simulator = replicateAnomalyVariation(i, anomalyGeneralizer);
            SimulationClient sc = new SimulationClient(simulator, true, newFolder);

            // Create Feedback loop
            FeedbackLoop feedbackLoop = new FeedbackLoop();

            // get probe and effectors
            Probe probe = sc.getProbe();
            Effector effector = sc.getEffector();

            // Connect probe and effectors with feedback loop
            feedbackLoop.setProbe(probe);
            feedbackLoop.setEffector(effector);

            // StartFeedback loop
            feedbackLoop.start(); //Start fa partire il loop

            ArrayList<QoS> result = sc.getNetworkQoS(96);

            writeQoSToCSV(result, newFolder+"/qos_"+i+".csv");
        }
        //we save new json in RL


    }





    void planning(int powAdd, int powSub, int dist) {

        // Go through all links
        boolean powerChanging = false;
        Link left, right;
        for (Mote mote : motes) {

            for (Link link : mote.getLinks()) {
                powerChanging = false;
                if (link.getSNR() > 0 && link.getPower() > 0) {
                    steps.add(new PlanningStep(Step.CHANGE_POWER, link, link.getPower() - powSub));
                    powerChanging = true;
                } else if (link.getSNR() < 0 && link.getPower() < 15) {
                    steps.add(new PlanningStep(Step.CHANGE_POWER, link, link.getPower() + powAdd));
                    powerChanging = true;
                }
            }
            if (mote.getLinks().size() == 2 && powerChanging == false) {
                left = mote.getLinks().get(0);
                right = mote.getLinks().get(1);
                if (left.getPower() != right.getPower()) {
                    // If distribution of all links is 100 then change it to 50
                    // 50
                    if (left.getDistribution() == 100 && right.getDistribution() == 100) {
                        left.setDistribution(50);
                        right.setDistribution(50);
                    }
                    if (left.getPower() > right.getPower() && left.getDistribution() < 100) {
                        steps.add(new PlanningStep(Step.CHANGE_DIST, left, left.getDistribution() + dist));
                        steps.add(new PlanningStep(Step.CHANGE_DIST, right, right.getDistribution() - dist));
                    } else if (right.getDistribution() < 100) {
                        steps.add(new PlanningStep(Step.CHANGE_DIST, right, right.getDistribution() + dist));
                        steps.add(new PlanningStep(Step.CHANGE_DIST, left, left.getDistribution() - dist));
                    }
                }
            }
        }

        if (steps.size() > 0) {
            execution();
        }
    }

    void std_planning(){
        // Go through all links
        boolean powerChanging = false;
        Link left, right;
        for (Mote mote : motes) {
            for (Link link : mote.getLinks()) {
                powerChanging = false;
                if (link.getSNR() > 0 && link.getPower() > 0) {
                    steps.add(new PlanningStep(Step.CHANGE_POWER, link, link.getPower() - 1));
                    powerChanging = true;
                } else if (link.getSNR() < 0 && link.getPower() < 15) {
                    steps.add(new PlanningStep(Step.CHANGE_POWER, link, link.getPower() + 1));
                    powerChanging = true;
                }
            }
            if (mote.getLinks().size() == 2 && powerChanging == false) {
                left = mote.getLinks().get(0);
                right = mote.getLinks().get(1);
                if (left.getPower() != right.getPower()) {
                    // If distribution of all links is 100 then change it to 50
                    // 50
                    if (left.getDistribution() == 100 && right.getDistribution() == 100) {
                        left.setDistribution(50);
                        right.setDistribution(50);
                    }
                    if (left.getPower() > right.getPower() && left.getDistribution() < 100) {
                        steps.add(new PlanningStep(Step.CHANGE_DIST, left, left.getDistribution() + 10));
                        steps.add(new PlanningStep(Step.CHANGE_DIST, right, right.getDistribution() - 10));
                    } else if (right.getDistribution() < 100) {
                        steps.add(new PlanningStep(Step.CHANGE_DIST, right, right.getDistribution() + 10));
                        steps.add(new PlanningStep(Step.CHANGE_DIST, left, left.getDistribution() - 10));
                    }
                }
            }
        }

        if (steps.size() > 0) {
            execution();
        }
    }

    void execution() {
        boolean addMote;
        List<Mote> motesEffected = new LinkedList<Mote>();
        for (Mote mote : motes) {
            addMote = false;
            for (PlanningStep step : steps) {
                if (step.link.getSource() == mote.getMoteid()) {
                    addMote = true;
                    if (step.step == Step.CHANGE_POWER) {
                        mote.getLinkWithDest(step.link.getDest()).setPower(step.value);
                    } else if (step.step == Step.CHANGE_DIST) {
                        mote.getLinkWithDest(step.link.getDest()).setDistribution(step.value);
                    }
                }
            }
            motesEffected.add(mote);
        }
        List<LinkSettings> newSettings;

        for(Mote mote: motesEffected){
            newSettings = new LinkedList<LinkSettings>();
            for(Link link: mote.getLinks()){
                newSettings.add(new LinkSettings(mote.getMoteid(), link.getDest(), link.getPower(), link.getDistribution(), link.getSF()));
            }
            effector.setMoteSettings(mote.getMoteid(), newSettings);
        }
        steps.clear();
    }



    Mote findMote(int source, int destination) {
        for (Mote mote : motes) {
            if (mote.getMoteid() == source) {
                return mote;
            }
        }
        return null;
    }

}
