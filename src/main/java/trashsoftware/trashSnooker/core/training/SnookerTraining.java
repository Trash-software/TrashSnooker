package trashsoftware.trashSnooker.core.training;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.GamePlayStage;
import trashsoftware.trashSnooker.core.GameSettings;
import trashsoftware.trashSnooker.core.snooker.SnookerBall;
import trashsoftware.trashSnooker.core.snooker.SnookerGame;

public class SnookerTraining extends SnookerGame implements Training {

    protected final TrainType trainType;
    protected final Challenge challenge;

    public SnookerTraining(EntireGame entireGame,
                           GameSettings gameSettings,
                           TrainType trainType,
                           Challenge challenge) {
        super(entireGame, gameSettings, 1);

        this.trainType = trainType;
        this.challenge = challenge;

        prepareSnake();
    }

    private void prepareSnake() {
        switch (trainType) {
            case SNAKE_FULL:
                moveToFullSnake();
                break;
            case SNAKE_HALF:
                moveToHalfSnake();
                break;
            case SNAKE_CROSS:
                moveToCrossSnake();
                break;
            case SNAKE_FULL_DENSE:
                moveToDenseSnake();
                break;
            case SNAKE_X:
                moveToXSnake();
                break;
            case CLEAR_COLOR:
                moveToClearColor();
                break;
        }
    }

    private void moveToHalfSnake() {
        moveToFullSnake();
        for (int i = 9; i < 15; i++) {
            redBalls[i].pot();
        }
    }

    private void moveToFullSnake() {
        double y = gameValues.table.midY;
        double blackX = getTable().blackBallPos()[0];
        double pinkX = getTable().pinkBallPos()[0];
        double blackPinkGap = (blackX - pinkX) / 4;
        redBalls[0].setXY(blackX + blackPinkGap, y);
        for (int i = 0; i < 3; i++) {
            redBalls[i + 1].setXY(blackX - blackPinkGap * (i + 1), y);
        }

        double blueX = getTable().blueBallPos()[0];
        double pinkBlueGap = (pinkX - blueX) / 6;
        for (int i = 0; i < 5; i++) {
            redBalls[i + 4].setXY(pinkX - pinkBlueGap * (i + 1), y);
        }

        double brownX = getTable().breakLineX();
        double blueBrownGap = (blueX - brownX) / 7;
        for (int i = 0; i < 6; i++) {
            redBalls[i + 9].setXY(blueX - blueBrownGap * (i + 1), y);
        }
    }

    private void moveToCrossSnake() {
        moveToDenseSnake();

        double midY = gameValues.table.midY;
        double blackX = getTable().blackBallPos()[0];
        double pinkX = getTable().pinkBallPos()[0];
        double yGap = (blackX - pinkX) / 6;
        for (int i = 0; i < 4; i++) {
            redBalls[i + 7].setXY(pinkX, midY + (i + 2) * yGap);
        }
        for (int i = 0; i < 4; i++) {
            redBalls[i + 11].setXY(pinkX, midY - (i + 2) * yGap);
        }
    }
    
    private void moveToClearColor() {
        for (SnookerBall ball : redBalls) {
            ball.pot();
        }
        currentTarget = 2;
    }
    
    private void moveToDenseSnake() {
        double y = gameValues.table.midY;
        double blackX = getTable().blackBallPos()[0];
        double pinkX = getTable().pinkBallPos()[0];
        double blackPinkGap = (blackX - pinkX) / 6;

        for (int i = 0; i < 2; i++) {
            redBalls[i].setXY(blackX + blackPinkGap * (i + 1), y);
        }
        for (int i = 0; i < 5; i++) {
            redBalls[i + 2].setXY(pinkX + blackPinkGap * (i + 1), y);
        }

        double blueX = getTable().blueBallPos()[0];
        double pinkBlueGap = (pinkX - blueX) / 9;
        for (int i = 0; i < 8; i++) {
            redBalls[i + 7].setXY(blueX + pinkBlueGap * (i + 1), y);
        }
    }

    private void moveToXSnake() {

    }

    @Override
    protected void endMoveAndUpdate() {
        super.endMoveAndUpdate();

        if (getChallenge() != null && !lastPotSuccess) {
            player1.withdraw();
            end();
        }
    }

    @Override
    public TrainType getTrainType() {
        return trainType;
    }

    @Override
    public Challenge getChallenge() {
        return challenge;
    }

    @Override
    public GamePlayStage getGamePlayStage(Ball predictedTargetBall, boolean printPlayStage) {
        return GamePlayStage.NORMAL;
    }

    @Override
    protected boolean canPlaceWhiteInTable(double x, double y) {
        return !isOccupied(x, y);
    }

    @Override
    protected int numRedBalls() {
        return 15;
//        switch (trainType) {
//            case SNAKE_CROSS:
//            case SNAKE_FULL_DENSE:
//            case SNAKE_FULL:
//                return 15;
//            case SNAKE_HALF:
//                return 8;
//            default:
//                throw new RuntimeException("Unsupported !");
//        }
    }
}
