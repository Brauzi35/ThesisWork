package mapek;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import antiFrag.SimulationClientAF;
import antiFrag.TwinInterrogation;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.client.SimulationClient;
import deltaiot.services.Link;
import deltaiot.services.LinkSettings;
import deltaiot.services.Mote;
import simulator.Simulator;


public class FeedbackLoop {

	Probe probe;
	Effector effector;

	// Knowledge
	ArrayList<Mote> motes;
	List<PlanningStep> steps = new LinkedList<>();

	public void setProbe(Probe probe) {
		this.probe = probe;
	}

	public void setEffector(Effector effector) {
		this.effector = effector;
	}

	public void start(int[] conf) {
		for (int i = 0; i < 95; i++) {
			monitor(conf);
		}
	}

	void monitor(int[] conf) {
		motes = probe.getAllMotes();

		// perform analysis
		analysis(conf);
	}

	void analysis(int[] conf) {

		// analyze all link settings
		boolean adaptationRequired = analyzeLinkSettings();

		// if adaptation required invoke the planner
		if (adaptationRequired) {
			planning(conf);
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

	void planning(int[] conf) {

		// Go through all links
		boolean powerChanging = false;
		Link left, right;
		for (Mote mote : motes) {
			for (Link link : mote.getLinks()) {
				powerChanging = false;
				if (link.getSNR() > 0 && link.getPower() > 0) {
					steps.add(new PlanningStep(Step.CHANGE_POWER, link, link.getPower() - conf[0]));
					powerChanging = true;
				} else if (link.getSNR() < 0 && link.getPower() < 15) {
					steps.add(new PlanningStep(Step.CHANGE_POWER, link, link.getPower() + conf[1]));
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
						steps.add(new PlanningStep(Step.CHANGE_DIST, left, left.getDistribution() + conf[2]));
						steps.add(new PlanningStep(Step.CHANGE_DIST, right, right.getDistribution() - conf[2]));
					} else if (right.getDistribution() < 100) {
						steps.add(new PlanningStep(Step.CHANGE_DIST, right, right.getDistribution() + conf[2]));
						steps.add(new PlanningStep(Step.CHANGE_DIST, left, left.getDistribution() - conf[2]));
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
