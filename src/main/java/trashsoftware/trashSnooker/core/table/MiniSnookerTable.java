package trashsoftware.trashSnooker.core.table;

import trashsoftware.trashSnooker.core.TableMetrics;

public class MiniSnookerTable extends AbstractSnookerTable {

    public MiniSnookerTable(TableMetrics tableMetrics) {
        super(tableMetrics);
    }

    @Override
    public double breakLineX() {
        return tableMetrics.leftX + 635.0;
    }

    @Override
    public double breakArcRadius() {
        return 219.0;  // todo: 存疑
    }

    @Override
    public double[] blackBallPos() {
        return new double[]{tableMetrics.rightX - 243.0, tableMetrics.midY};  // todo: 存疑
    }

    @Override
    public int nBalls() {
        return 14;
    }
}
