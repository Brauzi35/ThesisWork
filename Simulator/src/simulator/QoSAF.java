package simulator;

public class QoSAF{
    int aliveNodes;
    double energyUsagePlus;

    private double PacketLoss;
    private double PowerConsumption;
    private String period;

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

    public int getAliveNodes() {
        return aliveNodes;
    }

    public void setAliveNodes(int aliveNodes) {
        this.aliveNodes = aliveNodes;
    }

    public double getEnergyUsagePlus() {
        return energyUsagePlus;
    }

    public void setEnergyUsagePlus(double energyUsagePlus) {
        this.energyUsagePlus = energyUsagePlus;
    }

    @Override
    public String toString() {
        return String.format("%s, %f, %f, %f, %d", period, PacketLoss, PowerConsumption, energyUsagePlus, aliveNodes);
    }
}
