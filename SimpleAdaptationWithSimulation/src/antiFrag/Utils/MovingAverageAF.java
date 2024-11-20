package antiFrag.Utils;

public class MovingAverageAF {
    private double lastValue;
    private double actualValue;
    private double lastDelta;

    private double actualDelta;

    public MovingAverageAF() {
        this.lastValue = 0;
        this.actualValue = 0;
        this.lastDelta = 0;
        this.actualDelta = 0;
    }

    public double getactualValue() {
        return actualValue;
    }

    private void setactualValue(double actualValue) {
        this.actualValue = actualValue;
    }

    public double getLastDelta() {
        return lastDelta;
    }

    private void setLastDelta(double lastDelta) {
        this.lastDelta = lastDelta;
    }

    public void update(double value){
        double backup = lastValue;
        lastValue = actualValue;
        actualValue = value;
        lastDelta = actualDelta;
        actualDelta = Math.abs(lastValue - value);

    }

    public boolean isRecovery(){
        if(actualDelta > lastDelta*3){
            return true;
        }
        return false;
    }


}
