package trashsoftware.trashSnooker.core;

public class DefenseAttempt extends CueAttempt {
    public final Player defensePlayer;
    private boolean solvingSnooker = false;
    private boolean solveSuccess;

    public DefenseAttempt(Player player, boolean solvingSnooker) {
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
