package antiFrag.AnomalyBuilder;

import domain.Mote;
import simulator.Simulator;

import java.util.ArrayList;

public class AnomalyGeneralizer {
    private ArrayList<Mote> addedMotes;
    private ArrayList<Mote> originalMotesMissing;
    private ArrayList<Mote> wrongPositionMotes;
    private ArrayList<Mote> tooMuchLinksMotes;
    private ArrayList<Mote> missingLinksMotes;
    private ArrayList<Mote> wrongValuesMotes;

    private Simulator anomalySimulator;

    public AnomalyGeneralizer() {
        this.addedMotes = new ArrayList<>();
        this.originalMotesMissing = new ArrayList<>();
        this.wrongPositionMotes = new ArrayList<>();
        this.tooMuchLinksMotes = new ArrayList<>();
        this.missingLinksMotes = new ArrayList<>();
        this.wrongValuesMotes = new ArrayList<>();
    }

    public ArrayList<Mote> getAddedMotes() {
        return addedMotes;
    }

    public void setAddedMotes(ArrayList<Mote> addedMotes) {
        this.addedMotes = addedMotes;
    }

    public ArrayList<Mote> getOriginalMotesMissing() {
        return originalMotesMissing;
    }

    public void setOriginalMotesMissing(ArrayList<Mote> originalMotesMissing) {
        this.originalMotesMissing = originalMotesMissing;
    }

    public ArrayList<Mote> getWrongPositionMotes() {
        return wrongPositionMotes;
    }

    public void setWrongPositionMotes(ArrayList<Mote> wrongPositionMotes) {
        this.wrongPositionMotes = wrongPositionMotes;
    }

    public ArrayList<Mote> getTooMuchLinksMotes() {
        return tooMuchLinksMotes;
    }

    public void setTooMuchLinksMotes(ArrayList<Mote> tooMuchLinksMotes) {
        this.tooMuchLinksMotes = tooMuchLinksMotes;
    }

    public ArrayList<Mote> getMissingLinksMotes() {
        return missingLinksMotes;
    }

    public void setMissingLinksMotes(ArrayList<Mote> missingLinksMotes) {
        this.missingLinksMotes = missingLinksMotes;
    }

    public ArrayList<Mote> getWrongValuesMotes() {
        return wrongValuesMotes;
    }

    public void setWrongValuesMotes(ArrayList<Mote> wrongValuesMotes) {
        this.wrongValuesMotes = wrongValuesMotes;
    }

    public Simulator getAnomalySimulator() {
        return anomalySimulator;
    }

    public void setAnomalySimulator(Simulator anomalySimulator) {
        this.anomalySimulator = anomalySimulator;
    }
}
