package antiFrag.AnomalyBuilder;

import antiFrag.SimulationClientAF;
import deltaiot.client.SimulationClient;
import domain.*;
import org.apache.commons.math3.stat.inference.BinomialTest;
import simulator.Simulator;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.stream.Collectors;

import static antiFrag.AnomalyBuilder.BaseNetwork.GATEWAY_ID;
import static antiFrag.Position.FindPositionAndNeighbour.findClosestNode;
import static antiFrag.Position.FindPositionAndNeighbour.getPosition;

public class AnomalyBuilder {

    public static void main(String[] args) {
        int[] x = {0,0};
        int[] y = {0,0};
        int[] neigh = {0,0};

        Point2D point2D = getPosition();
        x[0] = (int)point2D.getX();
        y[0] = (int)point2D.getY();
        neigh[0] = findClosestNode(point2D);
        point2D = getPosition();
        x[1] = (int)point2D.getX();
        y[1] = (int)point2D.getY();
        neigh[1] = findClosestNode(point2D);
        SimulationClientAF sc = new SimulationClientAF(SimulationClientAF.Case.UNKNOWN, x, y, 118800.0, 200, neigh);
        BaseNetwork baseNetwork = new BaseNetwork();
        AnomalyGeneralizer anomalyGeneralizer = baseNetwork.analyzeUnknownAnomaly(sc.getSimulator());

        System.out.println(" -getAddedMotes: " +anomalyGeneralizer.getAddedMotes() + "\n -getOriginalMotesMissing: " + anomalyGeneralizer.getOriginalMotesMissing() + "\n -getTooMuchLinksMotes: " + anomalyGeneralizer.getTooMuchLinksMotes() + "\n -getMissingLinksMotes: " +
                anomalyGeneralizer.getMissingLinksMotes() + "\n -getWrongPositionMotes: " + anomalyGeneralizer.getWrongPositionMotes() + "\n -getWrongValuesMotes: " + anomalyGeneralizer.getWrongValuesMotes());

        Simulator replica = replicateAnomalyVariation(4, anomalyGeneralizer);
        System.out.println(replica.getMotes());
        System.out.println(replica.getMoteWithId(16).getLinks());
        System.out.println(replica.getMoteWithId(17).getLinks());
    }

    public static Simulator replicateAnomalyVariation(int seed, AnomalyGeneralizer anomalyGeneralizer){

        BaseNetwork baseNetwork = new BaseNetwork();
        SimulationClient baseClient = baseNetwork.getBaseSimulationClient();
        Simulator base = baseClient.getSimulator();
        Random r = new Random(seed);
        List<Mote> baseMotes = baseNetwork.getBaseMotesList();//base.getMotes();
        Simulator ret = new Simulator();
        Mote[] allMotes = new Mote[anomalyGeneralizer.getAnomalySimulator().getMotes().size()];
        List<Mote> anomalyMotesList = anomalyGeneralizer.getAnomalySimulator().getMotes();
        int power = 15;
        int distribution = 100;


        //removing base motes and altering their values if needed
        ArrayList<Integer> baseids = new ArrayList<>();

        for(Mote m : baseMotes){
            baseids.add(m.getId());
        }

        Collections.shuffle(baseids);
        for(int i = 0; i < anomalyGeneralizer.getOriginalMotesMissing().size(); i++){
             baseids.remove(0); //remove the forst after shuffle
        }
        for(Mote m : baseMotes){
            if(!baseids.contains(m.getId())){
                baseMotes.remove(m); //trimming base motes
            }
        }
        Collections.sort(baseids);

        ArrayList<Mote> wrongPositionMotes=anomalyGeneralizer.getWrongPositionMotes();
        ArrayList<Integer> alreadyModified = new ArrayList<>(wrongPositionMotes.size());
        for(int i = 0; i < wrongPositionMotes.size(); i++){
            int idx;
            do{
                idx = r.nextInt(baseids.size());
            }while (alreadyModified.contains(idx));
            alreadyModified.add(idx);

            baseMotes.get(idx).setPosition(wrongPositionMotes.get(i).getPosition());
        }


        //adding motes
        ArrayList<Mote> newMotes = new ArrayList<>();

        for(Mote m : anomalyGeneralizer.getAddedMotes()) {
            int id = m.getId();
            int load = m.getLoad();
            double capacity = m.getBatteryCapacity();

            Point2D point2D = getPosition();
            int x = (int)point2D.getX();
            int y = (int)point2D.getY();

            baseids.add(id);
            baseMotes.add(new Mote(id, capacity, load, new Position(x*2,y*2)));
            newMotes.add(new Mote(id, capacity, load, new Position(x*2,y*2)));
        }
        //got motes in positions, randomized if needed

        allMotes = baseMotes.toArray(new Mote[0]);
        ret.addMotes(allMotes);
        //motes added, now links

        List<Mote> baseMotesFromAnomaly = new ArrayList<>();
        for(Mote m : anomalyMotesList){
            if(m.getId()>=2 && m.getId()<=15){
                baseMotesFromAnomaly.add(m);
            }
        }

        Gateway gateway = new Gateway(GATEWAY_ID, new Position(482 * 2, 360 * 2));
        baseids.add(gateway.getId());
        gateway.setView(allMotes);
        ret.addGateways(gateway);

        HashMap<Integer, Integer> baseLinksMapping = new HashMap<>();
        HashMap<Integer, List<Link>> missingLinksMapping = new HashMap<>(); // ID mote -> Lista dei link mancanti
        HashMap<Integer, List<Link>> extraLinksMapping = new HashMap<>();   // ID mote -> Lista dei link aggiunti

        // motes with less link than normal
        for (Mote baseMote : base.getMotes()) {
            Mote anomalyMote = anomalyGeneralizer.getMissingLinksMotes().stream()
                    .filter(m -> m.getId() == baseMote.getId())
                    .findFirst()
                    .orElse(null);

            if (anomalyMote != null) {
                // link base
                List<Link> baseLinks = baseMote.getLinks();

                // link current
                List<Link> currentLinks = anomalyMote.getLinks();

                // get missing links
                List<Link> missingLinks = baseLinks.stream()
                        .filter(baseLink -> currentLinks.stream()
                                .noneMatch(currentLink -> currentLink.getTo().getId() == baseLink.getTo().getId()
                                        && currentLink.getPowerNumber() == baseLink.getPowerNumber()
                                        && currentLink.getDistribution() == baseLink.getDistribution()))
                        .collect(Collectors.toList());

                // add missing links
                if (!missingLinks.isEmpty()) {
                    missingLinksMapping.put(baseMote.getId(), missingLinks);
                }
            }
        }

        // motes with more links than normal
        for (Mote anomalyMote : anomalyGeneralizer.getTooMuchLinksMotes()) {

            Mote baseMote = base.getMotes().stream()
                    .filter(m -> m.getId() == anomalyMote.getId())
                    .findFirst()
                    .orElse(null);

            if (baseMote != null) {

                List<Link> baseLinks = baseMote.getLinks();


                List<Link> currentLinks = anomalyMote.getLinks();

                // find extra links
                List<Link> extraLinks = currentLinks.stream()
                        .filter(currentLink -> baseLinks.stream()
                                .noneMatch(baseLink -> baseLink.getTo().getId() == currentLink.getTo().getId()
                                        && baseLink.getPowerNumber() == currentLink.getPowerNumber()
                                        && baseLink.getDistribution() == currentLink.getDistribution()))
                        .collect(Collectors.toList());

                // map extra links
                if (!extraLinks.isEmpty()) {
                    extraLinksMapping.put(anomalyMote.getId(), extraLinks);
                }
            }
        }
    //-----------------------------------------------------------------




        for(Mote m : baseNetwork.getBaseSimulationClient().getSimulator().getMotes()){
            List<Link> links = m.getLinks();
            for(Link l : links){
                if(baseids.contains(l.getFrom().getId()) && baseids.contains(l.getTo().getId())) {

                    baseLinksMapping.put(l.getFrom().getId(), l.getTo().getId());
                    Node to;
                    if(l.getTo().getId() != GATEWAY_ID){
                        to = ret.getMoteWithId(l.getTo().getId());
                    }else{
                        to = gateway;
                    }
                    ret.getMoteWithId(m.getId()).addLinkTo(to, gateway, power, distribution);
                }
                //l.getTo()
            }
        }//setup base links already trimmed with missing motes

        //TODO finire di implementare il posizionamento dei link in più e in meno
        /**
         * Risultato
         * missingLinksMapping: Contiene gli ID dei mote e i link che mancano rispetto alla configurazione base.
         * extraLinksMapping: Contiene gli ID dei mote e i link in più rispetto alla configurazione base.
         * questi li costruisco ma non li uso!
         */



        int seedInc = 0;
        //setup links for extra motes in a random way
        for(Mote m : anomalyGeneralizer.getAddedMotes()){
            List<Link> addedMoteLinks = m.getLinks();

            List<Integer> neighs = extractNIntegers(baseids, GATEWAY_ID, addedMoteLinks.size(), seed+seedInc);
            int count = 0;
            seedInc++;
            for(Link l : addedMoteLinks){

                ret.getMoteWithId(m.getId()).addLinkTo(ret.getMoteWithId(neighs.get(count)), l.getDirection(), l.getPowerNumber(), l.getDistribution());
                count++;
            }

            for(Link l : ret.getMoteWithId(m.getId()).getLinks()){
                l.setSnrEquation(m.getLinks().get(0).getSnrEquation());
            }


        }

        // get modifiable list
        List<Integer> anomalyOrder = new ArrayList<>(anomalyGeneralizer.getAnomalySimulator().getTurnOrder());

        // get valid motes id
        List<Integer> validMoteIds = Arrays.asList(allMotes).stream()
                .map(Mote::getId)
                .collect(Collectors.toList());

        // remove id's not valid motes
        anomalyOrder.removeIf(id -> !validMoteIds.contains(id));

        List<Integer> order = new ArrayList<>(anomalyOrder);

        Integer[] turnOrderArray = order.toArray(new Integer[0]);
        ret.setTurnOrder(turnOrderArray);

        applySettingsToMotes(ret, gateway);


        ret.getRunInfo().setGlobalInterference(new DoubleRange(-5.0, 5.0));





        return ret;
    }

    public static List<Integer> extractNIntegers(ArrayList<Integer> baseids, int GATEWAY_ID, int N, int seed) {
        // Filter IDs > GATEWAY_ID & <= 15
        List<Integer> filteredIds = new ArrayList<>();
        for (int id : baseids) {
            if (id > GATEWAY_ID && id <= 15) {
                filteredIds.add(id);
            }
        }


        if (filteredIds.size() < N) {
            throw new IllegalArgumentException("Not enough valid IDs to extract " + N + " distinct integers.");
        }


        List<Integer> extracted = new ArrayList<>();
        Random random = new Random(seed);

        while (extracted.size() < N) {
            int index = random.nextInt(filteredIds.size());
            int chosen = filteredIds.get(index);
            if (!extracted.contains(chosen)) {
                extracted.add(chosen);
            }
        }

        return extracted;
    }




    public static void applySettingsToMotes(Simulator ret, Gateway gateway) {
        List<Mote> motes = ret.getMotes();
        //Gateway gateway = ret.getGateways().get(0);
        //System.out.println("debug " + gateway);




        Mote mote2 = getMoteSafe(motes, 2);
        Mote mote3 = getMoteSafe(motes, 3);
        Mote mote4 = getMoteSafe(motes, 4);
        Mote mote5 = getMoteSafe(motes, 5);
        Mote mote6 = getMoteSafe(motes, 6);
        Mote mote7 = getMoteSafe(motes, 7);
        Mote mote8 = getMoteSafe(motes, 8);
        Mote mote9 = getMoteSafe(motes, 9);
        Mote mote10 = getMoteSafe(motes, 10);
        Mote mote11 = getMoteSafe(motes, 11);
        Mote mote12 = getMoteSafe(motes, 12);
        Mote mote13 = getMoteSafe(motes, 13);
        Mote mote14 = getMoteSafe(motes, 14);
        Mote mote15 = getMoteSafe(motes, 15);


        if (mote2 != null) mote2.setActivationProbability(new Constant<>(0.85));
        if (mote8 != null) mote8.setActivationProbability(new Constant<>(0.85));
        if (mote10 != null) mote10.setActivationProbability(new FileProfile("deltaiot/scenario_data/PIR1.txt", 1.0));
        if (mote13 != null) mote13.setActivationProbability(new FileProfile("deltaiot/scenario_data/PIR2.txt", 1.0));
        if (mote14 != null) mote14.setActivationProbability(new Constant<>(0.85));


        if (mote12 != null && mote3 != null) {
            Link link = mote12.getLinkTo(mote3);
            if (link != null){
                link.setInterference(new FileProfile("deltaiot/scenario_data/SNR2.txt", 0.0));
            }
        }


        setSnrEquationSafe(mote2, mote4, new SNREquation(0.0473684210526, -5.29473684211));
        setSnrEquationSafe(mote3, gateway, new SNREquation(0.0280701754386, 4.25263157895));
        setSnrEquationSafe(mote4, gateway, new SNREquation(0.119298245614, -1.49473684211));
        setSnrEquationSafe(mote5, mote9, new SNREquation(-0.019298245614, 4.8));
        setSnrEquationSafe(mote6, mote4, new SNREquation(0.0175438596491, -3.84210526316));
        setSnrEquationSafe(mote7, mote3, new SNREquation(0.168421052632, 2.30526315789));
        setSnrEquationSafe(mote7, mote2, new SNREquation(-0.0157894736842, 3.77894736842));
        setSnrEquationSafe(mote8, gateway, new SNREquation(0.00350877192982, 0.45263157895));
        setSnrEquationSafe(mote9, gateway, new SNREquation(0.0701754385965, 2.89473684211));
        setSnrEquationSafe(mote10, mote6, new SNREquation(3.51139336547e-16, -2.21052631579));
        setSnrEquationSafe(mote10, mote5, new SNREquation(0.250877192982, -1.75789473684));
        setSnrEquationSafe(mote11, mote7, new SNREquation(0.380701754386, -2.12631578947));
        setSnrEquationSafe(mote12, mote7, new SNREquation(0.317543859649, 2.95789473684));
        setSnrEquationSafe(mote12, mote3, new SNREquation(-0.0157894736842, -3.77894736842));
        setSnrEquationSafe(mote13, mote11, new SNREquation(-0.0210526315789, -2.81052631579));
        setSnrEquationSafe(mote14, mote12, new SNREquation(0.0333333333333, 2.58947368421));
        setSnrEquationSafe(mote15, mote12, new SNREquation(0.0438596491228, 1.31578947368));
    }

    private static Mote getMoteSafe(List<Mote> motes, int id) {
        return motes.stream()
                .filter(m -> m.getId() == id)
                .findFirst()
                .orElse(null);
    }


    private static void setSnrEquationSafe(Mote from, Node to, SNREquation equation) {
        if (from != null && to != null) {
            Link link = from.getLinkTo(to);
            if (link != null) {
                link.setSnrEquation(equation);
            }
        }
    }

}
