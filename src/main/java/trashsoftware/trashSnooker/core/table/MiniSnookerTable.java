package trashsoftware.trashSnooker.core.table;

import trashsoftware.trashSnooker.core.GameValues;

public class MiniSnookerTable extends AbstractSnookerTable {

    public MiniSnookerTable() {
        super(GameValues.MINI_SNOOKER_VALUES);
    }

    @Override
    public double breakLineX() {
        return gameValues.leftX + 635.0;
    }

    @Override
    public double breakArcRadius() {
        return 219.0;  // todo: 存疑
    }

    @Override
    public double[] blackBallPos() {
        return new double[]{gameValues.rightX - 243.0, gameValues.midY};  // todo: 存疑
    }
}
