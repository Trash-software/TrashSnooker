package trashsoftware.trashSnooker.core.table;

import javafx.scene.canvas.GraphicsContext;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.GameHolder;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.fxml.GameView;

import java.util.ArrayList;
import java.util.List;

public class ChineseEightTable extends NumberedBallTable {

    public ChineseEightTable(TableMetrics tableMetrics) {
        super(tableMetrics);
    }

    @Override
    public void drawTableMarks(GameView view, GraphicsContext graphicsContext, double scale) {
        // 开球线
        double breakLineX = view.canvasX(breakLineX());
        graphicsContext.setStroke(GameView.WHITE);
        graphicsContext.strokeLine(
                breakLineX,
                view.canvasY(tableMetrics.topY),
                breakLineX,
                view.canvasY(tableMetrics.topY + tableMetrics.innerHeight));
    }
    
    public static List<PoolBall> filterRemainingTargetOfPlayer(
            int playerTarget, GameHolder holder) {
        List<PoolBall> list = new ArrayList<>();
        if (playerTarget == ChineseEightBallGame.NOT_SELECTED_REP) return list;
        
        int base = playerTarget == ChineseEightBallGame.FULL_BALL_REP ? 1 : 9;
        for (int i = base; i < base + 7; i++) {
            Ball ball = holder.getBallByValue(i);
            if (!ball.isPotted()) {
                list.add((PoolBall) ball);
            }
        }
        Ball eight = holder.getBallByValue(8);
        if (!eight.isPotted()) {
            list.add((PoolBall) eight);
        }
        return list;
    }

    @Override
    public int nBalls() {
        return 16;
    }
}
