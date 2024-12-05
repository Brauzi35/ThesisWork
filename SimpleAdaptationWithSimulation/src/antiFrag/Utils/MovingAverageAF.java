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

    public double getLastValue() {
        return lastValue;
    }

    public void setLastValue(double lastValue) {
        this.lastValue = lastValue;
    }

    public double getActualValue() {
        return actualValue;
    }

    public void setActualValue(double actualValue) {
        this.actualValue = actualValue;
    }

    public double getActualDelta() {
        return actualDelta;
    }

    public void setActualDelta(double actualDelta) {
        this.actualDelta = actualDelta;
    }

    public void update(double value){
        System.out.println("getDistance " + value);

        double backup = lastValue;
        lastValue = actualValue;
        actualValue = value;
        lastDelta = actualDelta;
        actualDelta = Math.abs(lastValue - value);




    }

    public boolean isRecovery(){
        //System.out.println("actual delta " + actualDelta);
        //System.out.println("last delta " + actualDelta);
        //System.out.println("actual value " + actualValue);
        //System.out.println("last value " + actualValue);
        if(actualDelta > lastDelta*3){
            return true;
        }
        return false;
    }


}
