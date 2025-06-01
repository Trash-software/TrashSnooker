package trashsoftware.trashSnooker.core.attempt;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.CuePlayParams;
import trashsoftware.trashSnooker.core.person.CuePlayerHand;
import trashsoftware.trashSnooker.core.movement.Movement;

import java.util.Set;

public abstract class CueAttempt {

    protected Movement movement;
    protected CuePlayerHand cuePlayerHand;
    protected AttemptBase attemptBase;
    protected Set<Ball> legalPots;  // 合法打进的球
    protected final CuePlayParams playParams;
    
    protected CueAttempt(CueType type, CuePlayParams playParams) {
        this.attemptBase = new AttemptBase(type);
        this.playParams = playParams;
    }

    public CuePlayParams getPlayParams() {
        return playParams;
    }

    public void setSuccess(boolean success) {
        this.attemptBase.setSuccess(success);
    }

    public boolean isSuccess() {
        return attemptBase.isSuccess();
    }

    public void setAfterFinish(CuePlayerHand cuePlayerHand,
                               Movement movement) {
        this.cuePlayerHand = cuePlayerHand;
        this.movement = movement;

//        System.out.println("Trace: before " + whiteTrace.getCushionBefore() + ", after " + whiteTrace.getCushionAfter());
    }

    public AttemptBase getAttemptBase() {
        return attemptBase;
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

    public CuePlayerHand getCuePlayerHand() {
        return cuePlayerHand;
    }
}
