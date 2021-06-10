package trashsoftware.trashSnooker.core;

public class ChineseEightPlayer extends Player {
    private int ballRange = 0;  // 0=未选球，8=8，16=1~7，17=9~15

    public ChineseEightPlayer(int number) {
        super(number);
    }

    public void setBallRange(int ballRange) {
        this.ballRange = ballRange;
    }

    public int getBallRange() {
        return ballRange;
    }
}
