package trashsoftware.trashSnooker.core.table;

import javafx.scene.canvas.GraphicsContext;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.GameHolder;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.fxml.widgets.GamePane;

import java.util.ArrayList;
import java.util.List;

public class ChineseEightTable extends NumberedBallTable {

    public ChineseEightTable(TableMetrics tableMetrics) {
        super(tableMetrics);
    }

    @Override
    public void drawTableMarks(GamePane view, GraphicsContext graphicsContext, double scale) {
        super.drawTableMarks(view, graphicsContext, scale);

        // 中八的置球线
        double firstBallX = firstBallPlacementX();
        double botPointX = firstBallPlacementX() + tableMetrics.innerWidth / 8;
        double botCanvasX = view.canvasX(botPointX);
        double midY = view.canvasY(tableMetrics.midY);

        double radius1 = 3.0 * scale;
        graphicsContext.setFill(GameView.WHITE);
        graphicsContext.setStroke(GameView.WHITE);
        graphicsContext.fillOval(
                botCanvasX - radius1,
                midY - radius1,
                radius1 * 2,
                radius1 * 2);
        graphicsContext.strokeLine(
                view.canvasX(firstBallX), midY,
                botCanvasX, midY
        );
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
