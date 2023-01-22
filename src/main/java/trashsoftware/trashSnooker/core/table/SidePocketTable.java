package trashsoftware.trashSnooker.core.table;

import javafx.scene.canvas.GraphicsContext;
import trashsoftware.trashSnooker.core.TableMetrics;
import trashsoftware.trashSnooker.fxml.GameView;

public class SidePocketTable extends NumberedBallTable {

    public SidePocketTable(TableMetrics tableMetrics) {
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

    @Override
    public int nBalls() {
        return 10;
    }
}
