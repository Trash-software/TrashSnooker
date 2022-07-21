package trashsoftware.trashSnooker.core.table;

import trashsoftware.trashSnooker.core.GameValues;

public class SnookerTable extends AbstractSnookerTable {

    SnookerTable() {
        super(GameValues.SNOOKER_VALUES);
    }

    @Override
    public double breakLineX() {
        return gameValues.leftX + 737.0;
    }

    @Override
    public double breakArcRadius() {
        return 292.0;
    }

    @Override
    public double[] blackBallPos() {
        return new double[]{gameValues.rightX - 324.0, gameValues.midY};
    }

    @Override
    public int nBalls() {
        return 22;
    }
}
