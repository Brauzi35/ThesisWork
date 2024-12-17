package mapek;

import antiFrag.SimulationClientAF;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.client.SimulationClient;
import deltaiot.services.Link;
import deltaiot.services.LinkSettings;
import deltaiot.services.Mote;
import domain.Gateway;
import domain.Profile;
import simulator.Simulator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static antiFrag.TwinInterrogation.buildCloneNetwork;

public class FeedbackLoopOracle {

    Probe probe;
    Effector effector;
    boolean alreadyRemoved = false;

    int[] stdConf = {1,1,10};


    // Knowledge
    SimulationClient networkMgmt;
    SimulationClientAF clientAF;
    ArrayList<Mote> motes;
    List<PlanningStep> steps = new LinkedList<>();

    int round = 0;
    private int recoveredTimestamp = -1;

    public int getRecoveredTimestamp() {
        return recoveredTimestamp;
    }

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

    private void removeAnomaly(){


        //just create delta iot simulator, so that we simulate a perfect recovery

        SimulationClient newSc = new SimulationClient();
        this.networkMgmt = newSc; //set new simulation client

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
                System.out.println("anomaly removed in run: " +i);
                removeAnomaly();
                alreadyRemoved = true;
                return networkMgmt;

            }
            monitor();
        }
        return networkMgmt;
    }

    void monitor() {
        motes = probe.getAllMotes();

        // perform analysis
        analysis();
    }

    void monitorVal(int[] conf) {
        motes = probe.getAllMotes();

        // perform analysis
        analysisVal(conf);
    }

    void analysis() {
        int pwrsAdd = 1;
        int pwrsSub = 1;
        int dists = 10;

        int[] bestConf = {1,1,10};
        int[] possiblePwrs = {0,1,2,3,4,5};
        int[] possibleDists = {0,10,20,30,40,50};

        //find the very best configuration
        for(int add : possiblePwrs){
            for(int sub : possiblePwrs){
                for(int dist : possibleDists){
                    bestConf = new int[]{add, sub, dist};
                    Simulator sim = buildCloneNetwork(networkMgmt);
                    monitorVal(bestConf);
                }

            }
        }


        // analyze all link settings
        boolean adaptationRequired = analyzeLinkSettings();
        // if adaptation required invoke the planner
        if (adaptationRequired) {
            if(alreadyRemoved){
                planning(stdConf[0],stdConf[1],stdConf[2]);
            }else {
                planning(pwrsAdd, pwrsSub, dists);
            }
        }
    }

    public void analysisVal(int[] conf){
        // analyze all link settings
        boolean adaptationRequired = analyzeLinkSettings();

        // if adaptation required invoke the planner
        if (adaptationRequired) {
            planning(conf[0], conf[1], conf[2]);
        }
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

    void planning(int pwrsAdd, int pwrsSub, int dists) {

        // Go through all links
        boolean powerChanging = false;
        Link left, right;
        for (Mote mote : motes) {
            for (Link link : mote.getLinks()) {
                powerChanging = false;
                if (link.getSNR() > 0 && link.getPower() > 0) {
                    steps.add(new PlanningStep(Step.CHANGE_POWER, link, link.getPower() - pwrsSub));
                    powerChanging = true;
                } else if (link.getSNR() < 0 && link.getPower() < 15) {
                    steps.add(new PlanningStep(Step.CHANGE_POWER, link, link.getPower() + pwrsAdd));
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
                        steps.add(new PlanningStep(Step.CHANGE_DIST, left, left.getDistribution() + dists));
                        steps.add(new PlanningStep(Step.CHANGE_DIST, right, right.getDistribution() - dists));
                    } else if (right.getDistribution() < 100) {
                        steps.add(new PlanningStep(Step.CHANGE_DIST, right, right.getDistribution() + dists));
                        steps.add(new PlanningStep(Step.CHANGE_DIST, left, left.getDistribution() - dists));
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
