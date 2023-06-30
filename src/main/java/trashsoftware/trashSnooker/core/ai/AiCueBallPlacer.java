package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallPlayer;
import trashsoftware.trashSnooker.core.numberedGames.nineBall.AmericanNineBallGame;
import trashsoftware.trashSnooker.core.numberedGames.nineBall.AmericanNineBallPlayer;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.core.snooker.SnookerPlayer;

import java.util.List;

public abstract class AiCueBallPlacer<G extends Game<?, ?>, P extends Player> {
    protected final G game;
    protected final P player;

    public AiCueBallPlacer(G game, P player) {
        this.game = game;
        this.player = player;
    }

    public static AiCueBallPlacer<? extends Game<?, ?>, ? extends Player> createAiCueBallPlacer(
            Game<?, ?> game, Player aiPlayer) {
        if (game instanceof AbstractSnookerGame) {
            return new SnookerAiCueBallPlacer((AbstractSnookerGame) game, (SnookerPlayer) aiPlayer);
        } else if (game instanceof ChineseEightBallGame) {
            return new ChineseEightAiCueBallPlacer(
                    (ChineseEightBallGame) game, (ChineseEightBallPlayer) aiPlayer);
        } else if (game instanceof AmericanNineBallGame) {
            return new AmericanNineAiCueBallPlacer(
                    (AmericanNineBallGame) game, (AmericanNineBallPlayer) aiPlayer);
        } else {
            throw new RuntimeException("No such game type");
        }
    }

    protected abstract List<double[]> legalPositions();
    
    protected abstract double[] breakPosition();

    /**
     * 返回AI选定的放置白球位坐标，但并不真正放置白球
     */
    public double[] getPositionToPlaceCueBall() {
        if (game.isBreaking()) {
            double[] breakPos = breakPosition();
            if (breakPos != null) return breakPos;
        }
        
        boolean justAfterBreak = false;
        if (game instanceof ChineseEightBallGame) {
            justAfterBreak = ((ChineseEightBallGame) game).isJustAfterBreak();
        }
        
        List<double[]> legalPositions = legalPositions();
        if (legalPositions.isEmpty()) return null;
        
        int target = game.getCurrentTarget();
        List<Ball> legalBalls = 
                game.getAllLegalBalls(target, 
                        false,  // 手中球怎么可能是斯诺克自由球
                        justAfterBreak);  // 摆球说明上一杆犯规了，是黑八的线内自由

        double[] bestPos = null;
        double maxPrice = 0;
        for (double[] pos : legalPositions) {
            List<AiCue.AttackChoice> attackChoices = AiCue.getAttackChoices(
                    game,
                    target,
                    player,
                    null,
                    legalBalls,
                    pos,
                    false
            );
            for (AiCue.AttackChoice choice : attackChoices) {
                if (choice.defaultRef.price > maxPrice) {
                    maxPrice = choice.defaultRef.price;
                    bestPos = pos;
                }
            }
        }
        if (bestPos == null) {
            // 没有进攻选择
            // 当前要求：别给自己摆杆斯诺克就好
            for (Ball ball : legalBalls) {
                for (double[] pos : legalPositions) {
//                    System.out.println();
                    if (game.pointToPointCanPassBall(
                            pos[0], pos[1],
                            ball.getX(), ball.getY(),
                            game.getCueBall(), ball, 
                            true, false)) return pos;
                }
            }
            // 要是还是null
            System.out.println("AI不知道能摆哪了");
            int index = (int) (Math.random() * legalPositions.size());
            return legalPositions().get(index);
        }

        return bestPos;
    }
}
