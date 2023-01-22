package trashsoftware.trashSnooker.core.table;

import trashsoftware.trashSnooker.core.TableMetrics;

public class SnookerTable extends AbstractSnookerTable {

    public SnookerTable(TableMetrics tableMetrics) {
        super(tableMetrics);
    }

    @Override
    public double breakLineX() {
        return tableMetrics.leftX + 737.0;
    }

    @Override
    public double breakArcRadius() {
        return 292.0;
    }

    @Override
    public double[] blackBallPos() {
        return new double[]{tableMetrics.rightX - 324.0, tableMetrics.midY};
    }

    @Override
    public int nBalls() {
        return 22;
    }
}
