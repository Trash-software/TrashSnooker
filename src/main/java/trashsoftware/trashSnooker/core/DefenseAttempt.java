package trashsoftware.trashSnooker.core;

public class DefenseAttempt {
    public final Player defensePlayer;
    private boolean solvingSnooker = false;
    private boolean solveSuccess;
    private boolean success = true;

    public DefenseAttempt(Player player, boolean solvingSnooker) {
        this.defensePlayer = player;
        this.solvingSnooker = solvingSnooker;
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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
