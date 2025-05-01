package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.Ball;

import java.util.ArrayList;
import java.util.List;

// 所有的列表型都代表：第一个是中心位置（正确位），后面是误差位置
public class DefenseResult {
    
    // 如果白球落位不准，对手的机会
//    List<List<AttackChoice>> surroundingDirectAttackChoices = new ArrayList<>();
    
    public final int opponentTarget;
    public final List<Ball> opponentBalls;
    public final boolean isSolving;
    
    double opponentAttackPrice = 0.0;
    double opponentAvailPrice = 0.0;
    double snookerScore = 0.0;
    
    List<AttackChoice> opponentEasies = new ArrayList<>();
    
    AttackChoice grossOpponentEasiest;
    
    public DefenseResult(int opponentTarget, List<Ball> opponentBalls, boolean isSolving) {
        this.opponentTarget = opponentTarget;
        this.opponentBalls = opponentBalls;
        this.isSolving = isSolving;
    }
}
