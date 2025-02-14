package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.movement.Movement;

import java.util.Set;

public abstract class CueAttempt {

    protected Movement movement;
    protected PlayerHand handSkill;
    protected boolean success;
    protected Set<Ball> legalPots;  // 合法打进的球
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

    public void setAfterFinish(PlayerHand handSkill, 
                               Movement movement) {
        this.handSkill = handSkill;
        this.movement = movement;

//        System.out.println("Trace: before " + whiteTrace.getCushionBefore() + ", after " + whiteTrace.getCushionAfter());
    }

    public boolean isPotLegalBall() {
        return legalPots != null && !legalPots.isEmpty();
    }
    
    public void setAfterScoreUpdate(Set<Ball> legalPots) {
        this.legalPots = legalPots;
    }

    public Movement.Trace getWhiteTrace() {
        return movement.getWhiteTrace();
    }

    public Movement.Trace getTargetTrace() {
        return movement.getTargetTrace();
    }

    public PlayerHand getHandSkill() {
        return handSkill;
    }
}
