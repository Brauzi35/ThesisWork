package mapek;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import deltaiot.client.Effector;
import deltaiot.services.Link;
import deltaiot.services.LinkSettings;
import deltaiot.services.Mote;
import deltaiot.client.Probe;

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

	public void start() {
		System.out.println("Feedback loop started.");
		for (int i = 0; i < 96; i++) {
			monitor();
			try {
				System.out.println("Wait for 15 minutes to let the adaptation settings apply in to the system.");
				Thread.currentThread().sleep(900000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	void monitor() {
		System.out.println("Monitoring started");
		motes = probe.getAllMotes();
		
		motes.forEach(mote -> printMote(mote));

		analysis();
	}

	void analysis() {

		// analyze all link settings
		boolean adaptationRequired = analyzeLinkSettings();

		// if adaptation required invoke the planner
		if (adaptationRequired) {
			planning();
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

	void planning() {

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
						findLink(mote, (step.link.getDest())).setPower(step.value);
					} else if (step.step == Step.CHANGE_DIST) {
						findLink(mote, (step.link.getDest())).setDistribution(step.value);
					}
				}
			}
			if (addMote)
				motesEffected.add(mote);
		}
		List<LinkSettings> newSettings;
		
		System.out.println("Adaptations:");
		for(Mote mote: motesEffected){
			printMote(mote);
			newSettings = new LinkedList<LinkSettings>();
			for(Link link: mote.getLinks()){
				newSettings.add(newLinkSettings(mote.getMoteid(), link.getDest(), link.getPower(), link.getDistribution(), link.getSF()));
			}
			effector.setMoteSettings(mote.getMoteid(), newSettings);
		}
		steps.clear();
	}
	
	Link findLink(Mote mote, int dest){
		for(Link link: mote.getLinks()){
			if (link.getDest() == dest)
				return link;
		}
		throw new RuntimeException(String.format("Link %d --> %d not found", mote.getMoteid(), dest));
	}
	
	public LinkSettings newLinkSettings(int src, int dest, int power, int distribution, int sf){
		LinkSettings settings = new LinkSettings();
		settings.setSrc(src);
		settings.setDest(dest);
		settings.setPowerSettings(power);
		settings.setDistributionFactor(distribution);
		settings.setSpreadingFactor(sf);
		return settings;
	}
	
	void printMote(Mote mote){
		System.out.println(String.format("MoteId: %d, BatteryRemaining: %f, Parents:%d, Links:%s", mote.getMoteid(), mote.getBattery(), mote.getParents(), getLinkString(mote.getLinks())));
	}
	
	String getLinkString(List<Link> links){
		StringBuilder strBuilder = new StringBuilder();
		for(Link link: links){
			strBuilder.append(String.format("[Dest: %d, Power:%d, DistributionFactor:%d]", link.getDest(), link.getPower(), link.getDistribution()));
		}
		return strBuilder.toString();
	}
}
