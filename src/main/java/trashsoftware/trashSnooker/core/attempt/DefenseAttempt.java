package trashsoftware.trashSnooker.core.attempt;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.CuePlayParams;
import trashsoftware.trashSnooker.core.Player;
import trashsoftware.trashSnooker.core.movement.Movement;

import java.util.Set;

public class DefenseAttempt extends CueAttempt {
    public final Player defensePlayer;
    private boolean legal;

    public DefenseAttempt(CueType type, 
                          Player player, CuePlayParams playParams) {
        super(type, playParams);
        
        this.defensePlayer = player;
        this.setSuccess(true);
    }

    /**
     * 就在这一杆结束后调用，并非是下一杆
     * 
     * @param legalPots    打进的该进的球
     * @param legal        是否未犯规
     */
    public void setAfterScoreUpdate(Set<Ball> legalPots,
                                    boolean legal) {
        setAfterScoreUpdate(legalPots);
        
        if (movement == null) {
            throw new RuntimeException("This method should not be called before setting trace");
        }
        
        this.legal = legal;
    }

    public boolean isSolveSuccess() {
        return isSolvingSnooker() && isLegal();
    }

    public boolean isSolvingSnooker() {
        return attemptBase.type == CueType.SOLVE;
    }

    public boolean isLegal() {
        return legal;
    }
    
    public boolean isBreaking() {
        return attemptBase.type == CueType.BREAK;
    }

    /**
     * @return 是否为传球进球
     */
    public boolean isPassPot() {
        if (!legal) return false;
        if (legalPots.isEmpty()) return false;
        if (isBreaking()) return false;
        if (movement.getTargetTrace() == null) {
            // 九球的push out会进这个分支
            System.out.println("Pushing out, legal but no target");
            return false;
        }
        
        for (Ball ball : legalPots) {
            if (ball != movement.getWhiteFirstCollide()) {
                Movement.Trace trace = movement.getTraceOfBallNullable(ball);
                if (trace != null) {
                    int edgeCushions = trace.getTotalEdgeCushionCount();
                    // 道理是：
                    // 进的球不是白球第一颗碰到的
                    // 又没在进球之前再碰库
                    // 那就假装你是传进的
                    if (edgeCushions == 0) {
                        System.out.println("Pass pot!");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @return 是否为翻袋进球
     */
    public boolean isDoublePot() {
        if (!legal) return false;
        if (legalPots.isEmpty()) return false;
        if (isBreaking()) return false;
        if (movement.getTargetTrace() == null) {
            // 九球的push out会进这个分支
            System.out.println("Pushing out, legal but no target");
            return false;
        }

        Movement.Trace targetTrace = getTargetTrace();
        int edgeCushions = targetTrace.getTotalEdgeCushionCount();
        return edgeCushions > 0;
    }
}
