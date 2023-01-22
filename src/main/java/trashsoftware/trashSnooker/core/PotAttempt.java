package trashsoftware.trashSnooker.core;

public class PotAttempt extends CueAttempt {

    private final GameValues gameValues;
    private final PlayerPerson playerPerson;
    private final Ball targetBall;
    private final double[] cueBallOrigPos;
    private final double[] targetBallOrigPos;
    private final double[] targetedHole;
    private Position positionSuccess = Position.NOT_SET;
    private PlayerPerson.HandSkill handSkill;

    public PotAttempt(GameValues gameValues, PlayerPerson playerPerson,
                      Ball targetBall,
                      double[] cueBallOrigPos, double[] targetBallOrigPos,
                      double[] targetedHole) {
        this.gameValues = gameValues;
        this.playerPerson = playerPerson;
        this.targetBall = targetBall;
        this.cueBallOrigPos = cueBallOrigPos;
        this.targetBallOrigPos = targetBallOrigPos;
        this.targetedHole = targetedHole;
    }

    public Position getPositionSuccess() {
        return positionSuccess;
    }

    public void setPositionSuccess(boolean positionSuccess) {
        this.positionSuccess = positionSuccess ? Position.SUCCESS : Position.FAILED;
    }

    public void setHandSkill(PlayerPerson.HandSkill handSkill) {
        this.handSkill = handSkill;
    }

    public boolean isRestPot() {
        return handSkill.hand == PlayerPerson.Hand.REST;
    }

    public GameRule getGameType() {
        return gameValues.rule;
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
        return totalLength >= gameValues.table.diagonalLength() * 0.6667;
    }

    public enum Position {
        NOT_SET,
        SUCCESS,
        FAILED
    }
}
