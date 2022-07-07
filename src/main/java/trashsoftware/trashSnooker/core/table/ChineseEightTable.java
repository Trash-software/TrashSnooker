package trashsoftware.trashSnooker.core.table;

import javafx.scene.canvas.GraphicsContext;
import trashsoftware.trashSnooker.core.GameValues;
import trashsoftware.trashSnooker.fxml.GameView;

public class ChineseEightTable extends NumberedBallTable {

    protected ChineseEightTable() {
        super(GameValues.CHINESE_EIGHT_VALUES);
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
