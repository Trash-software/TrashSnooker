package trashsoftware.trashSnooker.core;

public class DefenseAttempt extends CueAttempt {
    public final Player defensePlayer;
    private boolean solvingSnooker;
    private boolean solveSuccess;

    public DefenseAttempt(Player player, CuePlayParams playParams, boolean solvingSnooker) {
        super(playParams);
        
        this.defensePlayer = player;
        this.solvingSnooker = solvingSnooker;
        this.success = true;
    }

    public boolean isSolveSuccess() {
        return solveSuccess;
    }

    public boolean isSolvingSnooker() {
        return solvingSnooker;
    }

    public void setSolveSuccess(boolean solveSuccess) {
        this.solveSuccess = solveSuccess;
        System.out.println("Solve success: " + solveSuccess);
    }
}
