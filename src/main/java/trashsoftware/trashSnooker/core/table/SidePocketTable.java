package trashsoftware.trashSnooker.core.table;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.GameHolder;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;

import java.util.ArrayList;
import java.util.List;

public class SidePocketTable extends NumberedBallTable {

    public SidePocketTable(TableMetrics tableMetrics) {
        super(tableMetrics);
    }

//    @Override
//    public int nBalls() {
//        return 10;
//    }
    
    public static List<PoolBall> getRemBalls(GameHolder holder) {
        List<PoolBall> rems = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            Ball poolBall = holder.getBallByValue(i);
            if (!poolBall.isPotted()) {
                rems.add((PoolBall) poolBall);
            }
        }
        return rems;
    }
}
