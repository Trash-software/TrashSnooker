package trashsoftware.trashSnooker.core.table;

import trashsoftware.trashSnooker.core.metrics.TableMetrics;

import java.util.ArrayList;
import java.util.List;

public class SidePocketTable extends NumberedBallTable {

    public SidePocketTable(TableMetrics tableMetrics) {
        super(tableMetrics);
    }

    @Override
    public int nBalls() {
        return 10;
    }
}
