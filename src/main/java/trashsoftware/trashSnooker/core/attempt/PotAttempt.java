package trashsoftware.trashSnooker.core.attempt;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.CuePlayParams;
import trashsoftware.trashSnooker.core.person.PlayerHand;
import trashsoftware.trashSnooker.core.person.PlayerPerson;
import trashsoftware.trashSnooker.core.ai.AttackChoice;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;

public class PotAttempt extends CueAttempt {

    private final GameValues gameValues;
    private final CuePlayParams cuePlayParams;
    private final PlayerPerson playerPerson;
    private final Ball targetBall;
//    private final double[] cueBallOrigPos;
//    private final double[] targetBallOrigPos;
//    private final double[][] targetDirHole;
    public final AttackChoice attackChoice;
    private Position positionSuccess = Position.NOT_SET;
    
    private PotAttempt positionToThis;  // 连续进攻中的上一杆

    public PotAttempt(CueType type,
                      GameValues gameValues,
                      CuePlayParams cuePlayParams,
                      PlayerPerson playerPerson,
                      Ball targetBall,
                      AttackChoice attackChoice) {
        super(type, cuePlayParams);
        
        this.gameValues = gameValues;
        this.cuePlayParams = cuePlayParams;
        this.playerPerson = playerPerson;
        this.targetBall = targetBall;
        this.attackChoice = attackChoice;
    }

    public Position getPositionSuccess() {
        return positionSuccess;
    }

    public void setPositionSuccess(boolean positionSuccess) {
        this.positionSuccess = positionSuccess ? Position.SUCCESS : Position.FAILED;
    }

    public void setPositionToThis(PotAttempt positionToThis) {
        this.positionToThis = positionToThis;
    }

    public PotAttempt getPositionToThis() {
        return positionToThis;
    }

    public boolean isRestPot() {
        return cuePlayerHand.playerHand.hand == PlayerHand.Hand.REST;
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
        return attackChoice.getWhitePos();
    }

    public double[] getTargetBallOrigPos() {
        return attackChoice.getTargetOrigPos();
    }

    public double[][] getTargetDirHole() {
        return attackChoice instanceof AttackChoice.DirectAttackChoice ?
                ((AttackChoice.DirectAttackChoice) attackChoice).getDirHole() : null;
    }

    public CuePlayParams getCuePlayParams() {
        return cuePlayParams;
    }
    
    public boolean isDoubleShot() {
        return attackChoice instanceof AttackChoice.DoubleAttackChoice;
    }
    
    public boolean isEasyShot() {
        double[][] targetDirHole = getTargetDirHole();
        if (targetDirHole == null) {
            // todo: 是翻袋
            return false;
        }
        return attackChoice.getDefaultRef().getPotProb() >= 0.9;
    }
    
    public boolean isDifficultShot() {
        if (attackChoice instanceof AttackChoice.DoubleAttackChoice) return true;
        return attackChoice.getDefaultRef().getPotProb() < 0.5;
    }

    public boolean isLongPot() {
        double[][] targetDirHole = getTargetDirHole();
        if (targetDirHole == null) {
            // todo: 是翻袋
            return false;
        }
        double[] targetBallOrigPos = getTargetBallOrigPos();
        double[] cueBallOrigPos = getCueBallOrigPos();
        
        double whiteTargetDt = Math.hypot(
                targetBallOrigPos[0] - cueBallOrigPos[0],
                targetBallOrigPos[1] - cueBallOrigPos[1]
        );
        double targetHoleDt = Math.hypot(
                targetDirHole[1][0] - targetBallOrigPos[0],
                targetDirHole[1][1] - targetBallOrigPos[1]
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
