package mapek;

import antiFrag.AnomalyDetection.AnomalyDetection;
import antiFrag.SimulationClientAF;
import antiFrag.TwinInterrogation;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.client.SimulationClient;
import deltaiot.services.Link;
import deltaiot.services.LinkSettings;
import deltaiot.services.Mote;
import simulator.QoS;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FeedbackLoopRL {

    Probe probe;
    Effector effector;

    // Knowledge
    SimulationClient networkMgmt;
    SimulationClientAF clientAF;
    ArrayList<Mote> originalMotes;
    boolean firstTime = true;
    ArrayList<Mote> motes;
    List<PlanningStep> steps = new LinkedList<>();

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

    public void start(int pwrsAdd, int pwrsSub, int dists) {
        for (int i = 0; i < 95; i++) {
            monitor(pwrsAdd, pwrsSub,dists);
        }
    }

    void monitor(int pwrsAdd, int pwrsSub, int dists) {
        motes = probe.getAllMotes();

        // perform analysis
        analysis(pwrsAdd, pwrsSub,dists);
    }

    void analysis(int pwrsAdd, int pwrsSub, int dists) {
        int[] bestConf = {0,0};
        if(firstTime){
            originalMotes = motes; //TODO what if the network starts in an unfamiliar setup?
            firstTime = false;
        } else{
            //if it's not the first analysis iteration
            //we should check if the network has changed its topology (and more)
            /*
            AnomalyDetection anomalyDetection = new AnomalyDetection();
            anomalyDetection.init();
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

             */
            //if(anomalyDetection.checkForAnomaly(timestamp, point)){//!(originalMotes.size()==motes.size())){
                //simulate possible scenarios and take the correct choice
                //Simulator sim = networkMgmt.getSimulator();
                //SimulationClient clientCopy = networkMgmt;
                //TwinInterrogation twin = new TwinInterrogation(clientCopy);

                //bestConf = twin.start();
                //System.out.println("sarebbe meglio usare questa conf" + bestConf[0] + bestConf[1]); //TODO ok riesco ad ottenerle: ogni quanto le voglio? che ci faccio?
            //}
        }
        // analyze all link settings
        boolean adaptationRequired = analyzeLinkSettings();
        round++;
        // if adaptation required invoke the planner
        if (adaptationRequired) {
            //if(round%5==0) {
               // System.out.println("better planning");
                planning(pwrsAdd, pwrsSub,dists);
           // }else {
               // planning(0,0);
            //}
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
