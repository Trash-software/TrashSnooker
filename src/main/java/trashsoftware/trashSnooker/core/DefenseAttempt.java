package trashsoftware.trashSnooker.core;

public class DefenseAttempt {
    public final Player defensePlayer;
    private boolean success = true;

    public DefenseAttempt(Player player) {
        this.defensePlayer = player;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
