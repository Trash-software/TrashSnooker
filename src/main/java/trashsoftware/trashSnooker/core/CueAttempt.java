package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.movement.Movement;

public abstract class CueAttempt {

    protected Movement.Trace whiteTrace;
    protected PlayerPerson.HandSkill handSkill;
    protected boolean success;

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setAfterFinish(PlayerPerson.HandSkill handSkill, Movement.Trace whiteTrace) {
        this.handSkill = handSkill;
        this.whiteTrace = whiteTrace;

//        System.out.println("Trace: before " + whiteTrace.getCushionBefore() + ", after " + whiteTrace.getCushionAfter());
    }

    public Movement.Trace getWhiteTrace() {
        return whiteTrace;
    }

    public PlayerPerson.HandSkill getHandSkill() {
        return handSkill;
    }
}
