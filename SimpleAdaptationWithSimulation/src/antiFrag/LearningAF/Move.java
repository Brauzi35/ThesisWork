package antiFrag.LearningAF;

public class Move {
    int oldState;
    int newState;
    int action;
    double reward;

    public Move(int oldState, int action, int newState, double reward) {
        this.oldState = oldState;
        this.newState = newState;
        this.reward = reward;
        this.action = action;
    }
}
