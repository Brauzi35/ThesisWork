package simulator;

public class QoS {
	private double PacketLoss;
	private double PowerConsumption;
	private String period;

	//new qos
	private int NumNodesLoss;
	private int NumNodesEnergy;

	private double fairnessIndex;
	
	public double getPacketLoss() {
		return PacketLoss;
	}
	
	public double getEnergyConsumption() {
		return PowerConsumption;
	}
	
	public String getPeriod() {
		return period;
	}
	
	public void setPacketLoss(double packetLoss) {
		this.PacketLoss = packetLoss;
	}
	
	public void setEnergyConsumption(double energyConsumption) {
		this.PowerConsumption = energyConsumption;
	}
	
	public void setPeriod(String period) {
		this.period = period;
	}

	public int getNumNodesLoss() {
		return NumNodesLoss;
	}

	public void setNumNodesLoss(int numNodesLoss) {
		this.NumNodesLoss = numNodesLoss;
	}

	public int getNumNodesEnergy() {
		return NumNodesEnergy;
	}

	public void setNumNodesEnergy(int numNodesEnergy) {
		this.NumNodesEnergy = numNodesEnergy;
	}

	public double getFairnessIndex() {
		return fairnessIndex;
	}

	public void setFairnessIndex(double fairnessIndex) {
		this.fairnessIndex = fairnessIndex;
	}

	@Override
	public String toString() {
		return String.format("%s, %f, %f, %d, %d, %f", period, PacketLoss, PowerConsumption, NumNodesEnergy, NumNodesLoss, fairnessIndex);
	}
}
