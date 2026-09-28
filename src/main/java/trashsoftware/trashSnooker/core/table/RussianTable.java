package trashsoftware.trashSnooker.core.table;

import javafx.scene.canvas.GraphicsContext;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.fxml.widgets.GamePane;

public class RussianTable extends Table {
    public RussianTable(TableMetrics tableMetrics) {
        super(tableMetrics);
    }

    @Override
    public void drawTableMarks(GamePane view, GraphicsContext graphicsContext, double scale) {
        
    }

    @Override
    public double breakLineX() {
        return tableMetrics.leftX + tableMetrics.innerWidth * 0.2065;
    }

    @Override
    public double rackFirstBallX(GameValues gameValues) {
        return tableMetrics.leftX + (tableMetrics.innerWidth * 0.75);
    }
}
