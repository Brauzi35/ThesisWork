package antiFrag.AnomalyBuilder;

import deltaiot.client.SimulationClient;
import domain.Gateway;
import domain.Link;
import domain.Mote;
import domain.Position;
import simulator.Simulator;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


public class BaseNetwork {
    final Lock lock = new ReentrantLock();
    final Condition adaptationCompleted = lock.newCondition();
    final static int GATEWAY_ID = 1;
    final static int PORT = 9888;

    public int load = 10;
    public double battery = 11880.0;
    public double posScale = 2;
    public Mote[] getBaseMotes(){
        // Motes

        Mote mote2  = new Mote(2 , battery, load, new Position(378 * posScale, 193 * posScale));
        Mote mote3  = new Mote(3 , battery, load, new Position(365 * posScale, 343 * posScale));
        Mote mote4  = new Mote(4 , battery, load, new Position(508 * posScale, 296 * posScale));
        Mote mote5  = new Mote(5 , battery, load, new Position(603 * posScale, 440 * posScale));
        Mote mote6  = new Mote(6 , battery, load, new Position(628 * posScale, 309 * posScale));
        Mote mote7  = new Mote(7 , battery, load, new Position(324 * posScale, 273 * posScale));
        Mote mote8  = new Mote(8 , battery, load, new Position(392 * posScale, 478 * posScale));
        Mote mote9  = new Mote(9 , battery, load, new Position(540 * posScale, 479 * posScale));
        Mote mote10 = new Mote(10, battery, load, new Position(694 * posScale, 356 * posScale));
        Mote mote11 = new Mote(11, battery, load, new Position(234 * posScale, 232 * posScale));
        Mote mote12 = new Mote(12, battery, load, new Position(221 * posScale, 322 * posScale));
        Mote mote13 = new Mote(13, battery, load, new Position(142 * posScale, 170 * posScale));
        Mote mote14 = new Mote(14, battery, load, new Position(139 * posScale, 293 * posScale));
        Mote mote15 = new Mote(15, battery, load, new Position(128 * posScale, 344 * posScale));

        return new Mote[]{mote2, mote3, mote4, mote5, mote6, mote7, mote8, mote9, mote10, mote11, mote12, mote13, mote14, mote15};
    }

    public List<Mote> getBaseMotesList(){
        // Motes

        Mote mote2  = new Mote(2 , battery, load, new Position(378 * posScale, 193 * posScale));
        Mote mote3  = new Mote(3 , battery, load, new Position(365 * posScale, 343 * posScale));
        Mote mote4  = new Mote(4 , battery, load, new Position(508 * posScale, 296 * posScale));
        Mote mote5  = new Mote(5 , battery, load, new Position(603 * posScale, 440 * posScale));
        Mote mote6  = new Mote(6 , battery, load, new Position(628 * posScale, 309 * posScale));
        Mote mote7  = new Mote(7 , battery, load, new Position(324 * posScale, 273 * posScale));
        Mote mote8  = new Mote(8 , battery, load, new Position(392 * posScale, 478 * posScale));
        Mote mote9  = new Mote(9 , battery, load, new Position(540 * posScale, 479 * posScale));
        Mote mote10 = new Mote(10, battery, load, new Position(694 * posScale, 356 * posScale));
        Mote mote11 = new Mote(11, battery, load, new Position(234 * posScale, 232 * posScale));
        Mote mote12 = new Mote(12, battery, load, new Position(221 * posScale, 322 * posScale));
        Mote mote13 = new Mote(13, battery, load, new Position(142 * posScale, 170 * posScale));
        Mote mote14 = new Mote(14, battery, load, new Position(139 * posScale, 293 * posScale));
        Mote mote15 = new Mote(15, battery, load, new Position(128 * posScale, 344 * posScale));

        List<Mote> ret = new ArrayList<>();
        Mote[] motes = {mote2, mote3, mote4, mote5, mote6, mote7, mote8, mote9, mote10, mote11, mote12, mote13, mote14, mote15};
        for(Mote m : motes){
            ret.add(m);
        }
        return ret;
    }

    public Gateway getBaseGateway(){

        // Gateway
        Gateway gateway = new Gateway(GATEWAY_ID, new Position(482 * posScale, 360 * posScale));
        gateway.setView(this.getBaseMotes());
        return gateway;
    }

    public SimulationClient getBaseSimulationClient(){
        return new SimulationClient();
    }

    public AnomalyGeneralizer analyzeUnknownAnomaly(Simulator s) {
        // Motes added, original motes not present, links added, original links not present, positions original motes, valori motes
        AnomalyGeneralizer anomalyGeneralizer = new AnomalyGeneralizer();
        List<Mote> currentMotes = s.getMotes();
        Mote[] originalMotes = this.getBaseMotes();
        Simulator originalNetwork = this.getBaseSimulationClient().getSimulator();

        // let's use a list
        List<Mote> originalMoteList = Arrays.asList(originalMotes);

        // get ids for both mote lists
        Map<Integer, Mote> currentMap = currentMotes.stream().collect(Collectors.toMap(Mote::getId, m -> m));
        Map<Integer, Mote> originalMap = originalMoteList.stream().collect(Collectors.toMap(Mote::getId, m -> m));

        // missing and extra motes & links
        ArrayList<Mote> extraMotes = new ArrayList<>();
        ArrayList<Mote> missingMotes = new ArrayList<>();
        ArrayList<Mote> wrongPositionMotes = new ArrayList<>();
        ArrayList<Mote> tooMuchLinksMotes = new ArrayList<>();
        ArrayList<Mote> missingLinksMotes = new ArrayList<>();
        ArrayList<Mote> wrongValuesMotes = new ArrayList<>();

        // check for extra motes and wrong value standard motes
        for (Mote mote : currentMotes) {
            if (!originalMap.containsKey(mote.getId())) {
                extraMotes.add(mote);
            } else if (mote.getId() >= 2 && mote.getId() <= 15) { // if standard mote
                Mote corrispondentOriginalMote = originalNetwork.getMoteWithId(mote.getId());

                // check if correct positions
                if (mote.getPosition().getX()!=corrispondentOriginalMote.getPosition().getX() || mote.getPosition().getY()!=corrispondentOriginalMote.getPosition().getY()) {
                    System.out.println(mote.getPosition());
                    wrongPositionMotes.add(mote);
                }

                // check links
                List<Link> currentLinks = mote.getLinks();
                List<Link> originalLinks = corrispondentOriginalMote.getLinks();

                // create sets based on from/to for future checks
                Set<String> currentLinksSet = currentLinks.stream()
                        .map(link -> link.getFrom().getId() + "->" + link.getTo().getId())
                        .collect(Collectors.toSet());

                Set<String> originalLinksSet = originalLinks.stream()
                        .map(link -> link.getFrom().getId() + "->" + link.getTo().getId())
                        .collect(Collectors.toSet());

                // find missing links
                boolean missingLinkFound = false;
                for (String originalLink : originalLinksSet) {
                    if (!currentLinksSet.contains(originalLink)) {
                        missingLinkFound = true;
                        break;
                    }
                }
                if (missingLinkFound) {
                    missingLinksMotes.add(mote);
                }

                // find extra links
                boolean extraLinkFound = false;
                for (String currentLink : currentLinksSet) {
                    if (!originalLinksSet.contains(currentLink)) {
                        extraLinkFound = true;
                        break;
                    }
                }
                if (extraLinkFound) {
                    tooMuchLinksMotes.add(mote);
                }


                if(corrispondentOriginalMote.getBatteryCapacity() != mote.getBatteryCapacity() || corrispondentOriginalMote.getLoad() != mote.getLoad()){

                    wrongValuesMotes.add(mote);
                }

            }
        }

        // find missing motes comparing originalMotes and currentMotes
        for (Mote mote : originalMotes) {
            if (!currentMap.containsKey(mote.getId())) {
                missingMotes.add(mote);
            }
        }

        // return anomalyGeneralizer
        anomalyGeneralizer.setAddedMotes(extraMotes);
        anomalyGeneralizer.setOriginalMotesMissing(missingMotes);
        anomalyGeneralizer.setTooMuchLinksMotes(tooMuchLinksMotes);
        anomalyGeneralizer.setMissingLinksMotes(missingLinksMotes);
        anomalyGeneralizer.setWrongValuesMotes(wrongValuesMotes);
        anomalyGeneralizer.setWrongPositionMotes(wrongPositionMotes);
        anomalyGeneralizer.setAnomalySimulator(s);
        return anomalyGeneralizer;
    }



}
