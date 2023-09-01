package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.metrics.Cushion;
import trashsoftware.trashSnooker.core.movement.Movement;

import java.util.Set;

public class DefenseAttempt extends CueAttempt {
    public final Player defensePlayer;
    private final boolean solvingSnooker;
    private boolean legal;
    private boolean breaking;

    public DefenseAttempt(Player player, CuePlayParams playParams, boolean solvingSnooker) {
        super(playParams);
        
        this.defensePlayer = player;
        this.solvingSnooker = solvingSnooker;
        this.success = true;
    }

    /**
     * 就在这一杆结束后调用，并非是下一杆
     * 
     * @param legalPots    打进的该进的球
     * @param isBreaking   是否为开球
     * @param legal        是否未犯规
     */
    public void setAfterScoreUpdate(Set<Ball> legalPots, 
                                    boolean isBreaking, 
                                    boolean legal) {
        setAfterScoreUpdate(legalPots);
        
        if (movement == null) {
            throw new RuntimeException("This method should not be called before setting trace");
        }
        
        this.breaking = isBreaking;
        this.legal = legal;
    }

    public boolean isSolveSuccess() {
        return isSolvingSnooker() && isLegal();
    }

    public boolean isSolvingSnooker() {
        return solvingSnooker;
    }

    public boolean isLegal() {
        return legal;
    }

    /**
     * @return 是否为传球进球
     */
    public boolean isPassPot() {
        if (!legal) return false;
        if (legalPots.isEmpty()) return false;
        if (breaking) return false;
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
        if (breaking) return false;
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
