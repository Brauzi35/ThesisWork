package deltaiot.client;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import javax.jws.WebMethod;
import javax.jws.WebService;

import deltaiot.DeltaIoTSimulator;
import deltaiot.services.LinkSettings;
import domain.Link;
import domain.Mote;
import domain.Node;
import simulator.QoS;
import simulator.Simulator;

public class SimulationClient implements Probe, Effector {

	private Simulator simulator;

	List<String> log = new LinkedList<String>();
	int randomNumber;
	private boolean writeInfos = false;
	private String folderName = null;
	
	public SimulationClient(){

		this.simulator = deltaiot.DeltaIoTSimulator.createSimulatorForDeltaIoT();
		Random random = new Random();

		this.randomNumber = random.nextInt(1000) + 1;
	}
	
	public SimulationClient(Simulator simulator) {
		this.simulator = simulator;
	}

	public SimulationClient(Simulator simulator, boolean writeInfos, String folderName) {
		this.simulator = simulator;
		this.writeInfos = writeInfos;
		this.folderName = folderName;


	}



	@Override
	public ArrayList<deltaiot.services.Mote> getAllMotes() {
		//here we start
		simulator.doSingleRun();


		if(writeInfos){
			String string = folderName +"/state"+this.simulator+".txt";
			printToponomy(simulator, string); //added myself
		}

		List<Mote> motes = simulator.getMotes();
		ArrayList<deltaiot.services.Mote> afMotes = new ArrayList<>();
		for (Mote mote : motes) {
			afMotes.add(DeltaIoTSimulator.getAfMote(mote, simulator));
		}
		return afMotes;
	}

	/*
	Aggiunto io per ottenere file per il training dell'anomaly detection
	 */
	/*
	public static void printToponomy(Simulator sim){
		int idx = sim.getRunInfo().getRunNumber(); //is like getting a timestamp
		System.out.println("timestamp:"+idx);
		List<Mote> motes = sim.getMotes();
		for(Mote m : motes){
			System.out.println(m.toString());

			List<Link> links = m.getLinks();
			for(Link l : links){
				System.out.println(l.toString());
			}
		}
	}

	 */

	public static void printToponomy(Simulator sim, String filePath) {
		int idx = sim.getRunInfo().getRunNumber(); // Timestamp basato sul numero di esecuzione
		List<Mote> motes = sim.getMotes();

		// Utilizza BufferedWriter per scrivere su file
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
			// Scrivi il timestamp
			writer.write("timestamp: " + idx);
			writer.newLine(); // Va a capo

			// Scrivi le informazioni su ciascun Mote
			for (Mote m : motes) {
				writer.write(m.toString());
				writer.newLine(); // Va a capo

				// Scrivi le informazioni sui Link associati a ciascun Mote
				List<Link> links = m.getLinks();
				for (Link l : links) {
					writer.write(l.toString());
					writer.newLine(); // Va a capo
				}
			}

			writer.write("---------------------------------------------------"); // Separatore tra timestamp
			writer.newLine(); // Va a capo
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Mote getMote(int moteId) {
		return simulator.getMoteWithId(moteId);
	}

	private domain.Link getLink(int src, int dest) {
		Mote from = simulator.getMoteWithId(src);
		Node to = simulator.getNodeWithId(dest);
		domain.Link link = from.getLinkTo(to);
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

//	@Override
//	public void setLinkSF(int src, int dest, int sf) {
//		getLink(src, dest).setSfTimeNumber(sf);
//	}
//
//	@Override
//	public void setLinkPower(int src, int dest, int power) {
//		getLink(src, dest).setPowerNumber(power);
//	}
//
//	@Override
//	public void setLinkDistributionFactor(int src, int dest, int distributionFactor) {
//		getLink(src, dest).setDistribution(distributionFactor);
//	}

	@Override
	public void setMoteSettings(int moteId, List<LinkSettings> linkSettings) {
		Mote mote = getMote(moteId);
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
		List<Mote> motes = simulator.getMotes();
		for (Mote mote : motes) {
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
