package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.movement.Movement;

public abstract class CueAttempt {

    protected Movement.Trace whiteTrace;
    protected Movement.Trace targetTrace;
    protected PlayerPerson.HandSkill handSkill;
    protected boolean success;
    protected final CuePlayParams playParams;
    
    protected CueAttempt(CuePlayParams playParams) {
        this.playParams = playParams;
    }

    public CuePlayParams getPlayParams() {
        return playParams;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setAfterFinish(PlayerPerson.HandSkill handSkill, 
                               Movement.Trace whiteTrace,
                               Movement.Trace targetTrace) {
        this.handSkill = handSkill;
        this.whiteTrace = whiteTrace;
        this.targetTrace = targetTrace;

//        System.out.println("Trace: before " + whiteTrace.getCushionBefore() + ", after " + whiteTrace.getCushionAfter());
    }

    public Movement.Trace getWhiteTrace() {
        return whiteTrace;
    }

    public Movement.Trace getTargetTrace() {
        return targetTrace;
    }

    public PlayerPerson.HandSkill getHandSkill() {
        return handSkill;
    }
}
