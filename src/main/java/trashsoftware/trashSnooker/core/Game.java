package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.ai.AiCue;
import trashsoftware.trashSnooker.core.ai.AiCueResult;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.movement.MovementFrame;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.numberedGames.sidePocket.SidePocketGame;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.core.snooker.MiniSnookerGame;
import trashsoftware.trashSnooker.core.snooker.SnookerGame;
import trashsoftware.trashSnooker.core.table.Table;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.recorder.GameRecorder;
import trashsoftware.trashSnooker.recorder.NaiveGameRecorder;
import trashsoftware.trashSnooker.util.Util;

import java.io.IOException;
import java.util.*;

public abstract class Game<B extends Ball, P extends Player> implements GameHolder {
    //    public static final double calculateMs = 1.0;
//    public static final double calculationsPerSec = 1000.0 / calculateMs;
//    public static final double calculationsPerSecSqr = calculationsPerSec * calculationsPerSec;
//    public static final double speedReducer = 120.0 / calculationsPerSecSqr;
//    public static final double spinReducer = 4000.0 / calculationsPerSecSqr;  // 数值越大，旋转衰减越大
//    public static final double sideSpinReducer = 100.0 / calculationsPerSecSqr;
//    public static final double spinEffect = 1400.0 / calculateMs;  // 数值越小影响越大
    // 进攻球判定角
    // 如实际角度与可通过的袋口连线的夹角小于该值，判定为进攻球
    public static final double MAX_ATTACK_DECISION_ANGLE = Math.toRadians(7.5);
    public static final double MIN_PLACE_DISTANCE = 0.0;  // 防止物理运算卡bug
    public static final double MIN_GAP_DISTANCE = 3.0;
    public final long frameStartTime = System.currentTimeMillis();
    public final int frameIndex;
    protected final Set<B> newPotted = new HashSet<>();
    protected final GameView parent;
    protected final EntireGame entireGame;
    protected final Map<B, double[]> recordedPositions = new HashMap<>();  // 记录上一杆时球的位置，复位用
    protected final B cueBall;
    protected final GameSettings gameSettings;
    protected P player1;
    protected P player2;
    protected int recordedTarget;  // 记录上一杆时的目标球，复位用
    protected int finishedCuesCount = 0;  // 击球的计数器
    protected double lastCueVx;
    protected P currentPlayer;
    /**
     * {@link Game#getCurrentTarget()}
     */
    protected int currentTarget;
    protected Ball whiteFirstCollide;  // 这一杆白球碰到的第一颗球
    protected boolean collidesWall;
    protected boolean ballInHand = true;
    protected boolean lastCueFoul = false;
    protected boolean lastPotSuccess;
    protected boolean isBreaking = true;
    protected GameValues gameValues;
    protected String foulReason;
    protected int thinkTime;
    protected long cueFinishTime;
    protected long cueStartTime;
    protected GameRecorder recorder;
    protected Map<Integer, B> numberBallMap;
    private boolean ended;
    private B[] randomOrderBallPool1;
    private B[] randomOrderBallPool2;
    private PhysicsCalculator physicsCalculator;

    protected Game(GameView parent, EntireGame entireGame, 
                   GameSettings gameSettings, GameValues gameValues,
                   int frameIndex) {
        this.parent = parent;
        this.entireGame = entireGame;
        this.gameValues = gameValues;
        this.gameSettings = gameSettings;
        this.frameIndex = frameIndex;

        initPlayers();
        currentPlayer = gameSettings.isPlayer1Breaks() ? player1 : player2;
        setBreakingPlayer(currentPlayer);
        cueBall = createWhiteBall();
    }

    public static Game<? extends Ball, ? extends Player> createGame(
            GameView gameView, GameSettings gameSettings,
            GameType gameType, EntireGame entireGame) {
        int frameIndex = entireGame.getP1Wins() + entireGame.getP2Wins() + 1;
        Game<? extends Ball, ? extends Player> game;
        if (gameType == GameType.SNOOKER) {
            game = new SnookerGame(gameView, entireGame, gameSettings, frameIndex);
        } else if (gameType == GameType.MINI_SNOOKER) {
            game = new MiniSnookerGame(gameView, entireGame, gameSettings, frameIndex);
        } else if (gameType == GameType.CHINESE_EIGHT) {
            game = new ChineseEightBallGame(gameView, entireGame, gameSettings, frameIndex);
        } else if (gameType == GameType.SIDE_POCKET) {
            game = new SidePocketGame(gameView, entireGame, gameSettings, frameIndex);
        } else throw new RuntimeException("Unexpected game type " + gameType);

        try {
            game.recorder = new NaiveGameRecorder(game, entireGame);
            game.recorder.startRecoding();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return game;
    }

    public abstract GameType getGameType();

    public abstract Table getTable();

    protected void setBreakingPlayer(Player breakingPlayer) {
    }

    protected abstract void initPlayers();

    protected abstract B createWhiteBall();

    protected abstract AiCue<?, ?> createAiCue(P aiPlayer);

    public abstract ScoreResult makeScoreResult(Player justCuedPlayer);

    /**
     * 返回所有能打的球
     */
    public List<Ball> getAllLegalBalls(int targetRep, boolean isSnookerFreeBall) {
        List<Ball> balls = new ArrayList<>();
        for (Ball ball : getAllBalls()) {
            if (isLegalBall(ball, targetRep, isSnookerFreeBall)) balls.add(ball);
        }
        return balls;
    }

    public abstract boolean isLegalBall(Ball ball, int targetRep, boolean isSnookerFreeBall);

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

    public GameValues getGameValues() {
        return gameValues;
    }

    public GameRecorder getRecorder() {
        return recorder;
    }

    public Movement cue(CuePlayParams params, Phy phy) {
        cueStartTime = System.currentTimeMillis();
        if (cueFinishTime != 0) thinkTime = (int) (cueStartTime - cueFinishTime);

        whiteFirstCollide = null;
        collidesWall = false;
        lastPotSuccess = false;
        lastCueFoul = false;
        newPotted.clear();
        recordPositions();
        recordedTarget = currentTarget;

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
     * @param checkCollisionAfterFirst 是否检查白球打到目标球后是否碰到下一颗球
     * @param recordTargetPos          是否记录第一颗碰撞球的信息
     * @param wipe                     是否还原预测前的状态
     */
    public WhitePrediction predictWhite(CuePlayParams params,
                                        Phy phy,
                                        double lengthAfterWall,
                                        boolean checkCollisionAfterFirst,
                                        boolean recordTargetPos,
                                        boolean wipe) {
        if (cueBall.isPotted()) return null;
        cueBall.setVx(params.vx / phy.calculationsPerSec);
        cueBall.setVy(params.vy / phy.calculationsPerSec);
        params.xSpin = params.xSpin == 0.0d ? params.vx / 1000.0 : params.xSpin;  // 避免完全无旋转造成的NaN
        params.ySpin = params.ySpin == 0.0d ? params.vy / 1000.0 : params.ySpin;
        cueBall.setSpin(
                params.xSpin / phy.calculationsPerSec,
                params.ySpin / phy.calculationsPerSec,
                params.sideSpin / phy.calculationsPerSec);
        WhitePredictor whitePredictor = new WhitePredictor();
//        long st = System.currentTimeMillis();
        WhitePrediction prediction =
                whitePredictor.predict(phy, lengthAfterWall, checkCollisionAfterFirst, recordTargetPos);
//        System.out.println("White prediction ms: " + (System.currentTimeMillis() - st));
//        cueBall.setX(whiteX);
//        cueBall.setY(whiteY);
//        cueBall.pickup();
        if (wipe) prediction.resetToInit();
//        System.out.println(canSeeBall(cueBall, getAllBalls()[20]));
        return prediction;
    }

    public void finishMove() {
        System.out.println("Move end");
        physicsCalculator = null;
        cueFinishTime = System.currentTimeMillis();

        Player player = currentPlayer;
        endMoveAndUpdate();
        isBreaking = false;
        finishedCuesCount++;
        parent.finishCue(player, currentPlayer);
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
        }
    }

    public final boolean canPlaceWhite(double x, double y) {
        return x >= gameValues.leftX + gameValues.ballRadius &&
                x < gameValues.rightX - gameValues.ballRadius &&
                y >= gameValues.topY + gameValues.ballRadius &&
                y < gameValues.botY - gameValues.ballRadius &&
                canPlaceWhiteInTable(x, y);
    }

    protected abstract boolean canPlaceWhiteInTable(double x, double y);

    public abstract B[] getAllBalls();

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

//        double high = gameValues.maxLength;
//        double low = gameValues.ballRadius;
//
//        Ball obstacle = null;
//        double collisionX = 0.0;
//        double collisionY = 0.0;
//        while (high - low > Values.PREDICTION_INTERVAL) {
//            double mid = (high + low) / 2;
//            double endX = xUnitDirection * mid + cueBallX;
//            double endY = yUnitDirection * mid + cueBallY;
//            Ball firstObs = pointToPointObstacleBall(
//                    cueBallX, cueBallY,
//                    endX, endY,
//                    cueBall, null,
//                    true, true
//            );
//            if (firstObs == null) {  // 太近了
//                low = mid;
//            } else {
//                high = mid;
//                obstacle = firstObs;
//                collisionX = endX;
//                collisionY = endY;
//            }
//        }
//        if (obstacle == null) return null;


//        return new PredictedPos(obstacle, new double[]{collisionX, collisionY});

        double dx = Values.PREDICTION_INTERVAL * xUnitDirection;
        double dy = Values.PREDICTION_INTERVAL * yUnitDirection;

        double x = cueBallX + dx;
        double y = cueBallY + dy;
        while (x >= gameValues.leftX &&
                x < gameValues.rightX &&
                y >= gameValues.topY &&
                y < gameValues.botY) {
            for (Ball ball : getAllBalls()) {
                if (!ball.isPotted() && !ball.isWhite()) {
                    if (Algebra.distanceToPoint(
                            x, y, ball.x, ball.y
                    ) < gameValues.ballDiameter) {
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
     * @param extendCheck   如true，多检查一颗球的距离。比如检查进球点，因为进球点是个虚拟的球，不会碰撞
     * @return 两点之间能不能过球
     */
    public boolean pointToPointCanPassBall(double p1x, double p1y, double p2x, double p2y,
                                           Ball selfBall1, Ball selfBall2, boolean checkFullBall,
                                           boolean extendCheck) {

        double shadowRadius = checkFullBall ? gameValues.ballDiameter : gameValues.ballRadius;
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
            extraShadowAngle = -Math.asin(gameValues.ballDiameter / pointsDt);
        }
//        System.out.printf("%f %f %f %f\n", p1x, p1y, p2x, p2y);

        for (Ball ball : getAllBalls()) {
            if (ball != selfBall1 && ball != selfBall2 && !ball.isPotted()) {
                if (extendCheck) {
                    // 障碍球占用了目标点
                    double ballTargetDt = Math.hypot(ball.x - p2x, ball.y - p2y);
                    if (ballTargetDt <= gameValues.ballDiameter) return false;
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

//    private boolean canSeeBall(Ball b1, Ball b2) {
//        return canSeeBall(b1.x, b1.y, b2.x, b2.y, b1, b2);
//    }

    /**
     * @param situation 1:薄边 2:全球 3:缝隙能过全球（斯诺克自由球那种）
     * @return 能看到的目标球数量
     */
    public int countSeeAbleTargetBalls(double whiteX, double whiteY,
                                       Collection<Ball> legalBalls, int situation) {
        Set<Ball> legalSet;
        if (legalBalls instanceof Set) {
            legalSet = (Set<Ball>) legalBalls;
        } else {
            legalSet = new HashSet<>(legalBalls);
        }
        double shadowRadius = situation == 3 ? gameValues.ballRadius : gameValues.ballDiameter;
        int result = 0;

        double circle = Math.PI * 2;

        for (Ball target : legalBalls) {
            double xDiff0 = target.x - whiteX;
            double yDiff0 = target.y - whiteY;
            double whiteTarDt = Math.hypot(xDiff0, yDiff0);
            double angle = Algebra.thetaOf(xDiff0, yDiff0);
            
            double extraShadowAngle;
            if (situation == 1) {
                extraShadowAngle = -Math.asin(gameValues.ballDiameter / whiteTarDt);
            } else if (situation == 2) {
                extraShadowAngle = 0;
            } else {
                extraShadowAngle = Math.asin(gameValues.ballDiameter / whiteTarDt);  // 目标球本身的投影角
            }

            boolean canSee = true;

            for (Ball ball : getAllBalls()) {
                if (!ball.isWhite() && !ball.isPotted() && !legalSet.contains(ball)) {
                    // 是障碍球
                    double xDiff = ball.x - whiteX;
                    double yDiff = ball.y - whiteY;
                    double dt = Math.hypot(xDiff, yDiff);  // 两球球心距离
                    if (dt > whiteTarDt + gameValues.ballRadius) {
                        continue;  // 障碍球比目标球远，不可能挡
                    }
                    double connectionAngle = Algebra.thetaOf(xDiff, yDiff);  // 连线的绝对角度

                    double ballShadowAngle = Math.asin(shadowRadius / dt);  // 从selfBall看ball占的的角
                    
                    double selfPassAngle = 0;
                    if (situation == 3) {
                        // 起始球与障碍球切线长度
                        double tanDt = Math.sqrt(Math.pow(dt, 2) - Math.pow(gameValues.ballRadius, 2));
                        // 起始球自己的半径占的角度
                        selfPassAngle = Math.asin(gameValues.ballRadius / tanDt);
                    }
                    double left = connectionAngle + selfPassAngle + ballShadowAngle + extraShadowAngle;
                    double right = connectionAngle - selfPassAngle - ballShadowAngle - extraShadowAngle;
//                    System.out.printf("%f, Ball %s, %f %f %f\n", angle, ball, left, connectionAngle, right);

                    if (left > circle) {  // 连线中心小于360，左侧大于360
                        if (angle >= right) {
                            canSee = false;  // angle在right与x正轴之间，挡住
//                            System.out.println(ball + " obstacle 1");
                            break;
                        } else if (angle <= left - circle) {
                            canSee = false;  // angle在x正轴与left之间，挡住
//                            System.out.println(ball + " obstacle 1.1");
                            break;
                        }
                    } else if (right < 0) {  // 连线右侧小于0
                        if (angle >= circle + right) {
                            canSee = false;  // angle在right以上x正轴之下，挡住
//                            System.out.println(ball + " obstacle 2");
                            break;
                        } else if (angle <= left) {
                            canSee = false;  // angle在x正轴以上left之下，挡住
//                            System.out.println(ball + " obstacle 2.1");
                            break;
                        }
                    }
                    if (left >= angle && right <= angle) {
                        canSee = false;
//                        System.out.println(ball + " obstacle 3");
                        break;
                    }
                }
            }
            if (canSee) result++;
        }
        return result;
    }

//    public boolean canSeeBall(double p1x, double p1y, double p2x, double p2y,
//                              Ball selfBall1, Ball selfBall2) {
//        Ball obs = getFirstObstacleBall(p1x, p1y, p2x, p2y, selfBall1, selfBall2);
//        return obs == null;
//    }

//    public Ball getFirstObstacleBall(double p1x, double p1y, double p2x, double p2y,
//                                     Ball selfBall1, Ball selfBall2) {
////        double simulateBallDiameter = gameValues.ballDiameter - Values.PREDICTION_INTERVAL;
//        double circle = Math.PI * 2;
//
//        double xDiff0 = p2x - p1x;
//        double yDiff0 = p2y - p1y;
//        double pointsDt = Math.hypot(xDiff0, yDiff0);
//        double angle = Algebra.thetaOf(xDiff0, yDiff0);
////        System.out.printf("%f %f %f %f\n", p1x, p1y, p2x, p2y);
//
//        for (Ball ball : getAllBalls()) {
//            if (ball != selfBall1 && ball != selfBall2 && !ball.isPotted()) {
//                // 计算这个球遮住的角度范围
//                double xDiff = ball.getX() - p1x;
//                double yDiff = ball.getY() - p1y;
//                double dt = Math.hypot(xDiff, yDiff);  // 两球球心距离
//                if (dt > pointsDt + gameValues.ballDiameter) {
//                    continue;  // 障碍球比目标远，不可能挡
//                }
//                double connectionAngle = Algebra.thetaOf(xDiff, yDiff);  // 连线的绝对角度
//
//                double ballRadiusAngle = Math.asin(gameValues.ballRadius / dt);  // 从selfBall看ball占的的角
//                double left = connectionAngle + ballRadiusAngle;
//                double right = connectionAngle - ballRadiusAngle;
////                System.out.println(angle + " " + left + " " + right);
//
//                if (left > circle) {  // 连线中心小于360，左侧大于360
//                    if (angle >= right) {
//                        return ball;  // angle在right与x正轴之间，挡住
//                    }
//                } else if (right <= 0) {  // 连线右侧小于0
//                    if (angle > circle + right) {
//                        return ball;  // angle在right以上x正轴之下，挡住
//                    }
//                }
//                if (left >= angle && right <= angle) {
//                    return ball;
//                }
//            }
//        }
//        return null;

    // 两球连线、预测的最薄击球点构成两个直角三角形，斜边为连线，其中一个直角边为球直的径（理想状况下）
//        double xDiff = p2x - p1x;
//        double yDiff = p2y - p1y;
//        double[] vec = new double[]{xDiff, yDiff};
//        double[] unitVec = Algebra.unitVector(vec);
//        double dt = Math.hypot(xDiff, yDiff);  // 两球球心距离
//        double theta = Math.asin(simulateBallDiameter / dt);  // 连线与预测线的夹角
//        double alpha = Algebra.thetaOf(unitVec);  // 两球连线与X轴的夹角
//
//        for (double d = -1.0; d <= 1.0; d += 0.25) {
//            double ang = Algebra.normalizeAngle(alpha + theta * d);
//            double[] angUnitVec = Algebra.unitVectorOfAngle(ang);
////            System.out.println(ang + " orig ang: " + alpha);
//
//            PredictedPos pp = getPredictedHitBall(p1x, p1y, angUnitVec[0], angUnitVec[1]);
////            System.out.println(pp);
//            if (pp != null && pp.getTargetBall().getValue() == selfBall2.getValue()) return true;
//        }
//        return false;
//    }

    /**
     * 返回{目标球与"从目标球处能直接看到的洞口"的连线的单位向量, 洞口坐标(注意不是洞底坐标)}。
     */
    public List<double[][]> directionsToAccessibleHoles(Ball targetBall) {
        List<double[][]> list = new ArrayList<>();
//        BIG_LOOP:
        for (double[] hole : gameValues.allHoleOpenCenters) {
            if (pointToPointCanPassBall(targetBall.x, targetBall.y, hole[0], hole[1], targetBall,
                    null, true, false)) {
                double directionX = hole[0] - targetBall.x;
                double directionY = hole[1] - targetBall.y;
                double[] unitXY = Algebra.unitVector(directionX, directionY);
                list.add(new double[][]{unitXY, hole});
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

    public void setBallInHand() {
        ballInHand = true;
    }

    public Movement collisionTest() {
        Ball ball1 = getAllBalls()[0];
        Ball ball2 = getAllBalls()[1];

        ball1.pickup();
        ball2.pickup();

        ball1.setX(800);
        ball1.setY(800);

        ball1.setVx(6);
        ball1.setVy(0.0);

        ball2.setX(2000);
        ball2.setY(800);

        ball2.setVx(1);
        ball2.setVy(0.0);

        return physicalCalculate(entireGame.playPhy);
    }

    public void tieTest() {
//        for (Ball ball : redBalls) ball.pot();
//        yellowBall.pot();
//        greenBall.pot();
//        brownBall.pot();
//        blueBall.pot();
//        pinkBall.pot();
//        player2.addScore(-player2.getScore() + 7);
//        player1.addScore(-player1.getScore());
//        currentPlayer = player1;
//        currentTarget = 7;
    }

    public void clearRedBallsTest() {
//        for (int i = 0; i < 14; ++i) {
//            redBalls[i].pot();
//        }
    }

    public boolean isEnded() {
        return ended;
    }

    public abstract Player getWiningPlayer();

    public P getCuingPlayer() {
        return currentPlayer;
    }

    public P getPlayer1() {
        return player1;
    }

    public P getPlayer2() {
        return player2;
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

    public void withdraw(Player player) {
        player.withdraw();
        end();
    }

    private void whiteCollide(Ball ball) {
        if (whiteFirstCollide == null) {
            whiteFirstCollide = ball;
            collidesWall = false;  // 必须白球在接触首个目标球后，再有球碰库
        }
    }

    private void recordPositions() {
        recordedPositions.clear();
        for (B ball : getAllBalls()) {
            if (!ball.isPotted()) {
                recordedPositions.put(ball, new double[]{ball.getX(), ball.getY()});
            }
        }
    }

    protected boolean isOccupied(double x, double y) {
        for (B ball : getAllBalls()) {
            if (!ball.isPotted()) {
                double dt = Algebra.distanceToPoint(x, y, ball.x, ball.y);
                if (dt < gameValues.ballDiameter + MIN_GAP_DISTANCE) return true;
            }
        }
        return false;
    }

    private Movement physicalCalculate(Phy phy) {
//        physicsTimer = new Timer();
        long st = System.currentTimeMillis();
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

    protected abstract void endMoveAndUpdate();

    protected abstract void updateTargetPotSuccess(boolean isSnookerFreeBall);

    protected abstract void updateTargetPotFailed();

    public abstract GamePlayStage getGamePlayStage(Ball predictedTargetBall, boolean printPlayStage);

    public String getFoulReason() {
        return foulReason;
    }

    public boolean isLastCueFoul() {
        return lastCueFoul;
    }

    protected void switchPlayer() {
//        parent.notifyPlayerWillSwitch(currentPlayer);
        currentPlayer.clearSinglePole();
        currentPlayer = getAnotherPlayer();
    }

    public P getAnotherPlayer(P player) {
        return player == player1 ? player2 : player1;
    }

    protected P getAnotherPlayer() {
        return getAnotherPlayer(currentPlayer);
    }

    protected void end() {
        ended = true;
    }

    public class WhitePredictor {
        private double lenAfterWall;
        private WhitePrediction prediction;
        private double cumulatedPhysicalTime = 0.0;
        //        private double lastPhysicalTime = 0.0;
        private double dtWhenHitFirstWall = -1.0;
        private boolean notTerminated = true;
        private boolean hitWall;
        private boolean checkCollisionAfterFirst;
        private boolean recordTargetPos;
        private Phy phy;

        WhitePrediction predict(Phy phy,
                                double lenAfterWall,
                                boolean checkCollisionAfterFirst,
                                boolean recordTargetPos) {
            this.phy = phy;
            this.lenAfterWall = lenAfterWall;
            this.checkCollisionAfterFirst = checkCollisionAfterFirst;
            this.recordTargetPos = recordTargetPos;
            prediction = new WhitePrediction(cueBall);

            while (!oneRun() && notTerminated) {
                if (cumulatedPhysicalTime >= 30000) {
                    // Must be something wrong
                    System.err.println("White prediction congestion");
                    break;
                }
            }

            notTerminated = false;
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
            if (firstBall.willPot(phy)) {
                prediction.potFirstBall();
                return true;
            }

            int holeAreaResult = firstBall.tryHitHoleArea(phy);
            if (holeAreaResult != 0) {
                tryHitBallOther(firstBall);
                if (holeAreaResult == 2) {
                    prediction.firstBallHitCushion();
                }
                return false;
            }
            if (firstBall.tryHitWall()) {
                prediction.firstBallHitCushion();
                return false;
            }
            tryHitBallOther(firstBall);
            firstBall.normalMove(phy);
            return false;
        }

        private boolean oneRunWhite() {
            prediction.addPointInPath(new double[]{cueBall.x, cueBall.y});
            cueBall.prepareMove(phy);

            if (cueBall.isLikelyStopped(phy)) return true;
            if (cueBall.willPot(phy)) {
                prediction.potCueBall();
                return true;
            }

            if (prediction.getFirstCollide() == null &&
                    dtWhenHitFirstWall >= 0.0 &&
                    cueBall.getDistanceMoved() - dtWhenHitFirstWall > lenAfterWall) {
                // 解斯诺克不能太容易了
                return true;
            }

            int holeAreaResult = cueBall.tryHitHoleArea(phy);
            if (holeAreaResult != 0) {
                // 袋口区域
                if (prediction.getFirstCollide() == null) {
                    tryWhiteHitBall();
                } else if (checkCollisionAfterFirst && prediction.getSecondCollide() == null) {
                    tryPassSecondBall();
                }
                if (holeAreaResult == 2) {
                    if (!hitWall) {
                        dtWhenHitFirstWall = cueBall.getDistanceMoved();
                    }
                    hitWall = true;
                    prediction.whiteCollidesHoleArcs();
                    prediction.whiteHitCushion();
                }
                return false;
            }
            if (cueBall.tryHitWall()) {
                // 库边
                if (!hitWall) {
                    dtWhenHitFirstWall = cueBall.getDistanceMoved();
                }
                hitWall = true;
                prediction.whiteHitCushion();
                return false;
            }
            if (prediction.getFirstCollide() == null) {
                if (tryWhiteHitBall()) {
                    cueBall.normalMove(phy);
                    return false;
                }
            } else if (checkCollisionAfterFirst && prediction.getSecondCollide() == null) {
                tryPassSecondBall();
            }
            cueBall.normalMove(phy);
            return false;
        }

//        private boolean ballPhysical(Ball ball) {
//            boolean isCueBall = ball.isWhite();
//            int holeAreaResult = ball.tryHitHoleArea(phy);
//            if (holeAreaResult != 0) {
//                // 袋口区域
//                if (isCueBall) {
//                    if (prediction.getFirstCollide() == null) {
//                        tryHitBall();
//                    } else if (checkCollisionAfterFirst && prediction.getSecondCollide() == null) {
//                        tryPassSecondBall();
//                    }
//                    if (holeAreaResult == 2) {
//                        if (!hitWall) {
//                            dtWhenHitFirstWall = cueBall.getDistanceMoved();
//                        }
//                        hitWall = true;
//                        prediction.whiteCollidesHoleArcs();
//                    }
//                    return false;
//                }
//            }
//            if (ball.tryHitWall()) {
//                // 库边
//                if (isCueBall) {
//                    if (!hitWall) {
//                        dtWhenHitFirstWall = cueBall.getDistanceMoved();
//                    }
//                    hitWall = true;
//                    return false;
//                }
//            }
//            if (isCueBall) {
//                if (prediction.getFirstCollide() == null) {
//                    if (tryHitBall()) {
//                        cueBall.normalMove(phy);
//                        return false;
//                    }
//                } else if (checkCollisionAfterFirst && prediction.getSecondCollide() == null) {
//                    tryPassSecondBall();
//                }
//                cueBall.normalMove(phy);
//            }
//            return false;
//        }

        private void tryPassSecondBall() {
            for (Ball ball : getAllBalls()) {
                if (!ball.isWhite() && !ball.isPotted()) {
                    if (cueBall.predictedDtToPoint(ball.x, ball.y) <
                            gameValues.ballDiameter) {
                        prediction.setSecondCollide(ball,
                                Math.hypot(cueBall.vx, cueBall.vy) * phy.calculationsPerSec);
                    }
                }
            }
        }

        private void tryHitBallOther(Ball firstHit) {
            for (Ball b : getAllBalls()) {
                if (b != firstHit && !b.isPotted()) {
                    if (firstHit.predictedDtToPoint(b.x, b.y) <
                            gameValues.ballDiameter) {
                        prediction.setFirstBallCollidesOther();
                    }
                }
            }
        }

        private boolean tryWhiteHitBall() {
            for (Ball ball : getAllBalls()) {
                if (!ball.isWhite() && !ball.isPotted()) {
                    if (cueBall.predictedDtToPoint(ball.x, ball.y) <
                            gameValues.ballDiameter) {
                        double whiteVx = cueBall.vx;
                        double whiteVy = cueBall.vy;
                        cueBall.twoMovingBallsHitCore(ball);
                        double[] ballDirectionUnitVec = Algebra.unitVector(ball.vx, ball.vy);
                        double[] whiteDirectionUnitVec = Algebra.unitVector(whiteVx, whiteVy);
                        double ballInitVMmPerS = Math.hypot(ball.vx, ball.vy) * phy.calculationsPerSec;
                        if (!recordTargetPos) {
                            ball.vx = 0;
                            ball.vy = 0;
                        }
                        prediction.setFirstCollide(ball, hitWall,
                                ballDirectionUnitVec[0], ballDirectionUnitVec[1],
                                ballInitVMmPerS,
                                whiteDirectionUnitVec[0], whiteDirectionUnitVec[1],
                                cueBall.x, cueBall.y);
                        return false;
                    }
                }
            }
            return true;
        }
    }

    public class PhysicsCalculator {

        private Movement movement;
        private double cumulatedPhysicalTime = 0.0;
        private boolean notTerminated = true;
        private final Phy phy;

        private final int[] movementTypes = new int[getTable().nBalls()];
        private final double[] movementValues = new double[getTable().nBalls()];

        PhysicsCalculator(Phy phy) {
            this.phy = phy;
        }

        Movement calculate() {
            movement = new Movement(getAllBalls());
            while (!oneRun() && notTerminated) {
                if (cumulatedPhysicalTime > 30000) {
                    // Must be something wrong
                    System.err.println("Physical calculation congestion");
                    break;
                }
            }
//            endMove();
            notTerminated = false;

            for (Ball ball : getAllBalls()) {
                if (!ball.isPotted()) {
                    if (ball.getX() < 0 || ball.getX() >= gameValues.outerWidth ||
                            ball.getY() < 0 || ball.getY() >= gameValues.outerHeight) {
                        System.err.println("Ball " + ball + " at a weired position: " +
                                ball.getX() + ", " + ball.getY());
                    }
                }
                if (ball.getValue() == 6) {
                    System.out.println("Pick speed " + ball.vx + ", " + ball.vy + ", " +
                            ball.x + " " + ball.y);
                }
            }

            return movement;
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
                if (ball.isPotted()) continue;
                if (!ball.isLikelyStopped(phy)) {
                    noBallMoving = false;
                    if (ball.willPot(phy)) {
                        movementTypes[i] = MovementFrame.POT;
                        movementValues[i] = Math.hypot(ball.vx, ball.vy) * phy.calculationsPerSec;
                        ball.pot();
                        newPotted.add(ball);
                        continue;
                    }
                    int holeAreaResult = ball.tryHitHoleArea(phy);
                    if (holeAreaResult != 0) {
                        // 袋口区域
                        tryHitBall(ball);
                        if (holeAreaResult == 2) {
                            collidesWall = true;
                            movementTypes[i] = MovementFrame.CUSHION;
                            movementValues[i] = Math.hypot(ball.vx, ball.vy) * phy.calculationsPerSec;
                        }
                        continue;
                    }
                    if (ball.tryHitWall()) {
                        // 库边
                        collidesWall = true;
                        movementTypes[i] = MovementFrame.CUSHION;
                        movementValues[i] = Math.hypot(ball.vx, ball.vy) * phy.calculationsPerSec;
                        continue;
                    }

                    boolean noHit = tryHitThreeBalls(ball);
                    if (noHit) {
                        if (tryHitBall(ball)) {
                            ball.normalMove(phy);
                        }
                    }
//                    ball.normalMove();
//                    if (noHit) ball.normalMove();
                }
            }
            double lastPhysicalTime = cumulatedPhysicalTime;
            cumulatedPhysicalTime += phy.calculateMs;

            if (Math.floor(cumulatedPhysicalTime / parent.frameTimeMs) !=
                    Math.floor(lastPhysicalTime / parent.frameTimeMs)) {
                for (int i = 0; i < allBalls.length; i++) {
                    B ball = allBalls[i];
                    movement.addFrame(ball, 
                            new MovementFrame(ball.x, ball.y, ball.isPotted(), 
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

        private boolean tryHitThreeBalls(B ball) {
            if (ball.isWhite()) {
                return true;  // 略过白球同时碰到两颗球的情况：无法处理
            }

            boolean noHit = true;
            for (B secondBall : randomOrderBallPool1) {
                if (ball != secondBall) {
                    for (Ball thirdBall : randomOrderBallPool2) {
                        if (ball != thirdBall && secondBall != thirdBall) {
                            if (ball.tryHitTwoBalls(secondBall, thirdBall)) {
                                // 同时撞到两颗球
                                noHit = false;
                                break;
                            }
                        }
                    }
                }
            }
            return noHit;
        }

        private boolean tryHitBall(B ball) {
            boolean noHit = true;
            for (B otherBall : randomOrderBallPool1) {
                if (ball != otherBall) {
                    if (ball.tryHitBall(otherBall)) {
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
}
