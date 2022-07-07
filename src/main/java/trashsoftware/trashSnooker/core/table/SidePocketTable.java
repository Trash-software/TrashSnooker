package trashsoftware.trashSnooker.core.table;

import javafx.scene.canvas.GraphicsContext;
import trashsoftware.trashSnooker.core.GameValues;
import trashsoftware.trashSnooker.fxml.GameView;

public class SidePocketTable extends NumberedBallTable {

    protected SidePocketTable() {
        super(GameValues.SIDE_POCKET);
    }
    
    @Override
    public void drawTableMarks(GameView view, GraphicsContext graphicsContext, double scale) {
        // 开球线
        double breakLineX = view.canvasX(breakLineX());
        graphicsContext.setStroke(GameView.WHITE);
        graphicsContext.strokeLine(
                breakLineX,
                view.canvasY(gameValues.topY),
                breakLineX,
                view.canvasY(gameValues.topY + gameValues.innerHeight));
    }
}
