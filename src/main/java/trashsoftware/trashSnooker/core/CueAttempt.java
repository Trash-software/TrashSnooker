package trashsoftware.trashSnooker.core;

public abstract class CueAttempt {
    
    protected boolean success;

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
