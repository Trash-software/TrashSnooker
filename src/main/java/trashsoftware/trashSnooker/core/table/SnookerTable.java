package trashsoftware.trashSnooker.core.table;

import trashsoftware.trashSnooker.core.metrics.TableMetrics;

public class SnookerTable extends AbstractSnookerTable {

    public SnookerTable(TableMetrics tableMetrics) {
        super(tableMetrics);
    }

    @Override
    public int nBalls() {
        return 22;
    }
}
