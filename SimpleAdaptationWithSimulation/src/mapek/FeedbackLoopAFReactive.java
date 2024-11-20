package mapek;

import antiFrag.AnomalyDetection.AnomalyDetection;
import antiFrag.SimulationClientAF;
import antiFrag.TwinInterrogation;
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
    int[][] prevConf = {{1, 1, 1,1,1,1,1,1,1,1,1,1,1,1,1},{1, 1, 1,1,1,1,1,1,1,1,1,1,1,1,1},{10, 10, 10,10,10,10,10,10,10,10,10,10,10,10,10}};

    AnomalyDetection anomalyDetection;
    boolean alreadyRemoved = false;

    MovingAverageAF movingAverageAF = new MovingAverageAF();
    public FeedbackLoopAFReactive() {
        anomalyDetection = new AnomalyDetection();
        anomalyDetection.init();
    }

    public FeedbackLoopAFReactive(AnomalyDetection anomalyDetection){
        this.anomalyDetection=anomalyDetection;
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

    public void removeAnomaly(){ //TODO per ora rimuove solo anomalia 1, dovrebbe essere agnostico all'anomalia

        Simulator simul = new Simulator();
        List<domain.Mote> motes = networkMgmt.getSimulator().getMotes();
        List<domain.Mote> modMotes = new ArrayList<>();
        for(domain.Mote m : motes){
            modMotes.add(m);
        }
        for(int i = 0; i<modMotes.size(); i++){


            //remove anomaly motes
            if(modMotes.get(i).getId() < 2 || modMotes.get(i).getId() > 15) {
                modMotes.remove(i);
                break;
            }
            List<domain.Link> links = modMotes.get(i).getLinks();
            for(domain.Link l : links){
                if(l.getFrom().getId() > 15){
                    links.remove(l);
                } else if (l.getTo().getId() > 15) {
                    links.remove(l);
                }
            }

            modMotes.get(i).setLinks(links);


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


                System.out.println("togliamo anomalia alla run: " +i);
                removeAnomaly();
                alreadyRemoved = true;


            }


            if(alreadyRemoved){
                if(movingAverageAF.isRecovery()) {
                    System.out.println("recovery trovata alla run: " +i);
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
        int[][] bestConf = {{1, 1, 1,1,1,1,1,1,1,1,1,1,1,1,1},{1, 1, 1,1,1,1,1,1,1,1,1,1,1,1,1},{10, 10, 10,10,10,10,10,10,10,10,10,10,10,10,10}}; //prima era lungo 2, da quando ho messo rl ho introdotto 3 gradi di libertà
        boolean isAnomaly = false;
        if(firstTime){
            originalMotes = motes; //TODO what if the network starts in an unfamiliar setup?
            firstTime = false;
        } else{
            //if it's not the first analysis iteration
            //we should check if the network has changed its topology (and more)

            int timestamp = networkMgmt.getSimulator().getRunInfo().getRunNumber();
            double[] point = new double[9]; // Punto con 9 dimensioni
            ArrayList<QoS> qos = networkMgmt.getNetworkQoS(timestamp);

            // Dati da qos
            point[0] = qos.get(timestamp - 1).getPacketLoss();
            point[1] = qos.get(timestamp - 1).getEnergyConsumption();
            point[2] = qos.get(timestamp - 1).getNumNodesEnergy();
            point[3] = qos.get(timestamp - 1).getNumNodesLoss();
            point[4] = qos.get(timestamp - 1).getFairnessIndex();

            // Calcolo dei dati aggregati dai nodi e dai link
            double totBattery = 0.0;
            double averagePower = 0.0;
            double totalDistribution = 0.0;
            int linkCount = 0;

            List<domain.Mote> motes = networkMgmt.getSimulator().getMotes();
            for (domain.Mote m : motes) {
                totBattery += m.getBatteryRemaining(); // Batteria rimanente
                List<domain.Link> links = m.getLinks();
                for (domain.Link l : links) {
                    averagePower += l.getPowerNumber();      // Potenza sul link
                    totalDistribution += l.getDistribution(); // Distribuzione sul link
                    linkCount++;
                }
            }

            // Dati calcolati dai nodi e dai link
            point[5] = totBattery / motes.size();       // Batteria media dei nodi
            point[6] = linkCount > 0 ? averagePower / linkCount : 0.0; // Potenza media sui link
            point[7] = linkCount > 0 ? totalDistribution / linkCount : 0.0; // Distribuzione media sui link
            point[8] = motes.size();                    // Numero totale di nodi

            timestamp = Math.min(94, timestamp); // Limita il timestamp massimo a 96

            isAnomaly = anomalyDetection.checkForAnomaly(timestamp, point);
            movingAverageAF.update(anomalyDetection.getDistance(timestamp, point));
            if(isAnomaly && !alreadyRemoved){//!(originalMotes.size()==motes.size())){ //TODO implementare un modo vero per accorgermi di un problema
                //simulate possible scenarios and take the correct choice
                //Simulator sim = networkMgmt.getSimulator();
                SimulationClient clientCopy = networkMgmt; //TODO probabilmente inutile in quanto shallow copy
                TwinInterrogation twin = new TwinInterrogation(clientCopy);

                //bestConf = twin.start();
                bestConf = twin.startRL(prevConf);
                System.out.println("round: "+round+"sarebbe meglio usare questa conf; " + bestConf[0] + ", "+bestConf[1]+ ", "+bestConf[2]); //TODO rendere dinamico, deve funzionare in funzione dei gradi di libertà, serve file conf
            }
        }
        // analyze all link settings
        boolean adaptationRequired = analyzeLinkSettings();
        round++;
        // if adaptation required invoke the planner
        if (adaptationRequired && round%10 == 0) {
            System.out.println("round: "+round);
            if(!alreadyRemoved) {

                System.out.println("better planning");
                planning(bestConf[0], bestConf[1], bestConf[2]);
                //prevConf = bestConf;
            }else {
                System.err.println("non dovrebbe entrare mai qui");
                std_planning(); //default settings
            }
        }
    }

    void provv_analysis() {
        int[][] bestConf = {{1, 1, 1,1,1,1,1,1,1,1,1,1,1,1,1},{1, 1, 1,1,1,1,1,1,1,1,1,1,1,1,1},{10, 10, 10,10,10,10,10,10,10,10,10,10,10,10,10}}; //prima era lungo 2, da quando ho messo rl ho introdotto 3 gradi di libertà
        if(firstTime){
            SimulationClient clientCopy = networkMgmt; //TODO probabilmente inutile in quanto shallow copy
            TwinInterrogation twin = new TwinInterrogation(clientCopy);

            //bestConf = twin.start();
            bestConf = twin.startRecovery(prevConf);
            prevConf = bestConf;




            firstTime = false;
        }
        /*
        int timestamp = networkMgmt.getSimulator().getRunInfo().getRunNumber();
        double[] point = new double[5]; //common dimension
        ArrayList<QoS> qos = networkMgmt.getNetworkQoS(timestamp);
        point[0] = qos.get(timestamp-1).getPacketLoss();
        point[1] = qos.get(timestamp-1).getEnergyConsumption();
        //total battery/mote_count
        double tot_battery = 0.0;
        double averagePower = 0.0;
        double totalDistribution = 0.0;
        int linkCount = 0;

        List<domain.Mote> motes = networkMgmt.getSimulator().getMotes();
        for(domain.Mote m : motes){
            tot_battery += m.getBatteryRemaining();
            List<domain.Link> links = m.getLinks();
            for(domain.Link l : links){
                averagePower += l.getPowerNumber();
                linkCount++;
                totalDistribution += l.getDistribution();
            }
        }
        point[2] = tot_battery/motes.size();
        point[3] = averagePower / linkCount;
        point[4] = totalDistribution / linkCount;
        timestamp = Math.max(timestamp, 95);
        System.out.println("distanza: "+anomalyDetection.getDistance(timestamp, point));


         */
        //if it's not the first analysis iteration
        //we should check if the network has changed its topology (and more)
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



    void planning(int[] powAdd, int[] powSub, int[] dist) {

        // Go through all links
        boolean powerChanging = false;
        Link left, right;
        for (Mote mote : motes) {

            for (Link link : mote.getLinks()) {
                powerChanging = false;
                if (link.getSNR() > 0 && link.getPower() > 0) {
                    steps.add(new PlanningStep(Step.CHANGE_POWER, link, link.getPower() - powSub[mote.getMoteid()-2]));
                    powerChanging = true;
                } else if (link.getSNR() < 0 && link.getPower() < 15) {
                    steps.add(new PlanningStep(Step.CHANGE_POWER, link, link.getPower() + powAdd[mote.getMoteid()-2]));
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
                        steps.add(new PlanningStep(Step.CHANGE_DIST, left, left.getDistribution() + dist[mote.getMoteid()-2]));
                        steps.add(new PlanningStep(Step.CHANGE_DIST, right, right.getDistribution() - dist[mote.getMoteid()-2]));
                    } else if (right.getDistribution() < 100) {
                        steps.add(new PlanningStep(Step.CHANGE_DIST, right, right.getDistribution() + dist[mote.getMoteid()-2]));
                        steps.add(new PlanningStep(Step.CHANGE_DIST, left, left.getDistribution() - dist[mote.getMoteid()-2]));
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
