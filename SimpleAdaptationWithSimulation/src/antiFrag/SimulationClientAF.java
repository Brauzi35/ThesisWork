package antiFrag;

import deltaiot.DeltaIoTSimulator;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.services.LinkSettings;
import deltaiot.services.Mote;
import domain.Link;
import domain.Node;
import simulator.QoS;
import simulator.Simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static deltaiot.client.SimulationClient.printToponomy;

public class SimulationClientAF implements Probe, Effector {

    Random random = new Random();
    /*
    my own implementation of simulation client;
    Simulator is to be intended as the real system, this serves us to force custom anomalies
     */
    public enum Case{
        DEFAULT,
        CASE1,
        UNKNOWN
    }

    private Simulator simulator;
    private double delta = 0.0;
    public SimulationClientAF(Case c, int x[], int y[], double battery, int load, int neighId[]){
        switch (c){
            case DEFAULT: this.simulator = NetworkSimulatorAF.createSimulatorForAF();
            break;
            case CASE1: this.simulator = NetworkSimulatorAF.createSimulatorCase1(x[0], y[0], battery, load, neighId[0], delta);
            break;
            case UNKNOWN: this.simulator = NetworkSimulatorAF.createSimulatorUnknown(x, y, battery, load, neighId, delta);
            break;
            default: this.simulator = NetworkSimulatorAF.createSimulatorForAF();
        }

    }

    public SimulationClientAF(Case c, int x[], int y[], double battery, int load, int neighId[], double delta){
        switch (c){
            case DEFAULT: this.simulator = NetworkSimulatorAF.createSimulatorForAF();
                break;
            case CASE1: this.simulator = NetworkSimulatorAF.createSimulatorCase1(x[0], y[0], battery, load, neighId[0], delta);
                break;
            case UNKNOWN: this.simulator = NetworkSimulatorAF.createSimulatorUnknown(x, y, battery, load, neighId, delta);
                break;
            default: this.simulator = NetworkSimulatorAF.createSimulatorForAF();
        }

    }




    public SimulationClientAF(Case c){

        this.simulator = NetworkSimulatorAF.createSimulatorForAF();


    }
    public SimulationClientAF(Simulator simulator){
        this.simulator = simulator;
    }

    @Override
    public ArrayList<Mote> getAllMotes() {
        simulator.doSingleRun();
        //String string = "AnomalyDetectionFiles1/state"+simulator+".txt";
        //printToponomy(simulator, string); //added myself

        List<domain.Mote> motes = simulator.getMotes();
        ArrayList<Mote> afMotes = new ArrayList<>();
        for (domain.Mote mote : motes) {
            afMotes.add(DeltaIoTSimulator.getAfMote(mote, simulator));
        }
        return afMotes;
    }

    private domain.Mote getMote(int moteId) {
        return simulator.getMoteWithId(moteId);
    }

    private Link getLink(int src, int dest) {
        domain.Mote from = simulator.getMoteWithId(src);
        Node to = simulator.getNodeWithId(dest);
        Link link = from.getLinkTo(to);
        return link;
    }

    @Override
    public double getMoteEnergyLevel(int moteId) {
        return getMote(moteId).getBatteryRemaining();
    }

    @Override
    public double getMoteTrafficLoad(int moteId) {
        return getMote(moteId).getActivationProbability().get(simulator.getRunInfo().getRunNumber());
    }

    @Override
    public int getLinkPowerSetting(int src, int dest) {
        return getLink(src, dest).getPowerNumber();
    }

    @Override
    public int getLinkSpreadingFactor(int src, int dest) {
        return getLink(src, dest).getSfTimeNumber();
    }

    @Override
    public double getLinkSignalNoise(int src, int dest) {
        return getLink(src, dest).getSRN(simulator.getRunInfo());
    }

    @Override
    public double getLinkDistributionFactor(int src, int dest) {
        return getLink(src, dest).getDistribution();
    }



    @Override
    public void setMoteSettings(int moteId, List<LinkSettings> linkSettings) {
        domain.Mote mote = getMote(moteId);
        Node node;
        Link link;
        for(LinkSettings setting: linkSettings){
            node = simulator.getNodeWithId(setting.getDest());
            link = mote.getLinkTo(node);
            link.setPowerNumber(setting.getPowerSettings());
            link.setDistribution(setting.getDistributionFactor());
            link.setSfTimeNumber(setting.getSpreadingFactor());
        }
    }

    @Override
    public void setDefaultConfiguration() {
        List<domain.Mote> motes = simulator.getMotes();
        for (domain.Mote mote : motes) {
            for (Link link : mote.getLinks()) {
                link.setDistribution(100);
                link.setPowerNumber(15);
                link.setSfTimeNumber(11);
            }
        }

    }

    @Override
    public ArrayList<QoS> getNetworkQoS(int period) {
        List<QoS> qosOrigList = simulator.getQosValues();
        int qosSize = qosOrigList.size();

        if (period >= qosSize)
            return (ArrayList<QoS>) qosOrigList;

        int startIndex = qosSize - period;

        ArrayList<QoS> newList = new ArrayList<QoS>();

        for(int i = startIndex; i < qosSize; i++){
            newList.add(qosOrigList.get(i));
        }
        return newList;
    }

    public Probe getProbe() {
        return this;
    }

    public Effector getEffector() {
        return this;
    }

    public Simulator getSimulator() {
        return this.simulator;
    }






}
