package trashsoftware.trashSnooker.core;

public class PotAttempt {

    private final GameType gameType;
    private final PlayerPerson playerPerson;
    private final Ball targetBall;
    private final double[] cueBallOrigPos;
    private final double[] targetBallOrigPos;
    private final double[] targetedHole;
    private boolean success;

    public PotAttempt(GameType gameType, PlayerPerson playerPerson,
                      Ball targetBall,
                      double[] cueBallOrigPos, double[] targetBallOrigPos,
                      double[] targetedHole) {
        this.gameType = gameType;
        this.playerPerson = playerPerson;
        this.targetBall = targetBall;
        this.cueBallOrigPos = cueBallOrigPos;
        this.targetBallOrigPos = targetBallOrigPos;
        this.targetedHole = targetedHole;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public GameType getGameType() {
        return gameType;
    }

    public PlayerPerson getPlayerPerson() {
        return playerPerson;
    }

    public Ball getTargetBall() {
        return targetBall;
    }

    public double[] getCueBallOrigPos() {
        return cueBallOrigPos;
    }

    public double[] getTargetBallOrigPos() {
        return targetBallOrigPos;
    }

    public boolean isLongPot() {
        double whiteTargetDt = Math.hypot(
                targetBallOrigPos[0] - cueBallOrigPos[0],
                targetBallOrigPos[1] - cueBallOrigPos[1]
        );
        double targetHoleDt = Math.hypot(
                targetedHole[0] - targetBallOrigPos[0],
                targetedHole[1] - targetBallOrigPos[1]
        );
        double totalLength = whiteTargetDt + targetHoleDt;
        return totalLength >= gameType.gameValues.diagonalLength() * 0.6667;
    }
}
