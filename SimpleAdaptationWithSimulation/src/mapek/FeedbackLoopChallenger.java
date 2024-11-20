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

public class FeedbackLoopChallenger {

    Probe probe;
    Effector effector;
    boolean alreadyRemoved = false;

    int[][] stdConf = {{1, 1, 1,1,1,1,1,1,1,1,1,1,1,1,1},{1, 1, 1,1,1,1,1,1,1,1,1,1,1,1,1},{10, 10, 10,10,10,10,10,10,10,10,10,10,10,10,10}}; //prima era lungo 2, da quando ho messo rl ho introdotto 3 gradi di libertà


    // Knowledge
    SimulationClient networkMgmt;
    SimulationClientAF clientAF;
    ArrayList<Mote> originalMotes;
    boolean firstTime = true;
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

    private void removeAnomaly(){ //TODO per ora rimuove solo anomalia 1, dovrebbe essere agnostico all'anomalia

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

    public SimulationClient start(int[] pwrsAdd, int[] pwrsSub, int[] dists, int seed) {
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
                return networkMgmt;

            }
            monitor(pwrsAdd, pwrsSub,dists);
        }
        return networkMgmt;
    }

    void monitor(int[] pwrsAdd, int[] pwrsSub, int[] dists) {
        motes = probe.getAllMotes();

        // perform analysis
        analysis(pwrsAdd, pwrsSub,dists);
    }

    void analysis(int[] pwrsAdd, int[] pwrsSub, int[] dists) {

        if(firstTime){
            originalMotes = motes;
            firstTime = false;
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

    void planning(int[] pwrsAdd, int[] pwrsSub, int[] dists) {

        // Go through all links
        boolean powerChanging = false;
        Link left, right;
        for (Mote mote : motes) {
            for (Link link : mote.getLinks()) {
                powerChanging = false;
                if (link.getSNR() > 0 && link.getPower() > 0) {
                    steps.add(new PlanningStep(Step.CHANGE_POWER, link, link.getPower() - pwrsSub[mote.getMoteid()-2]));
                    powerChanging = true;
                } else if (link.getSNR() < 0 && link.getPower() < 15) {
                    steps.add(new PlanningStep(Step.CHANGE_POWER, link, link.getPower() + pwrsAdd[mote.getMoteid()-2]));
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
                        steps.add(new PlanningStep(Step.CHANGE_DIST, left, left.getDistribution() + dists[mote.getMoteid()-2]));
                        steps.add(new PlanningStep(Step.CHANGE_DIST, right, right.getDistribution() - dists[mote.getMoteid()-2]));
                    } else if (right.getDistribution() < 100) {
                        steps.add(new PlanningStep(Step.CHANGE_DIST, right, right.getDistribution() + dists[mote.getMoteid()-2]));
                        steps.add(new PlanningStep(Step.CHANGE_DIST, left, left.getDistribution() - dists[mote.getMoteid()-2]));
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
