package trashsoftware.trashSnooker.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import trashsoftware.trashSnooker.core.ai.AiCue;
import trashsoftware.trashSnooker.core.ai.AiCueResult;
import trashsoftware.trashSnooker.core.ai.AttackChoice;
import trashsoftware.trashSnooker.core.ai.AttackParam;
import trashsoftware.trashSnooker.core.attempt.CueAttempt;
import trashsoftware.trashSnooker.core.attempt.DefenseAttempt;
import trashsoftware.trashSnooker.core.attempt.PotAttempt;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.infoRec.CueInfoRec;
import trashsoftware.trashSnooker.core.metrics.*;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.movement.MovementFrame;
import trashsoftware.trashSnooker.core.movement.WhitePrediction;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.LisEightGame;
import trashsoftware.trashSnooker.core.numberedGames.nineBall.AmericanNineBallGame;
import trashsoftware.trashSnooker.core.person.PlayerHand;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.core.snooker.MiniSnookerGame;
import trashsoftware.trashSnooker.core.snooker.SnookerGame;
import trashsoftware.trashSnooker.core.snooker.SnookerTenGame;
import trashsoftware.trashSnooker.core.table.Table;
import trashsoftware.trashSnooker.core.training.PoolTraining;
import trashsoftware.trashSnooker.core.training.SnookerTraining;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.recorder.GameRecorder;
import trashsoftware.trashSnooker.recorder.InvalidRecorder;
import trashsoftware.trashSnooker.recorder.NaiveActualRecorder;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;
import trashsoftware.trashSnooker.util.db.DBAccess;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

public abstract class Game<B extends Ball, P extends Player> implements GameHolder, Cloneable {
    public static final int END_REP = 31;
    public static final int CONGESTION_LIMIT = 30000;
    // 进攻球判定角
    // 如实际角度与可通过的袋口连线的夹角小于该值，判定为进攻球
    public static final double MAX_ATTACK_DECISION_ANGLE = Math.toRadians(5.0);
    public static final double MIN_PLACE_DISTANCE = 0.0;  // 防止物理运算卡bug
    public static final double MIN_GAP_DISTANCE = 3.0;
    public final long frameStartTime = System.currentTimeMillis();
    public final int frameIndex;  // 相当于是这局是第几次开球
    public final int frameNumber;  // 规则上算第几局。如果全都没有重开过，那就=frameIndex
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
    protected boolean wasDoingFreeBall;  // 记录上一杆是不是自由球，复位用
    protected int finishedCuesCount = 0;  // 击球的计数器
    protected double lastCueVx;
    protected P lastCuedPlayer;
    protected P currentPlayer;
    protected CuePlayParams recordedCueParams;
    /**
     * {@link Game#getCurrentTarget()}
     */
    protected int currentTarget;
    protected int recordedTarget;  // 记录上一杆时的目标球，复位用
    protected Ball whiteFirstCollide;  // 这一杆白球碰到的第一颗球
    protected boolean collidesWall;
    protected boolean ballInHand = true;
    protected FoulInfo thisCueFoul = new FoulInfo();
    protected FoulInfo lastCueFoul;
    protected boolean lastPotSuccess;
    protected boolean isBreaking = true;
    protected boolean placedHandBallButNoHit;
    protected boolean firstCueAfterHandBall;
    protected boolean playingRepositionBall;  // 是否在打复位的球
    protected boolean playingLetBall;  // 是否在打对方的让杆
    protected boolean ballHeapIntact = true;  // 球堆是否没被动过
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
    protected Ball specifiedTarget;
    //    protected final Set<SubRule> subRules = new HashSet<>();
    protected BreakStats breakStats;

    protected Game(EntireGame entireGame,
                   GameSettings gameSettings, GameValues gameValues,
                   Table table,
                   int frameIndex,
                   int frameNumber) {
//        this.parent = parent;
        this.entireGame = entireGame;
        this.gameValues = gameValues;
        this.gameSettings = gameSettings;
        this.frameIndex = frameIndex;
        this.frameNumber = frameNumber;
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
            EntireGame entireGame,
            int frameIndex, 
            int frameNumber) {
        Game<? extends Ball, ? extends Player> game = createGameInternal(gameSettings, gameValues, entireGame,
                frameIndex, frameNumber);

        // 处理让球
        if (gameValues.isTraining()) {
            // todo: 可能有开球训练？
            game.isBreaking = false;
        } else {
            LetScoreOrBall p1Let = game.getP1().getLetScoreOrBall();
            LetScoreOrBall p2Let = game.getP2().getLetScoreOrBall();
            if (game instanceof AbstractSnookerGame asg) {
                if (p1Let instanceof LetScoreOrBall.LetScoreFace ls)
                    asg.getPlayer1().addScore(ls.score);
                else if (p1Let != null && p1Let != LetScoreOrBall.NOT_LET)
                    EventLogger.warning("Wrong let");
                if (p2Let instanceof LetScoreOrBall.LetScoreFace ls)
                    asg.getPlayer2().addScore(ls.score);
                else if (p2Let != null && p2Let != LetScoreOrBall.NOT_LET)
                    EventLogger.warning("Wrong let");
            } else if (game instanceof ChineseEightBallGame ceb) {
                if (p1Let instanceof LetScoreOrBall.LetBallFace lb) {
                    ceb.getPlayer1().getLettedBalls().putAll(lb.letBalls);
                } else if (p1Let != null && p1Let != LetScoreOrBall.NOT_LET)
                    EventLogger.warning("Wrong let");
                if (p2Let instanceof LetScoreOrBall.LetBallFace lb) {
                    ceb.getPlayer2().getLettedBalls().putAll(lb.letBalls);
                } else if (p2Let != null && p2Let != LetScoreOrBall.NOT_LET)
                    EventLogger.warning("Wrong let");
            }
        }

        try {
            if (DBAccess.RECORD && entireGame != null) {
                game.recorder = new NaiveActualRecorder(game, entireGame.getMetaMatchInfo());
            } else {
                game.recorder = new InvalidRecorder();
            }
            game.recorder.startRecoding();
        } catch (IOException e) {
            EventLogger.warning(e);
        }

        return game;
    }

    private static @NotNull Game<? extends Ball, ? extends Player> createGameInternal(
            GameSettings gameSettings, 
            GameValues gameValues, 
            EntireGame entireGame,
            int frameIndex,
            int frameRestartIndex) {
//        int frameIndex = entireGame == null ? 1 : (entireGame.getP1Wins() + entireGame.getP2Wins() + 1);
        Game<? extends Ball, ? extends Player> game;
        if (gameValues.rule == GameRule.SNOOKER) {
            if (gameValues.isTraining()) {
                game = new SnookerTraining(entireGame, gameSettings, gameValues,
                        gameValues.getTrainType(),
                        gameValues.getTrainChallenge());
            } else {
                game = new SnookerGame(entireGame, gameSettings, gameValues, frameIndex, frameRestartIndex);
            }
        } else if (gameValues.rule == GameRule.MINI_SNOOKER) {
            if (gameValues.isTraining()) {
                game = new SnookerTraining(entireGame, gameSettings, gameValues,
                        gameValues.getTrainType(),
                        gameValues.getTrainChallenge());
            } else {
                game = new MiniSnookerGame(entireGame, gameSettings, gameValues, frameIndex, frameRestartIndex);
            }
        } else if (gameValues.rule == GameRule.SNOOKER_TEN) {
            if (gameValues.isTraining()) {
                game = new SnookerTraining(entireGame, gameSettings, gameValues,
                        gameValues.getTrainType(),
                        gameValues.getTrainChallenge());
            } else {
                game = new SnookerTenGame(entireGame, gameSettings, gameValues, frameIndex, frameRestartIndex);
            }
        } else if (gameValues.rule == GameRule.CHINESE_EIGHT) {
            if (gameValues.isTraining()) {
                game = new PoolTraining(entireGame, gameSettings, gameValues, gameValues.getTrainType(), gameValues.getTrainChallenge());
            } else {
                game = new ChineseEightBallGame(entireGame, gameSettings, gameValues, frameIndex, frameRestartIndex);
            }
        } else if (gameValues.rule == GameRule.LIS_EIGHT) {
            if (gameValues.isTraining()) {
                game = new PoolTraining(entireGame, gameSettings, gameValues, gameValues.getTrainType(), gameValues.getTrainChallenge());
            } else {
                game = new LisEightGame(entireGame, gameSettings, gameValues, frameIndex, frameRestartIndex);
            }
        } else if (gameValues.rule == GameRule.AMERICAN_NINE) {
            if (gameValues.isTraining()) {
                game = new PoolTraining(entireGame, gameSettings, gameValues, gameValues.getTrainType(), gameValues.getTrainChallenge());
            } else {
                game = new AmericanNineBallGame(entireGame, gameSettings, gameValues, frameIndex, frameRestartIndex);
            }
        } else throw new RuntimeException("Unexpected game rule " + gameValues.rule);
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
    @SuppressWarnings("unchecked")
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

    protected abstract boolean isBallPlacedInHeap(Ball ball);

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

    @Override
    public GameValues getGameValues() {
        return gameValues;
    }

    public GameSettings getGameSettings() {
        return gameSettings;
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
            if (potAttempt.getTargetDirHole() == null) {
                // todo: 翻袋
                // 实际上是AI翻袋才会是null，只有ai代打时才会到这个分支
                // 而AI代打理论上也不应该计入成就，所以其实也没有todo
                // 确实是因为逻辑这么写有点太丑了
                return;
            }

            AttackChoice.DirectAttackChoice directAttackChoice = AttackChoice.DirectAttackChoice.createChoice(
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
            if (directAttackChoice != null) {
                CuePlayParams cpp = potAttempt.getCuePlayParams();
                AttackParam attackParam = new AttackParam(
                        directAttackChoice,
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
        recordedCueParams = params;

        System.out.println("Think time: " + thinkTime);
        if (thinkTime >= 60000) {
            AchManager.getInstance().addAchievement(Achievement.LONG_THINK, getCuingIgp());
        }

        if (false) {
            double speed = Math.hypot(params.vx, params.vy);
            System.out.println("Speed: " + speed + ", Est move:");
            double estDtMove = gameValues.estimatedMoveDistance(phy, speed);
            System.out.println(gameValues.estimateMoveTime(phy, speed, estDtMove - 10));
            System.out.println("SpeedNeed: " + gameValues.estimateSpeedNeeded(phy, estDtMove));
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
        aiCue.setPresetTarget(specifiedTarget);
        specifiedTarget = null;
        return aiCue.makeCue(phy);
    }

    /**
     * @param params                   击球参数
     * @param phy                      使用哪一个物理值
     * @param lengthAfterWall          直接碰库后白球预测线的长度
     * @param predictPath              是否预测撞击之后的线路
     * @param checkCollisionAfterFirst 是否检查白球打到目标球后是否碰到下一颗球
     * @param predictTargetBall        是否记录对第一颗碰撞球进行模拟
     * @param stopAtCollision          是否在白球撞上第一颗球时就结束模拟
     * @param wipe                     是否还原预测前的状态
     * @param useClone                 是否使用拷贝的白球，仅为多线程服务
     */
    public WhitePrediction predictWhite(CuePlayParams params,
                                        Phy phy,
                                        double lengthAfterWall,
                                        boolean predictPath,
                                        boolean checkCollisionAfterFirst,
                                        boolean predictTargetBall,
                                        boolean stopAtCollision,
                                        boolean wipe,
                                        boolean useClone) {
        if (cueBall.isNotOnTable()) {
            return null;
        }
        if (stopAtCollision) {
            if (predictTargetBall || checkCollisionAfterFirst) {
                throw new IllegalArgumentException("Argument 'stopAtCollision' is repulsive to" +
                        "'checkCollisionAfterFirst' or 'predictTargetBall'.");
            }
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
                whitePredictor.predict(phy,
                        lengthAfterWall,
                        predictPath,
                        checkCollisionAfterFirst,
                        predictTargetBall,
                        stopAtCollision);
//        System.out.println("White prediction ms: " + (System.currentTimeMillis() - st));
//        cueBall.setX(whiteX);
//        cueBall.setY(whiteY);
//        cueBall.pickup();
        if (wipe) prediction.resetToInit();
//        System.out.println(canSeeBall(cueBall, getAllBalls()[20]));
        return prediction;
    }
    
    private void recordInfo(Map<Integer, Integer> newlyPot,
                            int[] scoresBefore,
                            List<CueInfoRec.Special> specials) {
        
        entireGame.getMatchInfoRec().recordACue(
                lastCuedPlayer.getInGamePlayer().getPlayerNumber(),
                recordedTarget,
                specifiedTarget,
                whiteFirstCollide == null ? 0 : whiteFirstCollide.value,
                recordedCueParams == null ?
                        PlayerHand.CueHand.makeDefault(PlayerHand.Hand.RIGHT, lastCuedPlayer.getInGamePlayer()) : 
                        recordedCueParams.cueParams.getCuePlayerHand().toCueHand(),
                newlyPot,
                new int[]{player1.getScore() - scoresBefore[0], player2.getScore() - scoresBefore[1]},
                new int[]{player1.getScore(), player2.getScore()},
                lastCuedPlayer.getAttempts().getLast(),
                thisCueFoul,
                specials
        );
    }
    
    private List<CueInfoRec.Special> getSpecials() {
        List<CueInfoRec.Special> specials = new ArrayList<>();
        if (firstCueAfterHandBall) specials.add(CueInfoRec.Special.BALL_IN_HAND);
        if (isDoingSnookerFreeBll()) specials.add(CueInfoRec.Special.SNOOKER_FREE_BALL);
        if (playingRepositionBall) specials.add(CueInfoRec.Special.REPOSITION);
        if (playingLetBall) specials.add(CueInfoRec.Special.LET_OTHER_PLAY);
        // todo: 八球让球和美式的pushout没做
        
        return specials;
    }

    public void finishMove(GameView gameView) {
        System.out.println("Move end");
        physicsCalculator = null;
        cueFinishTime = System.currentTimeMillis();
        
        // record info, 在endMoveAndUpdate之前的部分
        Map<Integer, Integer> newlyPot = new TreeMap<>();
        for (Ball ball : newPotted) {
            newlyPot.merge(ball.value, 1, Integer::sum);
        }
        int[] pScores = new int[]{player1.getScore(), player2.getScore()};
        List<CueInfoRec.Special> specials = getSpecials();

        Player player = currentPlayer;
        endMoveAndUpdate();
        playingRepositionBall = false;
        playingLetBall = false;
        recordInfo(newlyPot, pScores, specials);
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
                } else if (ball.x > gameValues.table.rightX + gameValues.ball.ballRadius) {
                    moveX = -1;
                }

                if (ball.y < gameValues.table.topY - gameValues.ball.ballRadius) {
                    moveY = 1;
                } else if (ball.y > gameValues.table.botY + gameValues.ball.ballRadius) {
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

    public int nBalls() {
        return getAllBalls().length;
    }

    public void quitGame() {
    }

    public void forcePlaceWhiteNoRecord(double realX, double realY) {
        cueBall.setX(realX);
        cueBall.setY(realY);
        cueBall.pickup();
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

    public void setSpecifiedTarget(Ball specifiedTarget) {
        this.specifiedTarget = specifiedTarget;
    }

    public Ball getSpecifiedTarget() {
        return specifiedTarget;
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
                                .getCueSelection().getSelected().brand.getWoodPartLength() + 300.0,
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
     * 时间复杂度: O(n), n=台上的球数
     *
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

    public List<DoublePotAiming> doublePotAble(double whiteX,
                                               double whiteY,
                                               Collection<Ball> legalBalls,
                                               int cushionLimit,
                                               @Nullable Pocket[] legalPockets) {
        if (legalPockets == null) legalPockets = gameValues.table.pockets;
        List<DoublePotAiming> list = new ArrayList<>();
        for (Ball ball : legalBalls) {
            double[] targetPos = new double[]{ball.x, ball.y};
            for (Pocket pocket : legalPockets) {
                // 检查每个球去每个袋是否有翻袋线路
                // 目前仅支持一库
                Set<DoublePotAiming> c1 = singleCushionDouble(whiteX, whiteY, ball, targetPos, pocket);
                if (!c1.isEmpty()) {
                    list.addAll(c1);
                }
            }
        }
        return list;
    }

    private Set<DoublePotAiming> singleCushionDouble(double whiteX,
                                                     double whiteY,
                                                     Ball ball,
                                                     double[] targetPos,
                                                     Pocket pocket) {
        // 用set是因为目前的算法来看，上方的库左右都是没有区别的，避免重复
        Set<DoublePotAiming> result = new HashSet<>();
        TableMetrics table = gameValues.table;
        double radius = gameValues.ball.ballRadius;
        double[] pocketOpenCenter = pocket.getOpenCenter(gameValues);

        Cushion.EdgeCushion[] avail = getCushionCanSeePocket(pocket);
        for (Cushion.EdgeCushion cushion : avail) {
            // 这就是假想袋的位置
            // todo: 没有考虑球的直径
            double[] pocketSymPos = new double[]{pocketOpenCenter[0], pocketOpenCenter[1]};
            double axisPointX = 0;
            double axisPointY = 0;
            if (cushion == table.leftCushion) {
                // x基于左库往右半颗球处对称
                axisPointX = table.leftX + radius;
                pocketSymPos[0] = axisPointX - (pocketOpenCenter[0] - axisPointX);
            } else if (cushion == table.rightCushion) {
                axisPointX = table.rightX - radius;
                pocketSymPos[0] = axisPointX + (axisPointX - pocketOpenCenter[0]);
            } else if (cushion == table.topLeftCushion || cushion == table.topRightCushion) {
                axisPointY = table.topY + radius;
                pocketSymPos[1] = axisPointY - (pocketOpenCenter[1] - axisPointY);
            } else if (cushion == table.botLeftCushion || cushion == table.botRightCushion) {
                axisPointY = table.botY - radius;
                pocketSymPos[1] = axisPointY + (axisPointY - pocketOpenCenter[1]);
            } else {
                throw new RuntimeException("Unexpected cushion " + cushion);
            }
            DoublePotAiming dpa = getDoublePotAiming(
                    whiteX, whiteY,
                    axisPointX, axisPointY,
                    ball,
                    cushion,
                    targetPos,
                    pocket,
                    pocketOpenCenter,
                    pocketSymPos);
            if (dpa != null) result.add(dpa);
        }
        return result;
    }

    /**
     * 一库翻袋的函数
     */
    private DoublePotAiming getDoublePotAiming(double whiteX,
                                               double whiteY,
                                               double axisPointX,  // x和y一个是0另一个不是
                                               double axisPointY,
                                               Ball ball,
                                               Cushion cushion,
                                               double[] targetPos,
                                               Pocket pocket,
                                               double[] pocketRealPos,
                                               double[] pocketSymPos) {
        double ballDirX = pocketSymPos[0] - targetPos[0];
        double ballDirY = pocketSymPos[1] - targetPos[1];
        double[] ballUnitDir = Algebra.unitVector(ballDirX, ballDirY);

        double[] collPos = new double[]{
                targetPos[0] - ballUnitDir[0] * gameValues.ball.ballDiameter,
                targetPos[1] - ballUnitDir[1] * gameValues.ball.ballDiameter
        };

        if (!pointToPointCanPassBall(whiteX, whiteY,
                collPos[0], collPos[1],
                cueBall, ball,
                true, true)) {
            // 白球处看不到点
            return null;
        }

        double[] whiteDir = Algebra.unitVector(collPos[0] - whiteX, collPos[1] - whiteY);
        double angle = Algebra.thetaBetweenVectors(whiteDir, ballUnitDir);
        if (angle >= Math.PI / 2) {
            return null;  // 不可能打进
        }

        double[] cushionPoint;
        if (axisPointX != 0) {
            // 两侧（短库）
            double ballToCushion = axisPointX - targetPos[0];
            double ratio = ballToCushion / ballDirX;
            if (ratio < 0) {
                // 一定同号，不然就是bug
                EventLogger.error(String.format("ball to cus: %f, ball dirX: %f, ax x: %f",
                        ballToCushion, ballDirX, axisPointX));
                return null;
            }

            double ballToCushion2 = ballDirY * ratio;
            cushionPoint = new double[]{axisPointX, targetPos[1] + ballToCushion2};
        } else {
            // 上下（长）
            double ballToCushion = axisPointY - targetPos[1];
            double ratio = ballToCushion / ballDirY;
            if (ratio < 0) {
                // 一定同号，不然就是bug
                EventLogger.error(String.format("ball to cus: %f, ball dirY: %f, ax y: %f",
                        ballToCushion, ballDirY, axisPointY));
                return null;
            }

            double ballToCushion2 = ballDirX * ratio;
            cushionPoint = new double[]{targetPos[0] + ballToCushion2, axisPointY};
        }

        if (!pointToPointCanPassBall(targetPos[0], targetPos[1],
                cushionPoint[0], cushionPoint[1],
                ball, null,
                true, true)) {
            // 目标球看不到库点
            return null;
        }
        if (!pointToPointCanPassBall(cushionPoint[0], cushionPoint[1],
                pocketRealPos[0], pocketRealPos[1],
                null, null,
                true, true)) {
            // 库点看不到袋
            return null;
        }

//        System.out.println("Coll: " + Arrays.toString(collPos) + ", " +
//                "cushion: " + cushion + ", " +
//                "cushion point: " + Arrays.toString(cushionPoint) + ", " +
//                "pocket: " + pocket.hole + ", " +
//                "Symmetry: " + Arrays.toString(pocketSymPos));

        return new DoublePotAiming(
                ball,
                targetPos,
                pocket,
                collPos,
                List.of(cushionPoint),  // 一库
                whiteDir,
                1
        );
    }

    /**
     * 实际上就是返回看得到袋的库
     */
    private Cushion.EdgeCushion[] getCushionCanSeePocket(Pocket pocket) {
        TableMetrics table = gameValues.table;

        if (pocket.hole == TableMetrics.Hole.TOP_MID) {
            return new Cushion.EdgeCushion[]{
                    table.botLeftCushion,
                    table.botRightCushion,
//                    table.leftCushion,  // 底袋翻中怕是玄幻了点？
//                    table.rightCushion
            };
        } else if (pocket.hole == TableMetrics.Hole.BOT_MID) {
            return new Cushion.EdgeCushion[]{
                    table.topLeftCushion,
                    table.topRightCushion,
//                    table.leftCushion,
//                    table.rightCushion
            };
        } else if (pocket.hole == TableMetrics.Hole.TOP_LEFT) {
            return new Cushion.EdgeCushion[]{
                    table.botLeftCushion,
                    table.botRightCushion,
                    table.rightCushion
            };
        } else if (pocket.hole == TableMetrics.Hole.TOP_RIGHT) {
            return new Cushion.EdgeCushion[]{
                    table.botLeftCushion,
                    table.botRightCushion,
                    table.leftCushion
            };
        } else if (pocket.hole == TableMetrics.Hole.BOT_RIGHT) {
            return new Cushion.EdgeCushion[]{
                    table.topLeftCushion,
                    table.topRightCushion,
                    table.leftCushion
            };
        } else if (pocket.hole == TableMetrics.Hole.BOT_LEFT) {
            return new Cushion.EdgeCushion[]{
                    table.topLeftCushion,
                    table.topRightCushion,
                    table.rightCushion
            };
        } else {
            throw new RuntimeException("No such pocket: " + pocket);
        }
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
        double weightedSumTargetDt = 0.0;
        List<double[]> seeAbleBallIntervals = new ArrayList<>();

        for (Ball target : legalBalls) {
            double xDiff0 = target.x - whiteX;
            double yDiff0 = target.y - whiteY;
            double whiteTarDt = Math.hypot(xDiff0, yDiff0);
            double angle = Algebra.thetaOf(xDiff0, yDiff0);
            sumTargetDt += whiteTarDt;
            weightedSumTargetDt += Math.pow(whiteTarDt / gameValues.table.maxLength, 0.5);  // 近的权重高

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

                // 研究能看到的角度
                double angleToCenter = Algebra.thetaOf(xDiff0, yDiff0);
                double angularRadius = Math.asin(gameValues.ball.ballRadius / whiteTarDt);
                double start, end;
                if (Double.isNaN(angularRadius)) {
                    start = Algebra.normalizeAnglePositive(angleToCenter - Math.PI / 4);
                    end = Algebra.normalizeAnglePositive(angleToCenter + Math.PI / 4);
                } else {
                    start = Algebra.normalizeAnglePositive(angleToCenter - angularRadius);
                    end = Algebra.normalizeAnglePositive(angleToCenter + angularRadius);
                }
                if (start > end) { // crosses 2π wrap
                    seeAbleBallIntervals.add(new double[]{start, 2 * Math.PI});
                    seeAbleBallIntervals.add(new double[]{0, end});
                } else {
                    seeAbleBallIntervals.add(new double[]{start, end});
                }

                result++;
            }
        }

        // 能看到的球的总角度，没考虑球被部分遮挡这种情况
        seeAbleBallIntervals.sort(Comparator.comparingDouble(a -> a[0]));
//        System.out.println(Arrays.deepToString(seeAbleBallIntervals.toArray()));
        List<double[]> merged = new ArrayList<>();
        for (double[] interval : seeAbleBallIntervals) {
            if (merged.isEmpty() || merged.getLast()[1] < interval[0]) {
                merged.add(interval);
            } else {
                double[] last = merged.getLast();
                last[1] = Math.max(last[1], interval[1]);
            }
        }

        // Sum merged arc lengths
        double totalSeeAbleRads = 0.0;
        for (double[] arc : merged) {
            totalSeeAbleRads += arc[1] - arc[0];
        }

        return new SeeAble(
                result,
                sumTargetDt / legalBalls.size(),
                weightedSumTargetDt / legalBalls.size(),
                totalSeeAbleRads,
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
        double[] xy = new double[]{x, y};
//        BIG_LOOP:
        for (int i = 0; i < 6; i++) {
            Pocket pocket = gameValues.table.pockets[i];
//            double[] holeOpenCenter = gameValues.allHoleOpenCenters[i];
            double[] holeOpenCenter = pocket.getOpenCenter(gameValues);
//            double[] holeBottom = gameValues.table.allHoles[i];

            boolean onPocketMouth = !pocket.isMid &&
                    Algebra.distanceToPoint(x, y, holeOpenCenter[0], holeOpenCenter[1]) < gameValues.ball.ballRadius;
            boolean inPocketMouth = Algebra.isBetweenPerpendiculars(holeOpenCenter, pocket.fallCenter, xy);

            if ((onPocketMouth || inPocketMouth) &&
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

    public void forceSetBallInHand() {
        setBallInHand();
    }

    protected void setBallInHand() {
        ballInHand = true;
        if (!cueBall.isPotted()) {
            cueBall.pot();
        }
        cueBall.model.initRotation(false);
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

    protected void updateBreakStats(Set<B> newPotted) {
        int uniqueBallsHitCushion = 0;
        int acrossBreakLine = 0;
        for (B ball : getAllBalls()) {
            if (!ball.isWhite()) {
                int[] stats = computeCushionAndAcrossLineOfBall(ball);
                acrossBreakLine += stats[1];
                if (stats[0] > 0) {
                    uniqueBallsHitCushion++;
                }
            }
        }

        breakStats = new BreakStats(newPotted.size(), uniqueBallsHitCushion, acrossBreakLine);
    }

    public BreakStats getBreakStats() {
        return breakStats;
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
        if (!isSimulate) {
            System.out.println("Reposition!");
            playingRepositionBall = true;
        }
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
            if (cueBall.isPotted()) setBallInHand();
            AchManager.getInstance().addAchievement(Achievement.MISSED_SHOT, getCuingIgp());
        }
        if (cueBall.isPotted()) {
            thisCueFoul.addFoul(strings.getString("cueBallPot"), foulScoreCalculator.get(), false);
            setBallInHand();
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

        List<AttackChoice.DirectAttackChoice> directAttackChoices = new ArrayList<>();

        for (Ball ball : targets) {
            List<double[][]> dirHoles = directionsToAccessibleHoles(ball);

            for (double[][] dirHole : dirHoles) {
                double collisionPointX = dirHole[2][0];
                double collisionPointY = dirHole[2][1];

                if (pointToPointCanPassBall(whitePos[0], whitePos[1],
                        collisionPointX, collisionPointY, getCueBall(), ball, true,
                        true)) {
                    // 从白球处看得到进球点
                    AttackChoice.DirectAttackChoice directAttackChoice = AttackChoice.DirectAttackChoice.createChoice(
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
                    if (directAttackChoice != null) {
                        directAttackChoices.add(directAttackChoice);
                    }
                }
            }
        }
        if (!directAttackChoices.isEmpty()) {
            // 如果有进攻机会，就返回最简单的那颗球
            Collections.sort(directAttackChoices);
            AttackChoice.DirectAttackChoice easiest = directAttackChoices.getFirst();
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
    
    public void letOtherPlay() {
        switchPlayer();
        
        playingLetBall = true;

        if (isPlacedHandBallButNoHit()) {
            // 哪有自己摆好球再让对手打的
            forceSetBallInHand();
        }
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

        boolean isCareerGame = getEntireGame().getMetaMatchInfo() != null;

        if (!gameValues.isTraining()) {
            AchManager achManager = AchManager.getInstance();
            InGamePlayer human = getP1().isHuman() ? getP1() : getP2();
            boolean humanWin = getWiningPlayer().getInGamePlayer().isHuman();
            if (isCareerGame && humanWin) {
                achManager.addAchievement(Achievement.WIN_A_FRAME, human);
                achManager.addAchievement(Achievement.WIN_FRAMES, human);

                CareerManager cm = CareerManager.getInstance();
                if (cm.getAiGoodness() >= 1.0 && cm.getPlayerGoodness() <= 1.0) {
                    achManager.addAchievement(Achievement.WIN_FRAMES_NORMAL_DIFFICULTY, human);
                }
            }
        }
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
        public final double weightedMeanTargetDistance;
        public final double totalSeeAbleRads;
        //        public final double 
        public final double maxShadowAngle;  // 那颗球离白球的距离

        SeeAble(int seeAbleTargets,
                double avgTargetDistance,
                double weightedMeanTargetDistance,
                double totalSeeAbleRads,
                double maxShadowAngle) {
            this.seeAbleTargets = seeAbleTargets;
            this.avgTargetDistance = avgTargetDistance;
            this.weightedMeanTargetDistance = weightedMeanTargetDistance;
            this.totalSeeAbleRads = totalSeeAbleRads;
            this.maxShadowAngle = maxShadowAngle;
        }
    }

    public static class DoublePotAiming {
        public final Ball target;
        public final double[] targetPos;
        public final Pocket pocket;
        public final double[] collisionPos;
        public final List<double[]> cushionPos;  // 库点
        public final double[] whiteAiming;  // 理论上的瞄球方向
        public final int cushionCount;

        public DoublePotAiming(Ball target,
                        double[] targetPos,
                        Pocket pocket,
                        double[] collisionPos,
                        List<double[]> cushionPos,
                        double[] whiteAiming,
                        int cushionCount) {
            this.target = target;
            this.targetPos = targetPos;
            this.pocket = pocket;
            this.collisionPos = collisionPos;
            this.cushionPos = cushionPos;
            this.whiteAiming = whiteAiming;
            this.cushionCount = cushionCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DoublePotAiming that = (DoublePotAiming) o;
            return cushionCount == that.cushionCount &&
                    Objects.equals(target, that.target) &&
                    Objects.equals(pocket, that.pocket) &&
                    Arrays.equals(collisionPos, that.collisionPos) &&
                    Arrays.equals(whiteAiming, that.whiteAiming);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(target, pocket, cushionCount);
            result = 31 * result + Arrays.hashCode(collisionPos);
            result = 31 * result + Arrays.hashCode(whiteAiming);
            return result;
        }
    }

    public class WhitePredictor {
        private final Ball cueBallClone;
        private double lenAfterWall;
        private WhitePrediction prediction;
        private double cumulatedPhysicalTime = 0.0;
        private double dtWhenHitFirstWall = -1.0;
        private boolean notTerminated = true;
        private boolean hitWall;
        private boolean predictPath;
        private boolean checkCollisionAfterFirst;
        private boolean predictTargetBall;
        private boolean stopAtCollision;
        private Phy phy;

        WhitePredictor(Ball cueBallClone) {
            this.cueBallClone = cueBallClone;
        }

        WhitePrediction predict(Phy phy,
                                double lenAfterWall,
                                boolean predictPath,
                                boolean checkCollisionAfterFirst,
                                boolean predictTargetBall,
                                boolean stopAtCollision) {
            this.phy = phy;
            this.lenAfterWall = lenAfterWall;
            this.predictPath = predictPath;
            this.checkCollisionAfterFirst = checkCollisionAfterFirst;
            this.predictTargetBall = predictTargetBall;
            this.stopAtCollision = stopAtCollision;

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
            if (stopAtCollision && firstBall != null) {
                return true;
            }

            if (predictTargetBall && firstBall != null) {
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
                if (prediction.getFirstCollide() != null && predictTargetBall) {
                    checkTwiceCollision();
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
                if (prediction.getFirstCollide() != null && predictTargetBall) {
                    checkTwiceCollision();
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
                if (!tryWhiteHitBall()) {
                    cueBallClone.normalMove(phy);
                    return false;
                }
            } else if (checkCollisionAfterFirst && prediction.getSecondCollide() == null) {
                tryPassSecondBall();
            }
            if (prediction.getFirstCollide() != null && predictTargetBall) {
                checkTwiceCollision();
            }
            cueBallClone.normalMove(phy);
            return false;
        }

        private void checkTwiceCollision() {
            // assert true了应该
            if (whiteFirstCollide != null) {
//                System.out.println("Twice Dt: " + cueBallClone.predictedDtToPoint(whiteFirstCollide.x, whiteFirstCollide.y));
                if (cueBallClone.predictedDtToPoint(whiteFirstCollide.x, whiteFirstCollide.y) <
                        gameValues.ball.ballDiameter) {
//                    System.out.println("Twice collision!");
                    prediction.setTwiceColl(true);
                    return;
                }
            }
        }

        private void tryPassSecondBall() {
            for (Ball ball : getAllBalls()) {
                if (!ball.isWhite() && !ball.isPotted() && ball != prediction.getFirstCollide()) {
                    if (cueBallClone.predictedDtToPoint(ball.x, ball.y) <
                            gameValues.ball.ballDiameter) {
                        Ball ballClone = ball.clone();
//                        System.out.println("second: " + ballClone.getValue());
                        cueBallClone.twoMovingBallsHitCore( ballClone, phy);
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
                        prediction.setFirstBallCollidesOther(b);
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
                        cueBallClone.twoMovingBallsHitCore(ball, phy);
                        double[] rawBallUnitVec = Algebra.unitVector(
                                ball.getLastCollisionX() - cueBallClone.getLastCollisionX(),
                                ball.getLastCollisionY() - cueBallClone.getLastCollisionY());
                        double[] ballDirectionUnitVec = Algebra.unitVector(ball.vx, ball.vy);
                        double[] whiteDirectionUnitVec = Algebra.unitVector(whiteVx, whiteVy);

                        // todo: 确认是否考虑了齿轮效应
                        double ballInitVMmPerS = Math.hypot(ball.vx, ball.vy) * phy.calculationsPerSec;
                        if (!predictTargetBall) {
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
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public class PhysicsCalculator {

        private final Phy phy;
        private final int[] movementTypes = new int[nBalls()];
        private final double[] movementValues = new double[nBalls()];
        private Movement movement;
        private double cumulatedPhysicalTime = 0.0;
        private boolean notTerminated = true;
        
        private final List<TouchingBallsHandler> frameTouchingBallHandlers = new ArrayList<>();

        private B[] randomOrderBallPool1;
//        private B[] randomOrderBallPool2;

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
//                randomOrderBallPool2 = Arrays.copyOf(allBalls, allBalls.length);
            }
//        Util.reverseArray(randomArrangedBalls);
            Util.shuffleArray(randomOrderBallPool1);
//            Util.shuffleArray(randomOrderBallPool2);
        }

        private boolean oneRun() {
            boolean noBallMoving = true;
//            long st = System.nanoTime();
            reorderRandomPool();
            frameTouchingBallHandlers.clear();
            B[] allBalls = getAllBalls();
            for (B ball : allBalls) {
                ball.prepareMove(phy);
            }

            OUT_LOOP:
            for (int i = 0; i < allBalls.length; i++) {
                B ball = allBalls[i];
                if (ball.isPotted()) {
                    int stat = ball.tryFrameInPocket(phy);
                    if (stat != 0) {
                        noBallMoving = false;
                        if (stat == 2) {
                            movementTypes[i] = MovementFrame.POCKET_BACK;
                            movementValues[i] = ball.getMaxInPocketSpeed() / Values.MAX_POWER_SPEED;
                        }
                    }
                    continue;
                }

                if (ball.isOutOfTable()) continue;
                
                if (!phy.isPrediction && !frameTouchingBallHandlers.isEmpty()) {
                    for (TouchingBallsHandler tbh : frameTouchingBallHandlers) {
                        if (tbh.affectedBalls.contains(ball)) {
                            continue OUT_LOOP;
                        }
                    }
                }

                if (!ball.isLikelyStopped(phy)) {
                    noBallMoving = false;

                    if (ball.tryHitPocketsBack(phy)) {
//                        ball.normalMove(phy);
                        movementTypes[i] = MovementFrame.POT;
                        movementValues[i] = Math.hypot(ball.vx, ball.vy)
                                * phy.calculationsPerSec / Values.MAX_POWER_SPEED;
                        ball.naturalPot(1000);
                        newPotted.add(ball);
                        continue;
                    }

                    if (ball.willPot(phy)) {
                        movementTypes[i] = MovementFrame.POT;
                        movementValues[i] = Math.hypot(ball.vx, ball.vy)
                                * phy.calculationsPerSec / Values.MAX_POWER_SPEED;
                        ball.naturalPot(1000);
                        newPotted.add(ball);
                        continue;
                    }
                    if (ball.currentBounce != null) {
                        ball.processBounce(App.PRINT_DEBUG);
                        if (!tryHitBall(ball)) {
                            ball.normalMove(phy);
                        } else {
                            movementTypes[i] = MovementFrame.COLLISION;
                            movementValues[i] = ball.getLastCollisionRelSpeed()
                                    * phy.calculationsPerSec / Values.MAX_POWER_SPEED;
                        }
                        continue;
                    }

                    ObjectOnTable.CushionHitResult holeAreaResult = ball.tryHitHoleArea(phy);
                    if (holeAreaResult != null && holeAreaResult.result() != 0) {
                        // 袋口区域
                        if (tryHitBall(ball)) {
                            movementTypes[i] = MovementFrame.COLLISION;
                            movementValues[i] = ball.getLastCollisionRelSpeed()
                                    * phy.calculationsPerSec / Values.MAX_POWER_SPEED;
                        }
                        if (holeAreaResult.result() == 2) {
                            collidesWall = true;
                            recordHitCushion(ball);
                            if (ball.isWhite())
                                movement.getWhiteTrace().hitCushion(holeAreaResult.cushion());
                            else
                                movement.getTraceOfBallNotNull(ball).hitCushion(holeAreaResult.cushion());
                            movementTypes[i] = holeAreaResult.cushion().movementType();
                            movementValues[i] = Math.hypot(ball.vx, ball.vy)
                                    * phy.calculationsPerSec / Values.MAX_POWER_SPEED;
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
                        movementValues[i] = Math.hypot(ball.vx, ball.vy)
                                * phy.calculationsPerSec / Values.MAX_POWER_SPEED;
                        continue;
                    }

                    if (tryHitBall(ball)) {
                        movementTypes[i] = MovementFrame.COLLISION;
                        movementValues[i] = ball.getLastCollisionRelSpeed()
                                * phy.calculationsPerSec / Values.MAX_POWER_SPEED;
                    } else {
                        // 真不撞
                        if (ball.checkEnterBreakArea(getTable().breakLineX())) {
                            // 一定在normal move之前
                            recordCrossLine(ball);
                        }
                        ball.normalMove(phy);
                    }
                } 
//                else {
//                    if (!ball.sideSpinAtPosition(phy)) {
//
//                    }
//                }
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
        }

        private void whiteCollide(Ball ball) {
            if (whiteFirstCollide == null) {
                whiteFirstCollide = ball;
                movement.setWhiteFirstCollide(ball);
                collidesWall = false;  // 必须白球在接触首个目标球后，再有球碰库
            }
            movement.getWhiteTrace().hitBall(ball);
        }

        private boolean tryHitBall(B ball) {
            boolean hit = false;
            for (B otherBall : randomOrderBallPool1) {
                // 因为球会在袋内滞留约1秒，滞留期间当然是不会碰撞的
                if (ball != otherBall && !otherBall.isPotted()) {
                    // 检查是否是撞上贴球堆
                    if (!phy.isPrediction && ball.willCollide(otherBall)) {
                        if (processHittingTouchingBalls(ball, otherBall)) {
                            hit = true;
                            if (ball.isWhite()) whiteCollide(otherBall);  // 记录白球撞到的球
                            else if (otherBall.isWhite()) whiteCollide(ball);
                            break;
                        }
                    }
                    
                    if (ball.tryHitBall(Game.this, otherBall, true, phy)) {
                        // hit ball
                        hit = true;
                        if (ball.isWhite()) whiteCollide(otherBall);  // 记录白球撞到的球
                        else if (otherBall.isWhite()) whiteCollide(ball);
                        break;  // 假设一颗球在一物理帧内不会撞到两颗球
                    }
                }
            }
            return hit;
        }
        
        private boolean processHittingTouchingBalls(B b1, B b2) {
            B movingBall;
            B staticBall;
            if (b1.isNotMoving()) {
                movingBall = b2;
                staticBall = b1;
            } else if (b2.isNotMoving()) {
                movingBall = b1;
                staticBall = b2;
            } else {
                // 两颗球都在动，不会是贴球的情况
                return false;
            }
            
            if (TouchingBallsHandler.hasTouching(movingBall, staticBall, randomOrderBallPool1)) {
                TouchingBallsHandler tbh = new TouchingBallsHandler(movingBall, staticBall, randomOrderBallPool1);
                tbh.handle(phy);
                frameTouchingBallHandlers.add(tbh);
                return true;
            }
            return false;
        }
    }

    public static class FrameAchievementRecorder {
        int[] easyBallFails = new int[2];
    }
    
    class TouchingBallsHandler {
        private final B movingBall;
        private final B firstBallInTouch;
        private final Set<B> affectedBalls = new HashSet<>();
        private final B[] pool;

        TouchingBallsHandler(B movingBall, B firstBallInTouch, B[] pool) {
            this.movingBall = movingBall;
            this.firstBallInTouch = firstBallInTouch;
            this.pool = pool;
        }
        
        void handle(Phy phy) {
            addToAffected(firstBallInTouch);
            
            double[] firstBallPos = firstBallInTouch.getPositionArray();
            
            movingBall.twoMovingBallsHitCore(firstBallInTouch, phy);
            firstBallInTouch.setPosition(firstBallPos);
            
            double vx = firstBallInTouch.vx;
            double vy = firstBallInTouch.vy;

            propagateImpulse(new ArrayList<>(affectedBalls), firstBallInTouch, vx, vy, 
                    gameValues.ball.ballBounceRatio);
            
            affectedBalls.add(movingBall);
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        void propagateImpulse(List<B> balls, Ball source, double vx, double vy, double restitution) {
            int n = balls.size();
            double[] initImpulse = new double[]{vx, vy};
            int startIndex = balls.indexOf(source);
            
            // Initialize impulse map
            Map<Ball, double[]> impulseMap = new HashMap<>();
            for (Ball b : balls) {
                impulseMap.put(b, new double[]{0.0, 0.0});
            }
            // Seed initial ball
            impulseMap.put(balls.get(startIndex), new double[]{initImpulse[0], initImpulse[1]});

            // Precompute neighbor list for tight packing
            Map<Ball, List<Ball>> neighbors = new HashMap<>();
            for (int i = 0; i < n; i++) {
                Ball bi = balls.get(i);
                neighbors.putIfAbsent(bi, new ArrayList<>());
                for (int j = 0; j < n; j++) {
                    if (i == j) continue;
                    Ball bj = balls.get(j);
                    double dx = bj.x - bi.x;
                    double dy = bj.y - bi.y;
                    if (Math.hypot(dx, dy) <= bi.radius + bj.radius + 1e-6) {
                        neighbors.get(bi).add(bj);
                    }
                }
            }

            // BFS queue
            Queue<Ball> queue = new LinkedList<>();
            Set<Ball> visited = new HashSet<>();
            queue.add(balls.get(startIndex));
            visited.add(balls.get(startIndex));

            // Propagate
            while (!queue.isEmpty()) {
                Ball bi = queue.poll();
                double[] pi = impulseMap.get(bi);
                for (Ball bj : neighbors.get(bi)) {
                    // Compute unit normal from bi to bj
                    double dx = bj.x - bi.x;
                    double dy = bj.y - bi.y;
                    double dist = Math.hypot(dx, dy);
                    if (dist == 0) continue;
                    double nx = dx / dist;
                    double ny = dy / dist;
                    // Project impulse onto that normal
                    double magIn = pi[0] * nx + pi[1] * ny;
                    if (magIn <= 0) continue; // no forward transfer
                    double transferMag = magIn * restitution;
                    // Transfer vector
                    double tx = nx * transferMag;
                    double ty = ny * transferMag;
                    // Apply transfer
                    pi[0] -= tx;
                    pi[1] -= ty;
                    double[] pj = impulseMap.get(bj);
                    pj[0] += tx;
                    pj[1] += ty;
                    impulseMap.put(bi, pi);
                    impulseMap.put(bj, pj);
                    if (!visited.contains(bj)) {
                        visited.add(bj);
                        queue.add(bj);
                    }
                }
            }
            
            for (var entry : impulseMap.entrySet()) {
                entry.getKey().setVelocity(entry.getValue());
            }
        }
        
        private void addToAffected(B ball) {
            if (affectedBalls.contains(ball)) return;
            affectedBalls.add(ball);
            
            List<B> touching = listTouchings(ball);
            for (B other : touching) {
                addToAffected(other);
            }
        }
        
        private List<B> listTouchings(B ball) {
            List<B> res = new ArrayList<>();
            for (B other : pool) {
                if (!other.isPotted() && other != ball) {
                    if (ball.isTouching(other)) {
                        res.add(other);
                    }
                }
            }
            return res;
        }

        /**
         * 遍历球堆，找到所有贴球，并返回是否处理了
         */
        static <B extends Ball> boolean hasTouching(B movingBall, B firstBallInTouch, B[] pool) {
            for (B other : pool) {
                if (!other.isPotted() && other != firstBallInTouch && other != movingBall) {
                    if (firstBallInTouch.isTouching(other)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
