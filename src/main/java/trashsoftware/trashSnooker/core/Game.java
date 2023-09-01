package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.ai.AiCue;
import trashsoftware.trashSnooker.core.ai.AiCueResult;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.metrics.*;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.movement.MovementFrame;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.LisEightGame;
import trashsoftware.trashSnooker.core.numberedGames.nineBall.AmericanNineBallGame;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.core.snooker.MiniSnookerGame;
import trashsoftware.trashSnooker.core.snooker.SnookerGame;
import trashsoftware.trashSnooker.core.table.Table;
import trashsoftware.trashSnooker.core.training.PoolTraining;
import trashsoftware.trashSnooker.core.training.SnookerTraining;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.recorder.GameRecorder;
import trashsoftware.trashSnooker.recorder.InvalidRecorder;
import trashsoftware.trashSnooker.recorder.NaiveActualRecorder;
import trashsoftware.trashSnooker.util.Util;
import trashsoftware.trashSnooker.util.db.DBAccess;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

public abstract class Game<B extends Ball, P extends Player> implements GameHolder, Cloneable {
    public static final int END_REP = 31;
    public static final int CONGESTION_LIMIT = 30000;
    //    public static final double calculateMs = 1.0;
//    public static final double calculationsPerSec = 1000.0 / calculateMs;
//    public static final double calculationsPerSecSqr = calculationsPerSec * calculationsPerSec;
//    public static final double speedReducer = 120.0 / calculationsPerSecSqr;
//    public static final double spinReducer = 4000.0 / calculationsPerSecSqr;  // 数值越大，旋转衰减越大
//    public static final double sideSpinReducer = 100.0 / calculationsPerSecSqr;
//    public static final double spinEffect = 1400.0 / calculateMs;  // 数值越小影响越大
    // 进攻球判定角
    // 如实际角度与可通过的袋口连线的夹角小于该值，判定为进攻球
    public static final double MAX_ATTACK_DECISION_ANGLE = Math.toRadians(5.0);
    public static final double MIN_PLACE_DISTANCE = 0.0;  // 防止物理运算卡bug
    public static final double MIN_GAP_DISTANCE = 3.0;
    public final long frameStartTime = System.currentTimeMillis();
    public final int frameIndex;
    protected final Set<B> newPotted = new HashSet<>();
    protected final Set<Ball> newPottedLegal = new HashSet<>();
    //    protected final GameView parent;
    protected final EntireGame entireGame;
    protected final Map<B, double[]> recordedPositions = new HashMap<>();  // 记录上一杆时球的位置，复位用
    protected final GameSettings gameSettings;
    protected final ResourceBundle strings = App.getStrings();
    private final Map<B, int[]> ballCushionCountAndCrossLine = new HashMap<>();  // 本杆的{库数, 过线次数}
    protected B cueBall;
    protected P player1;
    protected P player2;
    protected int recordedTarget;  // 记录上一杆时的目标球，复位用
    protected boolean wasDoingFreeBall;  // 记录上一杆是不是自由球，复位用
    protected int finishedCuesCount = 0;  // 击球的计数器
    protected double lastCueVx;
    protected P lastCuedPlayer;
    protected P currentPlayer;
    /**
     * {@link Game#getCurrentTarget()}
     */
    protected int currentTarget;
    protected Ball whiteFirstCollide;  // 这一杆白球碰到的第一颗球
    protected boolean collidesWall;
    protected boolean ballInHand = true;
    //    protected boolean thisCueFoul = false;
//    protected boolean lastCueFoul = false;
    protected FoulInfo thisCueFoul = new FoulInfo();
    protected FoulInfo lastCueFoul;
    protected boolean lastPotSuccess;
    protected boolean isBreaking = true;
    protected boolean placedHandBallButNoHit;
    protected boolean firstCueAfterHandBall;
    protected GameValues gameValues;
    //    protected String foulReason;
    protected int thinkTime;
    protected long cueFinishTime;
    protected long cueStartTime;
    protected GameRecorder recorder;
    protected Map<Integer, B> numberBallMap;
    protected B[] allBalls;
    protected Table table;
    private boolean ended;
    private PhysicsCalculator physicsCalculator;
    protected FrameAchievementRecorder achievementRecorder = new FrameAchievementRecorder();

    protected Game(EntireGame entireGame,
                   GameSettings gameSettings, GameValues gameValues,
                   Table table,
                   int frameIndex) {
//        this.parent = parent;
        this.entireGame = entireGame;
        this.gameValues = gameValues;
        this.gameSettings = gameSettings;
        this.frameIndex = frameIndex;
        this.table = table;

        if (gameSettings != null) {
            initPlayers();
            currentPlayer = gameSettings.isPlayer1Breaks() ? player1 : player2;
            lastCuedPlayer = currentPlayer;  // 这里不重要，只要是防null
            setBreakingPlayer(currentPlayer);
        }
        cueBall = createWhiteBall();
    }

    public static Game<? extends Ball, ? extends Player> createGame(
            GameSettings gameSettings,
            GameValues gameValues,
            EntireGame entireGame) {
        int frameIndex = entireGame == null ? 1 : (entireGame.getP1Wins() + entireGame.getP2Wins() + 1);
        Game<? extends Ball, ? extends Player> game;
        if (gameValues.rule == GameRule.SNOOKER) {
            if (gameValues.isTraining()) {
                game = new SnookerTraining(entireGame, gameSettings, gameValues,
                        gameValues.getTrainType(),
                        gameValues.getTrainChallenge());
            } else {
                game = new SnookerGame(entireGame, gameSettings, gameValues, frameIndex);
            }
        } else if (gameValues.rule == GameRule.MINI_SNOOKER) {
            if (gameValues.isTraining()) {
                game = new SnookerTraining(entireGame, gameSettings, gameValues,
                        gameValues.getTrainType(),
                        gameValues.getTrainChallenge());
            } else {
                game = new MiniSnookerGame(entireGame, gameSettings, gameValues, frameIndex);
            }
        } else if (gameValues.rule == GameRule.CHINESE_EIGHT) {
            if (gameValues.isTraining()) {
                game = new PoolTraining(entireGame, gameSettings, gameValues, gameValues.getTrainType(), gameValues.getTrainChallenge());
            } else {
                game = new ChineseEightBallGame(entireGame, gameSettings, gameValues, frameIndex);
            }
        } else if (gameValues.rule == GameRule.LIS_EIGHT) {
            if (gameValues.isTraining()) {
                game = new PoolTraining(entireGame, gameSettings, gameValues, gameValues.getTrainType(), gameValues.getTrainChallenge());
            } else {
                game = new LisEightGame(entireGame, gameSettings, gameValues, frameIndex);
            }
        } else if (gameValues.rule == GameRule.AMERICAN_NINE) {
            if (gameValues.isTraining()) {
                game = new PoolTraining(entireGame, gameSettings, gameValues, gameValues.getTrainType(), gameValues.getTrainChallenge());
            } else {
                game = new AmericanNineBallGame(entireGame, gameSettings, gameValues, frameIndex);
            }
        } else throw new RuntimeException("Unexpected game rule " + gameValues.rule);

        try {
            if (DBAccess.RECORD && entireGame != null) {
                game.recorder = new NaiveActualRecorder(game, entireGame.getMetaMatchInfo());
            } else {
                game.recorder = new InvalidRecorder();
            }
            game.recorder.startRecoding();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return game;
    }

    public static double frameImportance(int playerNum, int totalFrames,
                                         int p1Wins, int p2Wins, GameRule gameRule) {
        if (totalFrames < 3) return 0;
        double divisor;
        if (gameRule == GameRule.SNOOKER) {
            divisor = 11.0;
        } else {
            divisor = 17.0;
        }

        double tfPrice = Algebra.shiftRange(
                0, 1,
                0.3, 1,
                Math.min(1, totalFrames / divisor)
        );
        int half = totalFrames / 2;
        if (p1Wins == half) {
            if (p2Wins == half) {
                // 决胜局
                return tfPrice;
            } else if (playerNum == 1) {
                // 赛点局
                return tfPrice / 2;
            }
        }
        if (p2Wins == half) {
            if (playerNum == 2) {
                // 赛点局
                return tfPrice / 2;
            }
        }
        return 0;
    }

    protected abstract void cloneBalls(B[] allBalls);

    @Override
    public Game<B, P> clone() {
        try {
            Game<B, P> copy = (Game<B, P>) super.clone();

            copy.cloneBalls(allBalls);

            for (B ball : copy.getAllBalls()) {
                if (ball.isWhite()) {
                    copy.cueBall = ball;
                    break;
                }
            }

            copy.numberBallMap = null;
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public EntireGame getEntireGame() {
        return entireGame;
    }

    public abstract GameRule getGameType();

    public Table getTable() {
        return table;
    }

    protected void setBreakingPlayer(Player breakingPlayer) {
    }

    protected abstract void initPlayers();

    protected abstract B createWhiteBall();

    protected abstract AiCue<?, ?> createAiCue(P aiPlayer);

    public abstract ScoreResult makeScoreResult(Player justCuedPlayer);

    /**
     * 返回所有能打的球
     */
    public final List<Ball> getAllLegalBalls(int targetRep, boolean isSnookerFreeBall, boolean isLineInFreeBall) {
        List<Ball> balls = new ArrayList<>();
        for (Ball ball : getAllBalls()) {
            if (!ball.isNotOnTable() &&
                    !ball.isWhite() &&
                    isLegalBall(ball, targetRep, isSnookerFreeBall, isLineInFreeBall))
                balls.add(ball);
        }
        return balls;
    }

    public abstract boolean isLegalBall(Ball ball, int targetRep, boolean isSnookerFreeBall, boolean isInLineHandBall);

    /**
     * 返回目标的价值，前提条件：ball是有效目标
     *
     * @param lastPotting 如果这杆为走位预测，则该值为AI第一步想打的球。如这杆就是第一杆，则为null
     */
    public abstract double priceOfTarget(int targetRep, Ball ball, Player attackingPlayer,
                                         Ball lastPotting);

    public boolean isDoingSnookerFreeBll() {
        return false;
    }

    public Set<B> getNewPotted() {
        return newPotted;
    }

    public GameValues getGameValues() {
        return gameValues;
    }

    public GameRecorder getRecorder() {
        return recorder;
    }
    
    public void abortRecording() {
        getRecorder().abort();
        recorder = new InvalidRecorder();
    }
    
    public void recordAttemptForAchievement(CueAttempt finishedAttempt, Player player) {
        if (finishedAttempt instanceof PotAttempt potAttempt) {
            AiCue.AttackChoice attackChoice = AiCue.AttackChoice.createChoice(
                    this,
                    entireGame.predictPhy, 
                    player,
                    potAttempt.getCueBallOrigPos(),
                    potAttempt.getTargetBall(),
                    null,
                    getCurrentTarget(),
                    false,
                    potAttempt.getTargetDirHole(),
                    potAttempt.getTargetBallOrigPos()
            );
            if (attackChoice != null) {
                CuePlayParams cpp = potAttempt.getCuePlayParams();
                AiCue.AttackParam attackParam = new AiCue.AttackParam(
                        attackChoice,
                        this,
                        entireGame.predictPhy,
                        cpp.cueParams
                );
                System.out.println("Attack pot prob: " + attackParam.getPotProb());
                if (attackParam.getPotProb() > 0.95 && !finishedAttempt.isSuccess()) {
                    int playerIndex = player.getInGamePlayer().getPlayerNumber() - 1;
                    int failed = ++achievementRecorder.easyBallFails[playerIndex];
                    
                    if (failed >= 3) {
                        AchManager.getInstance().addAchievement(Achievement.MULTIPLE_EASY_FAILS, player.getInGamePlayer());
                    }
                }
            } else {
                System.out.println("Attack is null. Why?");
            }
        } else if (finishedAttempt instanceof DefenseAttempt defenseAttempt) {
            
        } 
    }

    public Movement cue(CuePlayParams params, Phy phy) {
        cueStartTime = System.currentTimeMillis();
        if (cueFinishTime != 0) thinkTime = (int) (cueStartTime - cueFinishTime);

        System.out.println("Think time: " + thinkTime);
        if (thinkTime >= 60000) {
            AchManager.getInstance().addAchievement(Achievement.LONG_THINK, getCuingIgp());
        }

        whiteFirstCollide = null;
        collidesWall = false;
        lastPotSuccess = false;
        lastCueFoul = thisCueFoul;
        thisCueFoul = new FoulInfo();
        clearCushionAndAcrossLine();
        newPotted.clear();
        newPottedLegal.clear();
        recordPositions();
        lastCuedPlayer = currentPlayer;
        recordedTarget = currentTarget;
        wasDoingFreeBall = isDoingSnookerFreeBll();
        firstCueAfterHandBall = placedHandBallButNoHit;
        placedHandBallButNoHit = false;

        lastCueVx = params.vx;
        cueBall.setVx(params.vx / phy.calculationsPerSec);
        cueBall.setVy(params.vy / phy.calculationsPerSec);
        params.xSpin = params.xSpin == 0.0d ? params.vx / 1000.0 : params.xSpin;  // 避免完全无旋转造成的NaN
        params.ySpin = params.ySpin == 0.0d ? params.vy / 1000.0 : params.ySpin;
        cueBall.setSpin(
                params.xSpin / phy.calculationsPerSec,
                params.ySpin / phy.calculationsPerSec,
                params.sideSpin / phy.calculationsPerSec);
        return physicalCalculate(phy);
    }

    public AiCueResult aiCue(Player aiPlayer, Phy phy) {
        AiCue<?, ?> aiCue = createAiCue((P) aiPlayer);
        return aiCue.makeCue(phy);
    }

    /**
     * @param params                   击球参数
     * @param phy                      使用哪一个物理值
     * @param lengthAfterWall          直接碰库后白球预测线的长度
     * @param predictPath              是否预测撞击之后的线路
     * @param checkCollisionAfterFirst 是否检查白球打到目标球后是否碰到下一颗球
     * @param recordTargetPos          是否记录第一颗碰撞球的信息
     * @param wipe                     是否还原预测前的状态
     * @param useClone                 是否使用拷贝的白球，仅为多线程服务
     */
    public WhitePrediction predictWhite(CuePlayParams params,
                                        Phy phy,
                                        double lengthAfterWall,
                                        boolean predictPath,
                                        boolean checkCollisionAfterFirst,
                                        boolean recordTargetPos,
                                        boolean wipe,
                                        boolean useClone) {
        if (cueBall.isNotOnTable()) {
            return null;
        }
        Ball cueBallClone = useClone ? cueBall.clone() : cueBall;
        cueBallClone.setVx(params.vx / phy.calculationsPerSec);
        cueBallClone.setVy(params.vy / phy.calculationsPerSec);
        params.xSpin = params.xSpin == 0.0d ? params.vx / 1000.0 : params.xSpin;  // 避免完全无旋转造成的NaN
        params.ySpin = params.ySpin == 0.0d ? params.vy / 1000.0 : params.ySpin;
        cueBallClone.setSpin(
                params.xSpin / phy.calculationsPerSec,
                params.ySpin / phy.calculationsPerSec,
                params.sideSpin / phy.calculationsPerSec);
        WhitePredictor whitePredictor = new WhitePredictor(cueBallClone);
//        long st = System.currentTimeMillis();
        WhitePrediction prediction =
                whitePredictor.predict(phy, lengthAfterWall, predictPath, checkCollisionAfterFirst, recordTargetPos);
//        System.out.println("White prediction ms: " + (System.currentTimeMillis() - st));
//        cueBall.setX(whiteX);
//        cueBall.setY(whiteY);
//        cueBall.pickup();
        if (wipe) prediction.resetToInit();
//        System.out.println(canSeeBall(cueBall, getAllBalls()[20]));
        return prediction;
    }

    public void finishMove(GameView gameView) {
        System.out.println("Move end");
        physicsCalculator = null;
        cueFinishTime = System.currentTimeMillis();

        Player player = currentPlayer;
        endMoveAndUpdate();
        isBreaking = false;
        finishedCuesCount++;
        gameView.finishCue(player, currentPlayer);
    }

    public boolean isCalculating() {
        return physicsCalculator != null && physicsCalculator.notTerminated;
    }

    public void forcedTerminate() {
        if (physicsCalculator != null) {
            physicsCalculator.notTerminated = false;
            for (B ball : getAllBalls()) ball.clearMovement();
        }
    }
    
    public void validateBalls() {
        for (B ball : getAllBalls()) {
            if (!ball.isNotOnTable()) {
                // 把没进又没飞出台，又没在台内的球救回来
                // 这里假设了袋口的深度不会超过球的半径x2
                double moveX = 0;
                double moveY = 0;
                if (ball.x < gameValues.table.leftX - gameValues.ball.ballRadius) {
                    moveX = 1;
                } else if (ball.x > gameValues.table.rightX + gameValues.ball.ballRadius){
                    moveX = -1;
                }

                if (ball.y < gameValues.table.topY - gameValues.ball.ballRadius) {
                    moveY = 1;
                } else if (ball.y > gameValues.table.botY + gameValues.ball.ballRadius){
                    moveY = -1;
                }
                if (moveX != 0 || moveY != 0) {
                    double x = ball.x;
                    double y = ball.y;
                    do {
                        x += moveX;
                        y += moveY;
                    } while (isOccupied(x, y));
                    ball.setX(x);
                    ball.setY(y);
                }
            }
        }
    }

    public B getCueBall() {
        return cueBall;
    }

    public void quitGame() {
    }

    public void placeWhiteBall(double realX, double realY) {
        if (canPlaceWhite(realX, realY)) {
            cueBall.setX(realX);
            cueBall.setY(realY);
            cueBall.pickup();
            ballInHand = false;
            placedHandBallButNoHit = true;
        } else {
            System.out.printf("Position %f, %f cannot place cue ball \n", realX, realY);
        }
    }

    public boolean isInTable(double x, double y) {
//        return x >= gameValues.table.leftX + gameValues.ball.ballRadius &&
//                x < gameValues.table.rightX - gameValues.ball.ballRadius &&
//                y >= gameValues.table.topY + gameValues.ball.ballRadius &&
//                y < gameValues.table.botY - gameValues.ball.ballRadius;
        if (!gameValues.isInTable(x, y, gameValues.ball.ballRadius)) return false;
        
        for (Pocket pocket : gameValues.table.pockets) {
            // 在袋里
            if (Algebra.distanceToPoint(x, y, pocket.fallCenter[0], pocket.fallCenter[1]) < pocket.fallRadius + pocket.extraSlopeWidth) {
                return false;
            }
        }
        return true;
    }

    public final boolean canPlaceWhite(double x, double y) {
        return isInTable(x, y) &&
                canPlaceWhiteInTable(x, y);
    }

    protected abstract boolean canPlaceWhiteInTable(double x, double y);

    public final B[] getAllBalls() {
        return allBalls;
    }

    /**
     * 返回白球后方障碍物的距离与高度
     */
    public CueBackPredictor.Result getObstacleDtHeight(double cursorPointingX,
                                                       double cursorPointingY,
                                                       double cueWidth) {
//        long st = System.currentTimeMillis();
        CueBackPredictor cueBackPredictor =
                new CueBackPredictor(this, cursorPointingX, cursorPointingY, cueWidth,
                        getCuingPlayer().getInGamePlayer()
                                .getCurrentCue(this).getTotalLength() + 300.0,
                        cueBall.x, cueBall.y);
        // System.out.println("Cue back prediction time: " + (System.currentTimeMillis() - st));
        return cueBackPredictor.predict();
    }

    public PredictedPos getPredictedHitBall(double cueBallX, double cueBallY,
                                            double xUnitDirection, double yUnitDirection) {

        double dx = Values.PREDICTION_INTERVAL * xUnitDirection;
        double dy = Values.PREDICTION_INTERVAL * yUnitDirection;

        double x = cueBallX + dx;
        double y = cueBallY + dy;
        while (x >= gameValues.table.leftX &&
                x < gameValues.table.rightX &&
                y >= gameValues.table.topY &&
                y < gameValues.table.botY) {
            for (Ball ball : getAllBalls()) {
                if (!ball.isNotOnTable() && !ball.isWhite()) {
                    if (Algebra.distanceToPoint(
                            x, y, ball.x, ball.y
                    ) < gameValues.ball.ballDiameter) {
                        return new PredictedPos(ball, new double[]{x, y});
                    }
                }
            }
            x += dx;
            y += dy;
        }

        return null;
    }

    /**
     * Get ball by ball's number.
     */
    private Map<Integer, B> getNumberBallMap() {
        if (numberBallMap == null) {
            numberBallMap = new HashMap<>();
            for (B ball : getAllBalls()) {
                numberBallMap.put(ball.getValue(), ball);
            }
        }
        return numberBallMap;
    }

    public B getBallByValue(int number) {
        Map<Integer, B> map = getNumberBallMap();
        return map.get(number);
    }

    /**
     * @param checkFullBall 如true，检查是否能过全球；如false，检查是否有薄边
     * @param checkPotPoint 如true，检查进球点，因为进球点是个虚拟的球，不会碰撞
     * @return 两点之间能不能过球
     */
    public boolean pointToPointCanPassBall(double p1x, double p1y, double p2x, double p2y,
                                           Ball selfBall1, Ball selfBall2, boolean checkFullBall,
                                           boolean checkPotPoint) {

        double shadowRadius = checkFullBall ? gameValues.ball.ballDiameter : gameValues.ball.ballRadius;
//        double p2Radius = gameValues.ballRadius;

        double circle = Math.PI * 2;

        double xDiff0 = p2x - p1x;
        double yDiff0 = p2y - p1y;
        double pointsDt = Math.hypot(xDiff0, yDiff0);
        double angle = Algebra.thetaOf(xDiff0, yDiff0);

        double extraShadowAngle;
        if (checkFullBall) {
            extraShadowAngle = 0;
        } else {
            extraShadowAngle = -Math.asin(gameValues.ball.ballDiameter / pointsDt);
        }
//        System.out.printf("%f %f %f %f\n", p1x, p1y, p2x, p2y);

        for (Ball ball : getAllBalls()) {
            if (!ball.equals(selfBall1) && !ball.equals(selfBall2) && !ball.isNotOnTable()) {
                if (checkPotPoint) {
                    // 障碍球占用了目标点
                    double ballTargetDt = Math.hypot(ball.x - p2x, ball.y - p2y);
                    if (ballTargetDt <= gameValues.ball.ballDiameter) return false;
                }

                // 计算这个球遮住的角度范围
                double xDiff = ball.x - p1x;
                double yDiff = ball.y - p1y;
                double dt = Math.hypot(xDiff, yDiff);  // 两球球心距离
                if (dt > pointsDt) {
                    continue;  // 障碍球比目标远，不可能挡。这个算式不够精确，实际应结合角度计算三角函数
                }
                double connectionAngle = Algebra.thetaOf(xDiff, yDiff);  // 连线的绝对角度

                double ballRadiusAngle = Math.asin(shadowRadius / dt);  // 从selfBall看ball占的的角
//                if ()
//                // 起始球与障碍球切线长度
//                double tanDt = Math.sqrt(Math.pow(dt, 2) - Math.pow(gameValues.ballRadius, 2));
//                // 起始球自己的半径占的角度
//                double selfPassAngle = Math.asin(gameValues.ballRadius / tanDt);

                double left = connectionAngle + ballRadiusAngle + extraShadowAngle;
                double right = connectionAngle - ballRadiusAngle - extraShadowAngle;

                if (left > circle) {  // 连线中心小于360，左侧大于360
                    if (angle >= right) {
                        return false;  // angle在right与x正轴之间，挡住
                    } else if (angle <= left - circle) {
                        return false;
                    }
                } else if (right <= 0) {  // 连线右侧小于0
                    if (angle > circle + right) {
                        return false;  // angle在right以上x正轴之下，挡住
                    } else if (angle <= left) {
                        return false;
                    }
                }

                if (left >= angle && right <= angle) {
                    return false;
                }

            }
        }
        return true;

    }

    public SeeAble countSeeAbleTargetBalls(double whiteX, double whiteY,
                                           Collection<Ball> legalBalls, int situation) {
        return getAllSeeAbleBalls(whiteX, whiteY, legalBalls, situation, null);
    }

    public SeeAble getAllSeeAbleBalls(double whiteX, double whiteY,
                                      Collection<Ball> legalBalls, int situation,
                                      List<Ball> listToPut) {
        return getAllSeeAbleBalls(whiteX, whiteY, legalBalls, situation, listToPut, getAllBalls());
    }

    /**
     * @param situation                1:薄边 2:全球 3:缝隙能过全球（斯诺克自由球那种）
     * @param obstaclesInConsideration 参与考虑的可能障碍球。只有这里面的球会被考虑为障碍
     * @return 能看到的目标球数量
     */
    public SeeAble getAllSeeAbleBalls(double whiteX, double whiteY,
                                      Collection<Ball> legalBalls, int situation,
                                      List<Ball> listToPut,
                                      Ball[] obstaclesInConsideration) {
        if (situation != 1 && situation != 2 && situation != 3) {
            throw new RuntimeException("Unknown situation " + situation);
        }
        Set<Ball> legalSet;
        if (legalBalls instanceof Set) {
            legalSet = (Set<Ball>) legalBalls;
        } else {
            legalSet = new HashSet<>(legalBalls);
        }
        double shadowRadius = situation == 3 ?
                gameValues.ball.ballRadius :
                gameValues.ball.ballDiameter;
        int result = 0;

        double maxShadowAngle = 0.0;
        double sumTargetDt = 0.0;

        for (Ball target : legalBalls) {
            double xDiff0 = target.x - whiteX;
            double yDiff0 = target.y - whiteY;
            double whiteTarDt = Math.hypot(xDiff0, yDiff0);
            double angle = Algebra.thetaOf(xDiff0, yDiff0);
            sumTargetDt += whiteTarDt;

            double extraShadowAngle;
            if (situation == 1) {
                extraShadowAngle = -Math.asin(gameValues.ball.ballDiameter / whiteTarDt);
            } else if (situation == 2) {
                extraShadowAngle = 0;
            } else {
                extraShadowAngle = Math.asin(gameValues.ball.ballDiameter / whiteTarDt);  // 目标球本身的投影角
            }

            boolean canSee = true;

            for (Ball ball : obstaclesInConsideration) {
                if (!ball.isWhite() && !ball.isNotOnTable() && !legalSet.contains(ball)) {
                    // 是障碍球
                    double xDiff = ball.x - whiteX;
                    double yDiff = ball.y - whiteY;
                    double dt = Math.hypot(xDiff, yDiff);  // 两球球心距离
                    if (dt > whiteTarDt + gameValues.ball.ballRadius) {
                        continue;  // 障碍球比目标球远，不可能挡
                    }
                    double connectionAngle = Algebra.thetaOf(xDiff, yDiff);  // 连线的绝对角度

                    double ballShadowAngle = Math.asin(shadowRadius / dt);  // 从selfBall看ball占的的角

                    double selfPassAngle = 0;
                    if (situation == 3) {
                        // 起始球与障碍球切线长度
                        double tanDt = Math.sqrt(Math.pow(dt, 2) - Math.pow(gameValues.ball.ballRadius, 2));
                        // 起始球自己的半径占的角度
                        selfPassAngle = Math.asin(gameValues.ball.ballRadius / tanDt);
                    }
                    double left = connectionAngle + selfPassAngle + ballShadowAngle + extraShadowAngle;
                    double right = connectionAngle - selfPassAngle - ballShadowAngle - extraShadowAngle;
//                    System.out.printf("%f, Ball %s, %f %f %f\n", angle, ball, left, connectionAngle, right);

                    if (left > Algebra.TWO_PI) {  // 连线中心小于360，左侧大于360
                        if (angle >= right) {
                            canSee = false;  // angle在right与x正轴之间，挡住
//                            System.out.println(ball + " obstacle 1");
                        } else if (angle <= left - Algebra.TWO_PI) {
                            canSee = false;  // angle在x正轴与left之间，挡住
//                            System.out.println(ball + " obstacle 1.1");
                        }
                    } else if (right < 0) {  // 连线右侧小于0
                        if (angle >= Algebra.TWO_PI + right) {
                            canSee = false;  // angle在right以上x正轴之下，挡住
//                            System.out.println(ball + " obstacle 2");
                        } else if (angle <= left) {
                            canSee = false;  // angle在x正轴以上left之下，挡住
//                            System.out.println(ball + " obstacle 2.1");
                        }
                    }
                    if (canSee && left >= angle && right <= angle) {
                        canSee = false;
//                        System.out.println(ball + " obstacle 3");
                    }
                    if (!canSee) {
                        if (maxShadowAngle < ballShadowAngle) {
                            maxShadowAngle = ballShadowAngle;
                        }
                    }
                }
            }
            if (canSee) {
                if (listToPut != null) listToPut.add(target);
                result++;
            }
        }
        return new SeeAble(
                result,
                sumTargetDt / legalBalls.size(),
                maxShadowAngle);
    }

    public int getPlayerNum(P player) {
        return player == player1 ? 1 : 2;
    }

    /**
     * 返回
     * {
     * 目标球与"从目标球处能直接看到的洞口"的连线的单位向量,
     * 洞口进球坐标(注意: 只有对于袋口球来说是洞底坐标),
     * 进球碰撞点坐标
     * }。
     */
    public List<double[][]> directionsToAccessibleHoles(Ball targetBall) {
        List<double[][]> list = new ArrayList<>();
        double x = targetBall.x;
        double y = targetBall.y;
//        BIG_LOOP:
        for (int i = 0; i < 6; i++) {
            double[] holeOpenCenter = gameValues.allHoleOpenCenters[i];
//            double[] holeBottom = gameValues.table.allHoles[i];
            Pocket pocket = gameValues.table.pockets[i];
            if (!pocket.isMid &&
                    Algebra.distanceToPoint(x, y, holeOpenCenter[0], holeOpenCenter[1]) < gameValues.ball.ballRadius &&
                    pointToPointCanPassBall(x, y, pocket.fallCenter[0], pocket.fallCenter[1], targetBall,
                            null, true, true)) {
                // 目标球离袋口瞄球点太近了，转而检查真正的袋口
                double directionX = pocket.fallCenter[0] - x;
                double directionY = pocket.fallCenter[1] - y;
                double[] unitXY = Algebra.unitVector(directionX, directionY);
                double collisionPointX = x - gameValues.ball.ballDiameter * unitXY[0];
                double collisionPointY = y - gameValues.ball.ballDiameter * unitXY[1];

                list.add(new double[][]{unitXY, pocket.fallCenter, new double[]{collisionPointX, collisionPointY}});
            } else if (pointToPointCanPassBall(x, y, holeOpenCenter[0], holeOpenCenter[1], targetBall,
                    null, true, true)) {
                double directionX = holeOpenCenter[0] - x;
                double directionY = holeOpenCenter[1] - y;
                double[] unitXY = Algebra.unitVector(directionX, directionY);
                double collisionPointX = x - gameValues.ball.ballDiameter * unitXY[0];
                double collisionPointY = y - gameValues.ball.ballDiameter * unitXY[1];

                list.add(new double[][]{unitXY, holeOpenCenter, new double[]{collisionPointX, collisionPointY}});
            }
        }
        return list;
    }

    public boolean isBallInHand() {
        return ballInHand;
    }

    public boolean isBreaking() {
        return isBreaking;
    }

    public boolean isJustAfterBreak() {
        return finishedCuesCount == 1;
    }

    public void setBallInHand() {
        ballInHand = true;
        cueBall.pot();
    }

    public boolean isSnookered() {
        return isSnookered(cueBall.x, cueBall.y,
                getAllLegalBalls(getCurrentTarget(), isDoingSnookerFreeBll(),
                        isInLineHandBall()));
    }

    public boolean isSnookered(double whiteX, double whiteY, List<Ball> legalBalls) {
        return countSeeAbleTargetBalls(whiteX, whiteY, legalBalls, 1).seeAbleTargets == 0;
    }

    public boolean isAnyFullBallVisible() {
        return countSeeAbleTargetBalls(
                cueBall.x, cueBall.y,
                getAllLegalBalls(getCurrentTarget(), isDoingSnookerFreeBll(),
                        isInLineHandBall()),
                2)
                .seeAbleTargets != 0;
    }

    public Movement collisionTest() {
        Ball ball1 = getCueBall();
        Ball ball2 = getAllBalls()[1];

        ball1.pickup();
        ball2.pickup();

        ball1.setX(2200);
        ball1.setY(1500);

        ball1.setVx(0.5);
        ball1.setVy(0.05);
//        ball1.setSpin(-10, -1, 0);

        ball2.setX(2250);
        ball2.setY(1350);

        ball2.setVx(0.4);
        ball2.setVy(0.3);

        return physicalCalculate(entireGame.playPhy);
    }

    public void tieTest() {
    }

    public void clearRedBallsTest() {
//        for (int i = 0; i < 14; ++i) {
//            redBalls[i].pot();
//        }
    }

    public boolean isEnded() {
        return ended;
    }

    /**
     * 返回这局球的重要性，0为不重要，1为极重要（如世锦赛决赛决胜局）
     */
    public double frameImportance(int playerNum) {
        return frameImportance(playerNum,
                entireGame.getTotalFrames(),
                entireGame.getP1Wins(),
                entireGame.getP2Wins(),
                gameValues.rule);
    }

    /**
     * @return 此局是否为>=三局两胜的决胜局
     */
    public boolean isFinalFrame() {
        int tf = entireGame.getTotalFrames();
        return tf > 1 &&
                entireGame.getP1Wins() == tf / 2 &&
                entireGame.getP2Wins() == tf / 2;
    }

    /**
     * @return 是否放置了手中球但是没打
     */
    public boolean isPlacedHandBallButNoHit() {
        return placedHandBallButNoHit;
    }

    public boolean isFirstCueAfterHandBall() {
        return firstCueAfterHandBall;
    }

    public abstract Player getWiningPlayer();

    public P getCuingPlayer() {
        return currentPlayer;
    }

    public P getLastCuedPlayer() {
        return lastCuedPlayer;
    }

    @Override
    public InGamePlayer getCuingIgp() {
        return currentPlayer.inGamePlayer;
    }

    public P getPlayer1() {
        return player1;
    }

    public P getPlayer2() {
        return player2;
    }

    protected int[] computeCushionAndAcrossLineOfBall(B ball) {
        return ballCushionCountAndCrossLine.computeIfAbsent(ball, b -> new int[2]);
    }

    private void recordHitCushion(B ball) {
        computeCushionAndAcrossLineOfBall(ball)[0]++;
    }

    private void recordCrossLine(B ball) {
        computeCushionAndAcrossLineOfBall(ball)[1]++;
    }

    private void clearCushionAndAcrossLine() {
        for (Map.Entry<B, int[]> entry : ballCushionCountAndCrossLine.entrySet()) {
            Arrays.fill(entry.getValue(), 0);
        }
    }

    public boolean canReposition() {
        return gameValues.rule.hasRule(Rule.FOUL_AND_MISS) && thisCueFoul.isFoul() && thisCueFoul.isMiss();
    }

    public void notReposition() {
    }

    public void reposition() {
        reposition(false);
    }

    protected void reposition(boolean isSimulate) {
        if (!isSimulate) System.out.println("Reposition!");
        thisCueFoul = new FoulInfo();
        ballInHand = false;
        for (Map.Entry<B, double[]> entry : recordedPositions.entrySet()) {
            B ball = entry.getKey();
            ball.setX(entry.getValue()[0]);
            ball.setY(entry.getValue()[1]);
            if (ball.isPotted()) ball.pickup();
        }
        switchPlayer();
        currentTarget = recordedTarget;
    }

    protected boolean checkStandardFouls(Supplier<Integer> foulScoreCalculator) {
        GameRule rule = gameValues.rule;
        if (whiteFirstCollide == null) {
            // 没打到球，除了白球也不可能有球进，白球进不进也无所谓，分都一样
            thisCueFoul.addFoul(strings.getString("emptyCue"), foulScoreCalculator.get(), true);
            if (cueBall.isPotted()) ballInHand = true;
            AchManager.getInstance().addAchievement(Achievement.MISSED_SHOT, getCuingIgp());
        }
        if (cueBall.isPotted()) {
            thisCueFoul.addFoul(strings.getString("cueBallPot"), foulScoreCalculator.get(), false);
            ballInHand = true;
            AchManager.getInstance().addAchievement(Achievement.CUE_BALL_POT, getCuingIgp());
        }
        if (rule.hasRule(Rule.HIT_CUSHION)) {
            if (newPotted.isEmpty() && !collidesWall) {
                thisCueFoul.addFoul(strings.getString("noBallHitCushion"), foulScoreCalculator.get(), false);
            }
        }
        for (B ball : getAllBalls()) {
            if (!ball.isPotted()) {
                if (ball.isOutOfTable()) {
                    thisCueFoul.addFoul(strings.getString("flyOutTable"), foulScoreCalculator.get(), false);
                    ball.pot();
                }
            }
        }
        return thisCueFoul.isFoul();
    }

    /**
     * 斯诺克：
     * 任意彩球=0，特定球=value
     * <p>
     * 十六球：
     * 0=任意，1-15=value，16=黑八（小），17=黑八（大）
     */
    public int getCurrentTarget() {
        return currentTarget;
    }

    public abstract int getTargetAfterPotSuccess(Ball pottingBall, boolean isSnookerFreeBall);

    public abstract int getTargetAfterPotFailed();

    public boolean isInLineHandBall() {
        return false;
    }

    public boolean isInLineHandBallForAi() {
        return false;
    }

    public void withdraw(Player player) {
        player.withdraw();
        end();
    }

    public Ball getEasiestTarget(Player player) {
        int target = getCurrentTarget();
        List<Ball> targets = getAllLegalBalls(target,
                isDoingSnookerFreeBll(),
                isInLineHandBall());

        if (targets.isEmpty()) return null;

        double[] whitePos = new double[]{cueBall.x, cueBall.y};

        List<AiCue.AttackChoice> attackChoices = new ArrayList<>();

        for (Ball ball : targets) {
            List<double[][]> dirHoles = directionsToAccessibleHoles(ball);

            for (double[][] dirHole : dirHoles) {
                double collisionPointX = dirHole[2][0];
                double collisionPointY = dirHole[2][1];

                if (pointToPointCanPassBall(whitePos[0], whitePos[1],
                        collisionPointX, collisionPointY, getCueBall(), ball, true,
                        true)) {
                    // 从白球处看得到进球点
                    AiCue.AttackChoice attackChoice = AiCue.AttackChoice.createChoice(
                            this,
                            entireGame.predictPhy,
                            player,
                            whitePos,
                            ball,
                            null,
                            target,
                            false,
                            dirHole,
                            null
                    );
                    if (attackChoice != null) {
                        attackChoices.add(attackChoice);
                    }
                }
            }
        }
        if (!attackChoices.isEmpty()) {
            // 如果有进攻机会，就返回最简单的那颗球
            Collections.sort(attackChoices);
            AiCue.AttackChoice easiest = attackChoices.get(0);
//            double[] tole = easiest.leftRightTolerance();
//            System.out.println("Tolerance: left " + tole[0] + ", right " + tole[1]);
            return easiest.getBall();
        }

        // 如果没有，就找出最近的能看见的球
        List<Ball> seeAbleBalls = new ArrayList<>();
        getAllSeeAbleBalls(cueBall.x, cueBall.y,
                targets, 2, seeAbleBalls);

        Ball closet = null;
        double closetDt = Double.MAX_VALUE;
        for (Ball sb : seeAbleBalls) {
            double dt = Math.hypot(whitePos[0] - sb.x, whitePos[1] - sb.y);
            if (dt < closetDt) {
                closetDt = dt;
                closet = sb;
            }
        }
        return closet;
    }

    private void recordPositions() {
        recordedPositions.clear();
        for (B ball : getAllBalls()) {
            if (!ball.isPotted()) {
                recordedPositions.put(ball, new double[]{ball.getX(), ball.getY()});
            }
        }
    }

    public boolean isOccupied(double x, double y) {
        for (B ball : getAllBalls()) {
            if (!ball.isPotted()) {
                double dt = Algebra.distanceToPoint(x, y, ball.x, ball.y);
                if (dt <= gameValues.ball.ballDiameter) return true;
            }
        }
        return false;
    }

    private Movement physicalCalculate(Phy phy) {
//        physicsTimer = new Timer();
        long st = System.currentTimeMillis();
//        try {
//            Thread.sleep(1500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        physicsCalculator = new PhysicsCalculator(phy);
        Movement movement = physicsCalculator.calculate();
        System.out.println("Physical calculation ends in " + (System.currentTimeMillis() - st) + " ms");

        return movement;
    }

    protected void potSuccess(boolean isSnookerFreeBall) {
        lastPotSuccess = true;
        updateTargetPotSuccess(isSnookerFreeBall);
    }

    protected void potSuccess() {
        potSuccess(false);
    }
    
    public boolean isStarted() {
        return finishedCuesCount > 0;
    }

    protected abstract void endMoveAndUpdate();

    protected abstract void updateTargetPotSuccess(boolean isSnookerFreeBall);

    protected abstract void updateTargetPotFailed();

    public abstract GamePlayStage getGamePlayStage(Ball predictedTargetBall, boolean printPlayStage);

    public int getContinuousFoulAndMiss() {
        return 0;
    }

    public String getFoulReason() {
        return thisCueFoul.getAllReasons();
    }

    public boolean isThisCueFoul() {
        return thisCueFoul.isFoul();
    }

    public FoulInfo getThisCueFoul() {
        return thisCueFoul;
    }

    public boolean wasLastPotSuccess() {
        return lastPotSuccess;
    }

    public Set<Ball> getNewPottedLegal() {
        return newPottedLegal;
    }

    public void switchPlayer() {
//        parent.notifyPlayerWillSwitch(currentPlayer);
        currentPlayer.clearSinglePole();
        currentPlayer = getAnotherPlayer();
    }

    public P getAnotherPlayer(Player player) {
        return player == player1 ? player2 : player1;
    }

    protected P getAnotherPlayer() {
        return getAnotherPlayer(currentPlayer);
    }
    
    public InGamePlayer getAnotherIgp(InGamePlayer igp) {
        return igp == player1.getInGamePlayer() ? player2.getInGamePlayer() : player1.getInGamePlayer();
    }

    protected void end() {
        ended = true;
    }

    @Override
    public InGamePlayer getP1() {
        return player1.getInGamePlayer();
    }

    @Override
    public InGamePlayer getP2() {
        return player2.getInGamePlayer();
    }

    public static class SeeAble {
        public final int seeAbleTargets;
        public final double avgTargetDistance;
        public final double maxShadowAngle;  // 那颗球离白球的距离

        SeeAble(int seeAbleTargets, double avgTargetDistance, double maxShadowAngle) {
            this.seeAbleTargets = seeAbleTargets;
            this.avgTargetDistance = avgTargetDistance;
            this.maxShadowAngle = maxShadowAngle;
        }
    }

    public class WhitePredictor {
        private final Ball cueBallClone;
        private double lenAfterWall;
        private WhitePrediction prediction;
        private double cumulatedPhysicalTime = 0.0;
        //        private double lastPhysicalTime = 0.0;
        private double dtWhenHitFirstWall = -1.0;
        private boolean notTerminated = true;
        private boolean hitWall;
        private boolean predictPath;
        private boolean checkCollisionAfterFirst;
        private boolean recordTargetPos;
        private Phy phy;

        WhitePredictor(Ball cueBallClone) {
            this.cueBallClone = cueBallClone;
        }

        WhitePrediction predict(Phy phy,
                                double lenAfterWall,
                                boolean predictPath,
                                boolean checkCollisionAfterFirst,
                                boolean recordTargetPos) {
            this.phy = phy;
            this.lenAfterWall = lenAfterWall;
            this.predictPath = predictPath;
            this.checkCollisionAfterFirst = checkCollisionAfterFirst;
            this.recordTargetPos = recordTargetPos;

//            cueBallClone = cueBall.clone();

            prediction = new WhitePrediction(cueBallClone);

            while (!oneRun() && notTerminated) {
                if (cumulatedPhysicalTime >= CONGESTION_LIMIT) {
                    // Must be something wrong
                    System.err.println("White prediction congestion");
                    for (Ball ball : getAllBalls()) {
                        ball.clearMovement();
                    }
                    break;
                }
            }

            notTerminated = false;
//            System.out.println("Prediction physical " + cumulatedPhysicalTime);
            return prediction;
        }

        /**
         * 返回是否所有运算已经完毕
         */
        private boolean oneRun() {
            cumulatedPhysicalTime += phy.calculateMs;
            boolean whiteRun = oneRunWhite();
            Ball firstBall = prediction.getFirstCollide();
            if (recordTargetPos && firstBall != null) {
                boolean firstBallRun = oneRunFirstBall(firstBall);
                return whiteRun && firstBallRun;
            } else {
                return whiteRun;
            }
        }

        private boolean oneRunFirstBall(Ball firstBall) {
            firstBall.prepareMove(phy);

            if (firstBall.isLikelyStopped(phy)) return true;
            if (firstBall.isOutOfTable()) return true;
            if (firstBall.willPot(phy)) {
                prediction.potFirstBall();
                return true;
            }
            if (firstBall.currentBounce != null) {
                firstBall.processBounce(false);
                tryHitBallOther(firstBall);
                firstBall.normalMove(phy);
                return false;
            }

            ObjectOnTable.CushionHitResult holeAreaResult = firstBall.tryHitHoleArea(phy);
            if (holeAreaResult != null && holeAreaResult.result() != 0) {
                tryHitBallOther(firstBall);
                if (holeAreaResult.result() == 2) {
                    prediction.firstBallHitCushion();
                }
                return false;
            }
            if (firstBall.tryHitWall(phy) != null) {
                prediction.firstBallHitCushion();
                return false;
            }
            tryHitBallOther(firstBall);
            firstBall.normalMove(phy);
            return false;
        }

        private boolean oneRunWhite() {
            prediction.addPointInPath(new double[]{cueBallClone.x, cueBallClone.y});
            cueBallClone.prepareMove(phy);

            if (cueBallClone.isLikelyStopped(phy)) return true;
            if (cueBallClone.isOutOfTable()) return true;
            if (!predictPath && prediction.getFirstCollide() != null) return true;
            if (cueBallClone.tryHitPocketsBack(phy)) {
//                cueBallClone.normalMove(phy);
                prediction.potCueBall();
                return true;
            }
            if (cueBallClone.willPot(phy)) {
                prediction.potCueBall();
                return true;
            }

            if (prediction.getFirstCollide() == null &&
                    dtWhenHitFirstWall >= 0.0 &&
                    cueBallClone.getDistanceMoved() - dtWhenHitFirstWall > lenAfterWall) {
                // 解斯诺克不能太容易了
                prediction.whiteHitCushion();
                return true;
            }

            if (cueBallClone.currentBounce != null) {
                cueBallClone.processBounce(false);
                if (prediction.getFirstCollide() == null) {
                    tryWhiteHitBall();
                } else if (checkCollisionAfterFirst && prediction.getSecondCollide() == null) {
                    tryPassSecondBall();
                }
                cueBallClone.normalMove(phy);
                return false;
            }

            ObjectOnTable.CushionHitResult holeAreaResult = cueBallClone.tryHitHoleArea(phy);
            if (holeAreaResult != null && holeAreaResult.result() != 0) {
                // 袋口区域
                if (prediction.getFirstCollide() == null) {
                    tryWhiteHitBall();
                } else if (checkCollisionAfterFirst && prediction.getSecondCollide() == null) {
                    tryPassSecondBall();
                }
                if (holeAreaResult.result() == 2) {
                    if (!hitWall) {
                        dtWhenHitFirstWall = cueBallClone.getDistanceMoved();
                    }
//                    if (prediction.getSecondCollide() == null) {
                    // 如果白球碰了第二颗，那接下来就根本不可预测
                    // 有bug，修不好，取消判定
                    hitWall = true;
                    prediction.whiteCollidesHoleArcs();
                    prediction.whiteHitCushion();
//                    }
                }
                return false;
            }
            if (cueBallClone.tryHitWall(phy) != null) {
                // 库边
                if (!hitWall) {
                    dtWhenHitFirstWall = cueBallClone.getDistanceMoved();
                }
                hitWall = true;
                prediction.whiteHitCushion();
                return false;
            }
            if (prediction.getFirstCollide() == null) {
                if (tryWhiteHitBall()) {
                    cueBallClone.normalMove(phy);
                    return false;
                }
            } else if (checkCollisionAfterFirst && prediction.getSecondCollide() == null) {
                tryPassSecondBall();
            }
            cueBallClone.normalMove(phy);
            return false;
        }

        private void tryPassSecondBall() {
            for (Ball ball : getAllBalls()) {
                if (!ball.isWhite() && !ball.isPotted() && ball != prediction.getFirstCollide()) {
                    if (cueBallClone.predictedDtToPoint(ball.x, ball.y) <
                            gameValues.ball.ballDiameter) {
                        Ball ballClone = ball.clone();
//                        System.out.println("second: " + ballClone.getValue());
                        cueBallClone.twoMovingBallsHitCore(ballClone, phy, true);
                        prediction.setSecondCollide(ball,
                                Math.hypot(cueBallClone.vx, cueBallClone.vy) * phy.calculationsPerSec);
                    }
                }
            }
        }

        private void tryHitBallOther(Ball firstHit) {
            for (Ball b : getAllBalls()) {
                if (b != firstHit && !b.isPotted()) {
                    if (firstHit.predictedDtToPoint(b.x, b.y) <
                            gameValues.ball.ballDiameter) {
                        prediction.setFirstBallCollidesOther();
                    }
                }
            }
        }

        private boolean tryWhiteHitBall() {
            for (Ball ball : getAllBalls()) {
                if (!ball.isWhite() && !ball.isPotted()) {
                    if (cueBallClone.predictedDtToPoint(ball.x, ball.y) <
                            gameValues.ball.ballDiameter) {
                        double whiteVx = cueBallClone.vx;
                        double whiteVy = cueBallClone.vy;
                        cueBallClone.twoMovingBallsHitCore(ball, phy, true);
                        double[] rawBallUnitVec = Algebra.unitVector(
                                ball.getLastCollisionX() - cueBallClone.getLastCollisionX(),
                                ball.getLastCollisionY() - cueBallClone.getLastCollisionY());
                        double[] ballDirectionUnitVec = Algebra.unitVector(ball.vx, ball.vy);
                        double[] whiteDirectionUnitVec = Algebra.unitVector(whiteVx, whiteVy);

                        // todo: 确认是否考虑了齿轮效应
                        double ballInitVMmPerS = Math.hypot(ball.vx, ball.vy) * phy.calculationsPerSec;
                        if (!recordTargetPos) {
                            ball.vx = 0;
                            ball.vy = 0;
                        }
                        // 额外一帧应该还好
                        prediction.addPointInPath(new double[]{cueBallClone.getLastCollisionX(), cueBallClone.getLastCollisionY()});
                        prediction.setFirstCollide(ball,
                                Math.hypot(whiteVx, whiteVy) * phy.calculationsPerSec,
                                hitWall,
                                ballDirectionUnitVec[0], ballDirectionUnitVec[1],
                                rawBallUnitVec[0], rawBallUnitVec[1],
                                ballInitVMmPerS,
                                whiteDirectionUnitVec[0], whiteDirectionUnitVec[1],
                                cueBallClone.getLastCollisionX(), cueBallClone.getLastCollisionY());
                        return false;
                    }
                }
            }
            return true;
        }
    }

    public class PhysicsCalculator {

        private final Phy phy;
        private final int[] movementTypes = new int[getTable().nBalls()];
        private final double[] movementValues = new double[getTable().nBalls()];
        private Movement movement;
        private double cumulatedPhysicalTime = 0.0;
        private boolean notTerminated = true;

        private B[] randomOrderBallPool1;
        private B[] randomOrderBallPool2;

        PhysicsCalculator(Phy phy) {
            this.phy = phy;
        }

        Movement calculate() {
            movement = new Movement(getAllBalls());
            movement.startTrace();
            while (!oneRun() && notTerminated) {
                if (cumulatedPhysicalTime >= CONGESTION_LIMIT) {
                    // Must be something wrong
                    System.err.println("Physical calculation congestion");
                    movement.setCongested();
                    for (Ball ball : getAllBalls()) {
                        ball.clearMovement();
                    }
                    break;
                }
            }
//            endMove();
            notTerminated = false;
//            System.out.println("physical " + cumulatedPhysicalTime);

            for (Ball ball : getAllBalls()) {
                if (!ball.isPotted()) {
                    if (ball.getX() < 0 || ball.getX() >= gameValues.table.outerWidth ||
                            ball.getY() < 0 || ball.getY() >= gameValues.table.outerHeight) {
                        System.err.println("Ball " + ball + " at a weired position: " +
                                ball.getX() + ", " + ball.getY());
                    }
                }
            }
            System.out.println("Frames: " + movement.getMovementMap().get(cueBall).size());

            return movement;
        }

        private void reorderRandomPool() {
            if (randomOrderBallPool1 == null) {
                B[] allBalls = getAllBalls();
                randomOrderBallPool1 = Arrays.copyOf(allBalls, allBalls.length);
                randomOrderBallPool2 = Arrays.copyOf(allBalls, allBalls.length);
            }
//        Util.reverseArray(randomArrangedBalls);
            Util.shuffleArray(randomOrderBallPool1);
            Util.shuffleArray(randomOrderBallPool2);
        }

        private boolean oneRun() {
            boolean noBallMoving = true;
//            long st = System.nanoTime();
            reorderRandomPool();
            B[] allBalls = getAllBalls();
            for (B ball : allBalls) {
                ball.prepareMove(phy);
            }

            for (int i = 0; i < allBalls.length; i++) {
                B ball = allBalls[i];
                if (ball.isPotted()) {
                    if (ball.tryFrameInPocket(phy)) {
                        noBallMoving = false;
                    }
                    continue;
                }

                if (ball.isOutOfTable()) continue;

                if (!ball.isLikelyStopped(phy)) {
                    noBallMoving = false;

                    if (ball.tryHitPocketsBack(phy)) {
//                        ball.normalMove(phy);
                        movementTypes[i] = MovementFrame.POT;
                        movementValues[i] = Math.hypot(ball.vx, ball.vy) * phy.calculationsPerSec;
                        ball.naturalPot(1000);
                        newPotted.add(ball);
                        continue;
                    }

                    if (ball.willPot(phy)) {
                        movementTypes[i] = MovementFrame.POT;
                        movementValues[i] = Math.hypot(ball.vx, ball.vy) * phy.calculationsPerSec;
                        ball.naturalPot(1000);
                        newPotted.add(ball);
                        continue;
                    }
                    if (ball.currentBounce != null) {
                        ball.processBounce(App.PRINT_DEBUG);
                        if (tryHitBall(ball, true)) {
                            ball.normalMove(phy);
                        }
                        continue;
                    }
                    
                    ObjectOnTable.CushionHitResult holeAreaResult = ball.tryHitHoleArea(phy);
                    if (holeAreaResult != null && holeAreaResult.result() != 0) {
                        // 袋口区域
                        tryHitBall(ball, true);
                        if (holeAreaResult.result() == 2) {
                            collidesWall = true;
                            recordHitCushion(ball);
                            if (ball.isWhite()) 
                                movement.getWhiteTrace().hitCushion(holeAreaResult.cushion());
                            else
                                movement.getTraceOfBallNotNull(ball).hitCushion(holeAreaResult.cushion());
                            movementTypes[i] = holeAreaResult.cushion().movementType();
                            movementValues[i] = Math.hypot(ball.vx, ball.vy) * phy.calculationsPerSec;
                        }
                        continue;
                    }
                    Cushion cushion;
                    if ((cushion = ball.tryHitWall(phy)) != null) {
                        // 库边
                        collidesWall = true;
                        recordHitCushion(ball);
                        if (ball.isWhite()) movement.getWhiteTrace().hitCushion(cushion);
                        else movement.getTraceOfBallNotNull(ball).hitCushion(cushion);
                        movementTypes[i] = cushion.movementType();
                        movementValues[i] = Math.hypot(ball.vx, ball.vy) * phy.calculationsPerSec;
                        continue;
                    }

                    int threeHit = tryHitThreeBalls(ball);
                    if (threeHit == 0 || threeHit == 2) {
                        if (threeHit == 2) {
                            System.out.println("Cannot process");
                        }
                        if (tryHitBall(ball, threeHit == 0)) {
                            if (ball.checkEnterBreakArea(getTable().breakLineX())) {
                                // 一定在normal move之前
                                recordCrossLine(ball);
                            }
                            ball.normalMove(phy);
                        }
                    }
                } else {
                    if (!ball.sideSpinAtPosition(phy)) {

                    }
                }
            }
            double lastPhysicalTime = cumulatedPhysicalTime;
            cumulatedPhysicalTime += phy.calculateMs;

            if (noBallMoving) {
                for (Ball ball : getAllBalls()) {
                    if (!phy.isPrediction && !ball.isPotted() && ball.pocketHitCount > 5) {
                        // 没进且晃了很多下
                        AchManager.getInstance().addAchievement(Achievement.SHAKE_POCKET, getCuingIgp());
                    }
                    if (ball.isWhite()) {
                        movement.getWhiteTrace().setDistanceMoved(ball.getDistanceMoved());
                    } else {
                        movement.getTraceOfBallNotNull(ball).setDistanceMoved(ball.getDistanceMoved());
                    }
                    ball.clearMovement();
                }
            }

            if (Math.floor(cumulatedPhysicalTime / GameView.frameTimeMs) !=
                    Math.floor(lastPhysicalTime / GameView.frameTimeMs)) {
                // 一个动画帧执行一次
                for (int i = 0; i < allBalls.length; i++) {
                    B ball = allBalls[i];
                    ball.calculateAxis(phy, GameView.frameTimeMs);
                    movement.addFrame(ball,
                            new MovementFrame(ball.x, ball.y,
                                    ball.getAxisX(), ball.getAxisY(), ball.getAxisZ(), ball.getFrameDegChange(),
                                    !ball.canDraw(),
                                    movementTypes[i], movementValues[i]));
                    movementTypes[i] = MovementFrame.NORMAL;
                    movementValues[i] = 0.0;
                }
            }
            return noBallMoving;
//            if (noBallMoving) {
//                endMove();
//            }
//            System.out.print((System.nanoTime() - st) + " ");
        }

        private int tryHitThreeBalls(B ball) {
            // 返回值参考 Ball.tryHitTwoBalls
//            if (ball.isWhite()) {
//                return 0;  // 略过白球同时碰到两颗球的情况：无法处理
//            }

            int result = 0;
            for (B secondBall : randomOrderBallPool1) {
                if (ball != secondBall) {
                    for (Ball thirdBall : randomOrderBallPool2) {
                        if (ball != thirdBall && secondBall != thirdBall) {
                            int res = ball.tryHitTwoBalls(secondBall, thirdBall, phy);
                            if (res == 1) {
                                // 同时撞到两颗球
                                result = 1;
                                break;
                            } else if (res == 2) {
                                result = 2;
                                break;
                            }
                        }
                    }
                }
            }
            return result;
        }

        private void whiteCollide(Ball ball) {
            if (whiteFirstCollide == null) {
                whiteFirstCollide = ball;
                movement.setWhiteFirstCollide(ball);
                collidesWall = false;  // 必须白球在接触首个目标球后，再有球碰库
            }
            movement.getWhiteTrace().hitBall(ball);
        }

        private boolean tryHitBall(B ball, boolean applyComplexCal) {
            boolean noHit = true;
            for (B otherBall : randomOrderBallPool1) {
                if (ball != otherBall) {
                    if (ball.tryHitBall(otherBall, true, applyComplexCal, phy)) {
                        // hit ball
                        noHit = false;
                        if (ball.isWhite()) whiteCollide(otherBall);  // 记录白球撞到的球
                        break;  // 假设一颗球在一物理帧内不会撞到两颗球
                    }
                }
            }
            return noHit;
        }
    }
    
    public class FrameAchievementRecorder {
        int[] easyBallFails = new int[2];
    }
}
